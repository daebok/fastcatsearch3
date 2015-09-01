package org.fastcatsearch.job.internal;

import java.io.IOException;

import org.fastcatsearch.common.io.Streamable;
import org.fastcatsearch.error.ErrorCode;
import org.fastcatsearch.error.SearchError;
import org.fastcatsearch.exception.FastcatSearchException;
import org.fastcatsearch.ir.IRService;
import org.fastcatsearch.ir.group.GroupsData;
import org.fastcatsearch.ir.io.DataInput;
import org.fastcatsearch.ir.io.DataOutput;
import org.fastcatsearch.ir.query.Query;
import org.fastcatsearch.ir.search.CollectionHandler;
import org.fastcatsearch.job.Job;
import org.fastcatsearch.query.QueryMap;
import org.fastcatsearch.query.QueryParser;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.transport.vo.StreamableGroupsData;

public class InternalGroupSearchJob extends Job implements Streamable {
	private QueryMap queryMap;
	
	public InternalGroupSearchJob(){}
	
	public InternalGroupSearchJob(QueryMap queryMap){
		this.queryMap = queryMap;
	}
	
	@Override
	public JobResult doRun() throws FastcatSearchException {
		
		Query q = QueryParser.getInstance().parseQuery(queryMap);

//		Metadata meta = q.getMeta();
		String collectionId = queryMap.collectionId();
		
		try {
			GroupsData result = null;
			
			IRService irService = ServiceManager.getInstance().getService(IRService.class);
			
			
			//Not Exist in Cache
			if(result == null){
				CollectionHandler collectionHandler = irService.collectionHandler(collectionId);
				if(collectionHandler == null){
					throw new FastcatSearchException("ERR-00520", collectionId);
				}
				
				result = collectionHandler.searcher().doGrouping(q);
			}
			
			return new JobResult(new StreamableGroupsData(result));
			
		} catch (FastcatSearchException e){
			throw e;
		} catch(Exception e){
			logger.error("", e);
//			EventDBLogger.error(EventDBLogger.CATE_SEARCH, "검색에러..", EventDBLogger.getStackTrace(e));
			throw new FastcatSearchException("ERR-00551", e, collectionId);
		}
		
	}
	
	@Override
	public void readFrom(DataInput input) throws IOException {
		this.queryMap = new QueryMap();
		queryMap.readFrom(input);
	}
	@Override
	public void writeTo(DataOutput output) throws IOException {
		queryMap.writeTo(output);
	}
}
