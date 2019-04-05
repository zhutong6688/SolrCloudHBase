package cn.guangtong.com.SolrCloudHBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
 
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
 
public class SolrCloudTest {
    public static final Log LOG = LogFactory.getLog(SolrCloudTest.class);
    private static CloudSolrClient cloudSolrClient;
 
    private static Connection connection;
    private static Table table;
    private static Get get;
    private static String defaultCollection = "userdev_pi_day";
    private static String hbaseTable = "userdev_pi_day";
    List<Get> list = new ArrayList<Get>();
 
    static {
        final List<String> zkHosts = new ArrayList<String>();
        zkHosts.add("1.1.1.1:2181");
        zkHosts.add("1.1.1.2:2181");
        zkHosts.add("1.1.1.3:2181");
        cloudSolrClient = new CloudSolrClient.Builder().withZkHost(zkHosts).build();
        final int zkClientTimeout = 10000;
        final int zkConnectTimeout = 10000;
        cloudSolrClient.setDefaultCollection(defaultCollection);
        cloudSolrClient.setZkClientTimeout(zkClientTimeout);
        cloudSolrClient.setZkConnectTimeout(zkConnectTimeout);
        try {
        	Configuration createConfiguration = HBaseConfiguration.create();
            connection = ConnectionFactory.createConnection(createConfiguration);
            table = connection.getTable(TableName.valueOf(hbaseTable));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    private void addIndex(CloudSolrClient cloudSolrClient) throws Exception {
        Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        for (int i = 0; i <= 100; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            String key = "";
            key = String.valueOf(i);
            doc.addField("rowkey", key);
            doc.addField("usermac", key + "usermac");
            doc.addField("userid", key + "userid");
            doc.addField("usertype", key + "usertype");
            doc.addField("city_id", key + "city_id");
            docs.add(doc);
        }
        LOG.info("docs info:" + docs + "\n");
        cloudSolrClient.add(docs);
        cloudSolrClient.commit();
    }
 
    public void search(CloudSolrClient cloudSolrClient, String Str) throws Exception {
        SolrQuery query = new SolrQuery();
        query.setRows(100);
        query.setQuery(Str);
        LOG.info("query string: " + Str);
        QueryResponse response = cloudSolrClient.query(query);
        SolrDocumentList docs = response.getResults();
        System.out.println("文档个数：" + docs.getNumFound()); //数据总条数也可轻易获取
        System.out.println("查询时间：" + response.getQTime());
        System.out.println("查询总时间：" + response.getElapsedTime());
        for (SolrDocument doc : docs) {
            String rowkey = (String) doc.getFieldValue("rowkey");
            get = new Get(Bytes.toBytes(rowkey));
            list.add(get);
        }
 
        Result[] res = table.get(list);
 
        for (Result rs : res) {
            Cell[] cells = rs.rawCells();
 
            for (Cell cell : cells) {
                System.out.println("============");
                System.out.println(new String(CellUtil.cloneRow(cell)));
                System.out.println(new String(CellUtil.cloneFamily(cell)));
                System.out.println(new String(CellUtil.cloneQualifier(cell)));
                System.out.println(new String(CellUtil.cloneValue(cell)));
                System.out.println("============");
                break;
            }
        }
 
 
        table.close();
    }
 
    public static void main(String[] args) throws Exception {
        cloudSolrClient.connect();
        SolrCloudTest solrt = new SolrCloudTest();
//            solrt.addIndex(cloudSolrClient);
        solrt.search(cloudSolrClient, "userid:11111");
        cloudSolrClient.close();
    }
}

