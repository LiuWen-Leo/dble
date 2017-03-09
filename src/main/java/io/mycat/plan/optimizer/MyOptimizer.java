package io.mycat.plan.optimizer;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import io.mycat.MycatServer;
import io.mycat.config.model.SchemaConfig;
import io.mycat.plan.PlanNode;
import io.mycat.plan.common.exception.MySQLOutPutException;
import io.mycat.plan.node.TableNode;
import io.mycat.route.util.RouterUtil;
import io.mycat.server.util.SchemaUtil;

public class MyOptimizer {
	//TODO:YHQ CHECK LOGIC
	public static PlanNode optimize(String schema, PlanNode node) {

		try {
			// 预先处理子查询
			node = SubQueryPreProcessor.optimize(node);
			int existGlobal = checkGlobalTable(node);
			if (node.isExsitView() || existGlobal != 1) {
				// 子查询优化
				node = SubQueryProcessor.optimize(node);

				node = JoinPreProcessor.optimize(node);

				// 预处理filter，比如过滤永假式/永真式
				node = FilterPreProcessor.optimize(node);
				//TODO  疑似错误
//				// // 将约束条件推向叶节点
//				node = FilterJoinColumnPusher.optimize(node);

				//TODO 
//				node = JoinERProcessor.optimize(node);
//
				if (existGlobal == 0) {
					node = GlobalTableProcessor.optimize(node);
				}

				node = FilterPusher.optimize(node);


				node = OrderByPusher.optimize(node);

				node = LimitPusher.optimize(node);

				node = SelectedProcessor.optimize(node);

				//TODO
//				boolean useJoinStrategy = ProxyServer.getInstance().getConfig().getSystem().isUseJoinStrategy();
//				if (useJoinStrategy){
//					node = JoinStrategyProcessor.optimize(node);
//				}
			}
			return node;
		} catch (MySQLOutPutException e) {
			Logger.getLogger(MyOptimizer.class).error(node.toString(), e);
			throw e;
		}
	}
	
	/**
	 * existShardTable
	 * 
	 * @param node
	 * @return 
	 * node不存在表名或者node全部为global表时  return 1;
	 * 全部为非global表时，return -1，之后不需要global优化;
	 * 可能需要优化global表，return 0；
	 */
	public static int checkGlobalTable(PlanNode node) {
		Set<String> dataNodes = null;
		boolean isAllGlobal = true;
		boolean isContainGlobal = false;
		for (TableNode tn : node.getReferedTableNodes()) {
			if (tn.getUnGlobalTableCount() == 0) {
				isContainGlobal = true;
				if (isAllGlobal) {
					if (dataNodes == null) {
						dataNodes = new HashSet<String>();
						dataNodes.addAll(tn.getNoshardNode());
					} else {
						dataNodes.retainAll(tn.getNoshardNode());
					}
				} else {
					return 0;
				}
			} else {
				isAllGlobal = false;
				if (isContainGlobal) {
					return 0;
				}
			}
		}
		
		if(isAllGlobal){
			if (dataNodes == null) {// all nonamenode 
				String db = SchemaUtil.getRandomDb();
				SchemaConfig schemaConfig = MycatServer.getInstance().getConfig().getSchemas().get(db);
				node.setNoshardNode(schemaConfig.getAllDataNodes());
				return 1;
			} else if (dataNodes.size() > 0) {//all global table
				node.setNoshardNode(dataNodes);
				String sql = node.getSql();
				for (TableNode tn : node.getReferedTableNodes()) {
					sql = RouterUtil.removeSchema(sql,tn.getSchema());
				}
				node.setSql(sql);
				return 1;
			} else {
				return 0;
			}
		}
		if(!isContainGlobal){
			return -1;
		}
		return 0;
	}

	
}