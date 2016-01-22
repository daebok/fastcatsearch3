package org.fastcatsearch.job.indexing;

import org.fastcatsearch.common.io.Streamable;
import org.fastcatsearch.env.SettingManager;
import org.fastcatsearch.exception.FastcatSearchException;
import org.fastcatsearch.ir.CollectionAddIndexer;
import org.fastcatsearch.ir.CollectionIndexerable;
import org.fastcatsearch.ir.CollectionMergeIndexer;
import org.fastcatsearch.ir.IRService;
import org.fastcatsearch.ir.common.IndexingType;
import org.fastcatsearch.ir.config.CollectionContext;
import org.fastcatsearch.ir.config.DataInfo;
import org.fastcatsearch.ir.io.DataInput;
import org.fastcatsearch.ir.io.DataOutput;
import org.fastcatsearch.ir.search.CollectionHandler;
import org.fastcatsearch.job.CacheServiceRestartJob;
import org.fastcatsearch.job.Job;
import org.fastcatsearch.job.indexing.IndexingStopException;
import org.fastcatsearch.job.state.IndexingTaskState;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.transport.vo.StreamableCollectionContext;
import org.fastcatsearch.util.CollectionContextUtil;
import org.fastcatsearch.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 각 노드로 전달되어 색인머징을 수행하는 작업.
 * Created by swsong on 2015. 12. 24..
 */
public class NodeIndexMergingJob extends Job implements Streamable {

    private String collectionId;
    private String documentId;

    public NodeIndexMergingJob() {
    }

    public NodeIndexMergingJob(String collectionId, String documentId) {
        this.collectionId = collectionId;
        this.documentId = documentId;
    }

    @Override
    public JobResult doRun() throws FastcatSearchException {
        IRService irService = ServiceManager.getInstance().getService(IRService.class);
        CollectionHandler collectionHandler = irService.collectionHandler(collectionId);
        try {
            // 머징 시작표시..
            collectionHandler.startMergingStatus();

            CollectionContext collectionContext = collectionHandler.collectionContext();

            List<String> merge100 = new ArrayList<String>();
            List<String> merge10K = new ArrayList<String>();
            List<String> merge100K = new ArrayList<String>();
            List<String> merge1M = new ArrayList<String>();
            List<String> merge10M = new ArrayList<String>();
            List<String> mergeOver10M = new ArrayList<String>();

            // 세그먼트를 확인하여 머징가능한 조합을 찾아낸다.
            List<DataInfo.SegmentInfo> segmentInfoList = collectionContext.dataInfo().getSegmentInfoList();
            for (DataInfo.SegmentInfo segmentInfo : segmentInfoList) {
                int docSize = segmentInfo.getDocumentCount();
                String segmentId = segmentInfo.getId();

                //크기가 비슷한 것끼리 묶는다.
                //100, 1만, 10만, 100만, 1000만, 그이상 구간을 둔다
                // TODO 삭제문서까지 고려한 realSize기반으로 머징한다.
                if (docSize <= 100) {
                    merge100.add(segmentId);
                } else if (docSize <= 10 * 1000) {
                    merge10K.add(segmentId);
                } else if (docSize <= 100 * 1000) {
                    merge100K.add(segmentId);
                } else if (docSize <= 1000 * 1000) {
                    merge1M.add(segmentId);
                } else if (docSize <= 10 * 1000 * 1000) {
                    merge10M.add(segmentId);
                } else if (docSize > 10 * 1000 * 1000) {
                    mergeOver10M.add(segmentId);
                }
            }

            // 머징시 하위 구간을 모두 포함한다.
            List<String> mergeSegmentIdList = new ArrayList<String>();
            if (mergeOver10M.size() >= 3) {
                mergeSegmentIdList.addAll(mergeOver10M);
                mergeSegmentIdList.addAll(merge10M);
                mergeSegmentIdList.addAll(merge1M);
                mergeSegmentIdList.addAll(merge100K);
                mergeSegmentIdList.addAll(merge10K);
                mergeSegmentIdList.addAll(merge100);
            } else if (merge10M.size() >= 3) {
                mergeSegmentIdList.addAll(merge10M);
                mergeSegmentIdList.addAll(merge1M);
                mergeSegmentIdList.addAll(merge100K);
                mergeSegmentIdList.addAll(merge10K);
                mergeSegmentIdList.addAll(merge100);
            } else if (merge1M.size() >= 3) {
                mergeSegmentIdList.addAll(merge1M);
                mergeSegmentIdList.addAll(merge100K);
                mergeSegmentIdList.addAll(merge10K);
                mergeSegmentIdList.addAll(merge100);
            } else if (merge100K.size() >= 3) {
                mergeSegmentIdList.addAll(merge100K);
                mergeSegmentIdList.addAll(merge10K);
                mergeSegmentIdList.addAll(merge100);
            } else if (merge10K.size() >= 3) {
                mergeSegmentIdList.addAll(merge10K);
                mergeSegmentIdList.addAll(merge100);
            } else if (merge100.size() >= 2) {
                mergeSegmentIdList.addAll(merge100);
            }

            if (mergeSegmentIdList.size() >= 2) {
                //mergeIdList 를 File[]로 변환.
                File[] segmentDirs = new File[mergeSegmentIdList.size()];
                for (int i = 0; i < mergeSegmentIdList.size(); i++) {
                    segmentDirs[i] = collectionContext.indexFilePaths().segmentFile(mergeSegmentIdList.get(i));
                }
                CollectionMergeIndexer mergeIndexer = new CollectionMergeIndexer(documentId, collectionHandler, segmentDirs);
                DataInfo.SegmentInfo segmentInfo = null;
                Throwable indexingThrowable = null;
                try {
                    mergeIndexer.doIndexing();
                } catch (Throwable e) {
                    indexingThrowable = e;
                } finally {
                    if (mergeIndexer != null) {
                        try {
                            segmentInfo = mergeIndexer.close();
                        } catch (Throwable closeThrowable) {
                            // 이전에 이미 발생한 에러가 있다면 close 중에 발생한 에러보다 이전 에러를 throw한다.
                            if (indexingThrowable == null) {
                                indexingThrowable = closeThrowable;
                            }
                        }
                    }
                    if (indexingThrowable != null) {
                        throw indexingThrowable;
                    }
                }

                File segmentDir = mergeIndexer.getSegmentDir();
                if(segmentInfo.getDocumentCount() == 0) {
                    logger.info("[{}] Delete segment dir due to no documents = {}", collectionHandler.collectionId(), segmentDir.getAbsolutePath());
                    //세그먼트를 삭제하고 없던 일로 한다.
                    FileUtils.deleteDirectory(segmentDir);
                    collectionContext = collectionHandler.removeMergedSegment(mergeSegmentIdList);
                } else {
                    collectionContext = collectionHandler.applyMergedSegment(segmentInfo, mergeIndexer.getSegmentDir(), mergeSegmentIdList);
                }
                CollectionContextUtil.saveCollectionAfterIndexing(collectionContext);

                return new JobResult(true);
            } else {
                //머징없음.
                return new JobResult(false);
            }
        } catch (Throwable e) {
            logger.error("", e);
            throw new FastcatSearchException("ERR-00525", e);
        } finally {
            // 머징 끝남표시..
            collectionHandler.endMergingStatus();
        }
    }

    @Override
    public void readFrom(DataInput input) throws IOException {
        collectionId = input.readString();
        documentId = input.readString();
    }

    @Override
    public void writeTo(DataOutput output) throws IOException {
        output.writeString(collectionId);
        output.writeString(documentId);
    }

}
