package cn.guangtong.com.SolrCloudHBase;

/**
 * hbase 二级索引
 * @author wing
 * @createTime 2017-4-7 
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

public class App extends BaseRegionObserver {

    private HTablePool pool = null;

    private final static String SOURCE_TABLE = "test";

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        pool = new HTablePool(env.getConfiguration(), 10);
    }

    @Override
    public void postGetOp(ObserverContext<RegionCoprocessorEnvironment> c,
            Get get, List<Cell> results) throws IOException {
        HTableInterface table = pool.getTable(Bytes.toBytes(SOURCE_TABLE));
        String newRowkey = Bytes.toString(get.getRow());
        String pre = newRowkey.substring(0, 1);

        if (pre.equals("t")) {
            String[] splits = newRowkey.split("_");
            String prepre = splits[0].substring(1, 3);
            String timestamp = splits[0].substring(3);
            String uid = splits[1];
            String mid = "";
            for (int i = 2; i < splits.length; i++) {
                mid += splits[i];
                mid += "_";
            }
            mid = mid.substring(0, mid.length() - 1);
            String rowkey = prepre + uid + "_" + timestamp + "_" + mid;
            System.out.println(rowkey);
            Get realget = new Get(rowkey.getBytes());
            Result result = table.get(realget);

            List<Cell> cells = result.listCells();
            results.clear();
            for (Cell cell : cells) {
                results.add(cell);
            }

        }
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e,
            Put put, WALEdit edit, Durability durability) throws IOException {
        try {

            String rowkey = Bytes.toString(put.getRow());
            HTableInterface table = pool.getTable(Bytes.toBytes(SOURCE_TABLE));

            String pre = rowkey.substring(0, 2);
            if (pre.equals("aa") || pre.equals("ab") || pre.equals("ac")
                    || pre.equals("ba") || pre.equals("bb") || pre.equals("bc")
                    || pre.equals("ca") || pre.equals("cb") || pre.equals("cc")) {
                String[] splits = rowkey.split("_");
                String uid = splits[0].substring(2);
                String timestamp = splits[1];
                String mid = "";
                for (int i = 2; i < splits.length; i++) {
                    mid += splits[i];
                    mid += "_";
                }
                mid = mid.substring(0, mid.length() - 1);
                String newRowkey = "t" + pre + timestamp + "_" + uid + "_"
                        + mid;
                System.out.println(newRowkey);
                Put indexput2 = new Put(newRowkey.getBytes());
                indexput2.addColumn("relation".getBytes(),
                        "column10".getBytes(), "45".getBytes());
                table.put(indexput2);

            }
            table.close();

        } catch (Exception ex) {

        }

    }

    @Override
    public boolean postScannerNext(
            ObserverContext<RegionCoprocessorEnvironment> e, InternalScanner s,
            List<Result> results, int limit, boolean hasMore)
            throws IOException {
        HTableInterface table = pool.getTable(Bytes.toBytes(SOURCE_TABLE));
        List<Result> newresults = new ArrayList<Result>();
        for (Result result : results) {
            String newRowkey = Bytes.toString(result.getRow());

            String pre = newRowkey.substring(0, 1);

            if (pre.equals("t")) {
                String[] splits = newRowkey.split("_");
                String prepre = splits[0].substring(1, 3);
                String timestamp = splits[0].substring(3);
                String uid = splits[1];
                String mid = "";
                for (int i = 2; i < splits.length; i++) {
                    mid += splits[i];
                    mid += "_";
                }
                mid = mid.substring(0, mid.length() - 1);
                String rowkey = prepre + uid + "_" + timestamp + "_" + mid;

                Get realget = new Get(rowkey.getBytes());
                Result newresult = table.get(realget);

                newresults.add(newresult);
            }

        }
         results.clear();
        for (Result result : newresults) {
            results.add(result);
        }

        return hasMore;

    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        pool.close();
    }
    
}

