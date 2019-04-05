package cn.guangtong.com.SolrCloudHBase;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.util.List;
 
/**
 * 为hbase提供二级索引的协处理器 Coprocesser
 */
public class UserDevPiSolrObserver extends BaseRegionObserver {
 
    //加载配置文件属性
    static Config config = ConfigFactory.load("userdev_pi_solr.properties");
 
    //log记录
    private static final Logger logger = LoggerFactory.getLogger(UserDevPiSolrObserver.class);
 
    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit, Durability durability) throws IOException {
        // 获取行键值
        String rowkey = Bytes.toString(put.getRow());
        //实例化 SolrDoc
        SolrInputDocument doc = new SolrInputDocument();
        //添加Solr uniqueKey值
        doc.addField("rowkey", rowkey);
        // 获取需要索引的列
        String[] hbase_columns = config.getString("hbase_column").split(",");
 
        // 获取需要索引的列的值并将其添加到SolrDoc
        for (int i = 0; i < hbase_columns.length; i++) {
            String colName = hbase_columns[i];
            String colValue = "";
            // 获取指定列
            List<Cell> cells = put.get("cf".getBytes(), colName.getBytes());
            if (cells != null) {
                try {
                    colValue = Bytes.toString(CellUtil.cloneValue(cells.get(0)));
                } catch (Exception ex) {
                    logger.error("添加solrdoc错误", ex);
                }
            }
 
            doc.addField(colName, colValue);
        }
 
        //发送数据到本地缓存
        SolrIndexTools.addDoc(doc);
    }
 
    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, Durability durability) throws IOException {
        //得到rowkey
        String rowkey = Bytes.toString(delete.getRow());
        //发送数据本地缓存
        String solr_collection = config.getString("solr_collection");
        SolrIndexTools.delDoc(rowkey);
    }
}
