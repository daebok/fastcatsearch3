package org.fastcatsearch.http.action.management.collections;

import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.fastcatsearch.db.DBService;
import org.fastcatsearch.db.InternalDBModule.MapperSession;
import org.fastcatsearch.db.mapper.IndexingResultMapper;
import org.fastcatsearch.db.vo.IndexingStatusVO;
import org.fastcatsearch.http.ActionAuthority;
import org.fastcatsearch.http.ActionAuthorityLevel;
import org.fastcatsearch.http.ActionMapping;
import org.fastcatsearch.http.action.ActionRequest;
import org.fastcatsearch.http.action.ActionResponse;
import org.fastcatsearch.http.action.AuthAction;
import org.fastcatsearch.ir.IRService;
import org.fastcatsearch.ir.config.CollectionsConfig.Collection;
import org.fastcatsearch.service.ServiceManager;
import org.fastcatsearch.util.ResponseWriter;


@ActionMapping(value = "/management/collections/collection-indexing-info-list", authority = ActionAuthority.Collections, authorityLevel = ActionAuthorityLevel.NONE )
public class GetCollectionIndexingInfoListAction extends AuthAction {

	@Override
	public void doAuthAction(ActionRequest request, ActionResponse response)
			throws Exception {
		
		IRService irService = ServiceManager.getInstance().getService(IRService.class);
		
		DBService dbService = ServiceManager.getInstance().getService(DBService.class);
		
		MapperSession<IndexingResultMapper> session = null;
		
		try {
		
			session = dbService.getMapperSession(IndexingResultMapper.class);
			
			IndexingResultMapper mapper = session.getMapper();
			
			String collectionListStr = request.getParameter("collectionId", "");
			
			List<String> collections = null;
			
			if(!"".equals(collectionListStr)) {
				collections = Arrays.asList(
						collectionListStr.replaceAll(" ", "")
						.split(","));
			}
		
			List<Collection> collectionList = irService.getCollectionList();
			
			Writer writer = response.getWriter();
			ResponseWriter responseWriter = getDefaultResponseWriter(writer);
			responseWriter.object().key("indexingInfoList").array("indexingInfo");
			for(Collection collection : collectionList) {
				String collectionId = collection.getId();
				
				//원하는 컬렉션만 골라낼 때
				if (collections != null && !collections.contains(collectionId)) {
					continue;
				}
				
				//fetch only one
				List<IndexingStatusVO> indexingInfo = mapper.getRecentEntryList(collectionId, 1);
				if(indexingInfo != null && indexingInfo.size() == 1) {
					IndexingStatusVO vo = indexingInfo.get(0);
					responseWriter.object()
						.key("id").value(collectionId)
						.key("status").value(vo.startTime)
						.key("docSize").value(vo.docSize)
						.key("duration").value(vo.duration)
						.key("time").value(vo.endTime)
					.endObject();
				} else {
					responseWriter.object()
						.key("id").value(collectionId)
						.key("status").value("")
						.key("docSize").value("")
						.key("duration").value("")
						.key("time").value("")
					.endObject();
				}
			}
			responseWriter.endArray().endObject();
			responseWriter.done();
		} finally {
			if(session != null) {
				session.closeSession();
			}
		}
	}
}
