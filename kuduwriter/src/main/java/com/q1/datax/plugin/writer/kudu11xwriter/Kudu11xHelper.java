package com.q1.datax.plugin.writer.kudu11xwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daizihao
 * @create 2020-08-27 18:30
 **/
public class Kudu11xHelper {

    private static final Logger LOG = LoggerFactory.getLogger(Kudu11xHelper.class);

    public static Map<String, Object> getKuduConfiguration(String kuduConfig) {
        if (StringUtils.isBlank(kuduConfig)) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.REQUIRED_VALUE,
                    "Connection configuration information required.");
        }
        Map<String, Object> kConfiguration;
        try {
            kConfiguration = JSON.parseObject(kuduConfig, HashMap.class);
            Validate.isTrue(kConfiguration != null, "kuduConfig is null!");
            kConfiguration.put(Key.KUDU_ADMIN_TIMEOUT, kConfiguration.getOrDefault(Key.KUDU_ADMIN_TIMEOUT, Constant.ADMIN_TIMEOUTMS));
            kConfiguration.put(Key.KUDU_SESSION_TIMEOUT, kConfiguration.getOrDefault(Key.KUDU_SESSION_TIMEOUT, Constant.SESSION_TIMEOUTMS));
        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_CONNECTION_ERROR, e);
        }

        return kConfiguration;
    }

    public static KuduClient getKuduClient(String kuduConfig) {
        Map<String, Object> conf = Kudu11xHelper.getKuduConfiguration(kuduConfig);
        KuduClient kuduClient = null;
        try {
            String masterAddress = (String)conf.get(Key.KUDU_MASTER);
          //是否有Kerberos认证
            boolean haveKerberos = Boolean.parseBoolean((String) conf.get(Key.HAVE_KERBEROS));
            if(haveKerberos){
               String kerberosKeytabFilePath = (String) conf.get(Key.KERBEROS_KEYTAB_FILE_PATH);
               String kerberosPrincipal = (String) conf.get(Key.KERBEROS_PRINCIPAL);
                org.apache.hadoop.conf.Configuration hadoopConf = null;
                hadoopConf = new org.apache.hadoop.conf.Configuration();
                hadoopConf.set("hadoop.security.authentication" , "Kerberos");
                UserGroupInformation.setConfiguration(hadoopConf);
                try {
    				UserGroupInformation.loginUserFromKeytab(kerberosPrincipal, kerberosKeytabFilePath);
    			} catch (IOException e) {
    				String message = String.format("kerberos认证失败,请确定kerberosKeytabFilePath[%s]和kerberosPrincipal[%s]是否填写正确",
                            kerberosKeytabFilePath, kerberosPrincipal);
                    LOG.error(message);
    				e.printStackTrace();
    			}
                try {
    				 kuduClient = UserGroupInformation.getLoginUser().doAs(
    				        new PrivilegedExceptionAction<KuduClient>() {
    				            @Override
    				            public KuduClient run() throws Exception {
    				                return new KuduClient.KuduClientBuilder(masterAddress)
    				                		.defaultAdminOperationTimeoutMs((Long) conf.get(Key.KUDU_ADMIN_TIMEOUT))
    				                        .defaultOperationTimeoutMs((Long)conf.get(Key.KUDU_SESSION_TIMEOUT))
    				                        .build();
    				            }
    				        }
    				);
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    				throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_CONNECTION_ERROR, e);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    				throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_CONNECTION_ERROR, e);
    			}
            }else {
            kuduClient = new KuduClient.KuduClientBuilder(masterAddress)
                    .defaultAdminOperationTimeoutMs((Long) conf.get(Key.KUDU_ADMIN_TIMEOUT))
                    .defaultOperationTimeoutMs((Long)conf.get(Key.KUDU_SESSION_TIMEOUT))
                    .build();
            }
        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_CONNECTION_ERROR, e);
        }
        return kuduClient;
    }

    public static KuduTable getKuduTable(Configuration configuration, KuduClient kuduClient) {
        String tableName = configuration.getString(Key.TABLE);

        KuduTable table = null;
        try {
            if (kuduClient.tableExists(tableName)) {
                table = kuduClient.openTable(tableName);
            } else {
                synchronized (Kudu11xHelper.class) {
                    if (!kuduClient.tableExists(tableName)) {
                        Schema schema = Kudu11xHelper.getSchema(configuration);
                        CreateTableOptions tableOptions = new CreateTableOptions();

                        Kudu11xHelper.setTablePartition(configuration, tableOptions, schema);
                        //副本数
                        Integer numReplicas = configuration.getInt(Key.NUM_REPLICAS, 3);
                        tableOptions.setNumReplicas(numReplicas);
                        table = kuduClient.createTable(tableName, schema, tableOptions);
                    } else {
                        table = kuduClient.openTable(tableName);
                    }
                }
            }


        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_TABLE_ERROR, e);
        }
        return table;
    }

    public static void createTable(Configuration configuration) {
        String tableName = configuration.getString(Key.TABLE);
        String kuduConfig = configuration.getString(Key.KUDU_CONFIG);
        KuduClient kuduClient = Kudu11xHelper.getKuduClient(kuduConfig);
        try {
            Schema schema = Kudu11xHelper.getSchema(configuration);
            CreateTableOptions tableOptions = new CreateTableOptions();

            Kudu11xHelper.setTablePartition(configuration, tableOptions, schema);
            //副本数
            Integer numReplicas = configuration.getInt(Key.NUM_REPLICAS, 3);
            tableOptions.setNumReplicas(numReplicas);
            kuduClient.createTable(tableName, schema, tableOptions);
        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.GREATE_KUDU_TABLE_ERROR, e);
        } finally {
            AtomicInteger i = new AtomicInteger(5);
            while (i.get()>0) {
                try {
                    if (kuduClient.isCreateTableDone(tableName)){
                        Kudu11xHelper.closeClient(kuduClient);
                        LOG.info("Table "+ tableName +" is created!");
                        break;
                    }
                    i.decrementAndGet();
                    LOG.error("timeout!");
                } catch (KuduException  e) {
                    LOG.info("Wait for the table to be created..... "+i);
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    i.decrementAndGet();
                }
            }
            try {
                if (kuduClient != null) {
                    kuduClient.close();
                }
            } catch (KuduException e) {
                LOG.info("Kudu client has been shut down!");
            }
        }
    }

    public static boolean isTableExists(Configuration configuration) {
        String tableName = configuration.getString(Key.TABLE);
        String kuduConfig = configuration.getString(Key.KUDU_CONFIG);
        KuduClient kuduClient = Kudu11xHelper.getKuduClient(kuduConfig);
        try {
            return kuduClient.tableExists(tableName);
        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.GET_KUDU_CONNECTION_ERROR, e);
        } finally {
            Kudu11xHelper.closeClient(kuduClient);
        }
    }

    public static void closeClient(KuduClient kuduClient) {
        try {
            if (kuduClient != null) {
                kuduClient.close();
            }
        } catch (KuduException e) {
            LOG.warn("kudu client is not gracefully closed !");

        }

    }

    public static Schema getSchema(Configuration configuration) {
        List<Configuration> columns = configuration.getListConfiguration(Key.COLUMN);
        List<ColumnSchema> columnSchemas = new ArrayList<>();
        Schema schema = null;
        if (columns == null || columns.isEmpty()) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.REQUIRED_VALUE, "column is not defined，eg：column:[{\"name\": \"cf0:column0\",\"type\": \"string\"},{\"name\": \"cf1:column1\",\"type\": \"long\"}]");
        }
        try {
            for (Configuration column : columns) {

                String type = "BIGINT".equals(column.getNecessaryValue(Key.TYPE, Kudu11xWriterErrorcode.REQUIRED_VALUE).toUpperCase()) ||
                        "LONG".equals(column.getNecessaryValue(Key.TYPE, Kudu11xWriterErrorcode.REQUIRED_VALUE).toUpperCase()) ?
                        "INT64" : "INT".equals(column.getNecessaryValue(Key.TYPE, Kudu11xWriterErrorcode.REQUIRED_VALUE).toUpperCase())?
                        "INT32":column.getNecessaryValue(Key.TYPE, Kudu11xWriterErrorcode.REQUIRED_VALUE).toUpperCase();
                String name = column.getNecessaryValue(Key.NAME, Kudu11xWriterErrorcode.REQUIRED_VALUE);
                Boolean key = column.getBool(Key.PRIMARYKEY, false);
                String encoding = column.getString(Key.ENCODING, Constant.ENCODING).toUpperCase();
                String compression = column.getString(Key.COMPRESSION, Constant.COMPRESSION).toUpperCase();
                String comment = column.getString(Key.COMMENT, "");

                columnSchemas.add(new ColumnSchema.ColumnSchemaBuilder(name, Type.getTypeForName(type))
                        .key(key)
                        .encoding(ColumnSchema.Encoding.valueOf(encoding))
                        .compressionAlgorithm(ColumnSchema.CompressionAlgorithm.valueOf(compression))
                        .comment(comment)
                        .build());
            }
            schema = new Schema(columnSchemas);
        } catch (Exception e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.REQUIRED_VALUE, e);
        }
        return schema;
    }

    public static Integer getPrimaryKeyIndexUntil(List<Configuration> columns){
        int i = 0;
        while ( i < columns.size()  ) {
            Configuration col = columns.get(i);
            if (!col.getBool(Key.PRIMARYKEY, false)) {
                break;
            }
            i++;
        }
        return i;
    }

    public static void setTablePartition(Configuration configuration,
                                         CreateTableOptions tableOptions,
                                         Schema schema) {
        Configuration partition = configuration.getConfiguration(Key.PARTITION);
        if (partition == null) {
            ColumnSchema columnSchema = schema.getColumns().get(0);
            tableOptions.addHashPartitions(Collections.singletonList(columnSchema.getName()), 3);
            return;
        }
        //range分区
        Configuration range = partition.getConfiguration(Key.RANGE);
        if (range != null) {
            List<String> rangeColums = new ArrayList<>(range.getKeys());
            tableOptions.setRangePartitionColumns(rangeColums);
            for (String rangeColum : rangeColums) {
                List<Configuration> lowerAndUppers = range.getListConfiguration(rangeColum);
                for (Configuration lowerAndUpper : lowerAndUppers) {
                    PartialRow lower = schema.newPartialRow();
                    lower.addString(rangeColum, lowerAndUpper.getNecessaryValue(Key.LOWER, Kudu11xWriterErrorcode.REQUIRED_VALUE));
                    PartialRow upper = schema.newPartialRow();
                    upper.addString(rangeColum, lowerAndUpper.getNecessaryValue(Key.UPPER, Kudu11xWriterErrorcode.REQUIRED_VALUE));
                    tableOptions.addRangePartition(lower, upper);
                }
            }
            LOG.info("Set range partition complete!");
        }

        // 设置Hash分区
        Configuration hash = partition.getConfiguration(Key.HASH);
        if (hash != null) {
            List<String> hashColums = hash.getList(Key.COLUMN, String.class);
            Integer hashPartitionNum = configuration.getInt(Key.HASH_NUM, 3);
            tableOptions.addHashPartitions(hashColums, hashPartitionNum);
            LOG.info("Set hash partition complete!");
        }
    }

    public static void validateParameter(Configuration configuration) {
        configuration.getNecessaryValue(Key.KUDU_CONFIG, Kudu11xWriterErrorcode.REQUIRED_VALUE);
        configuration.getNecessaryValue(Key.TABLE, Kudu11xWriterErrorcode.REQUIRED_VALUE);
        String encoding = configuration.getString(Key.ENCODING, Constant.DEFAULT_ENCODING);
        if (!Charset.isSupported(encoding)) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.ILLEGAL_VALUE,
                    String.format("Encoding is not supported:[%s] .", encoding));
        }
        configuration.set(Key.ENCODING, encoding);
        String insertMode = configuration.getString(Key.INSERT_MODE, Constant.INSERT_MODE);
        try {
            InsertModeType.getByTypeName(insertMode);
        } catch (Exception e) {
            insertMode = Constant.INSERT_MODE;
        }
        configuration.set(Key.INSERT_MODE, insertMode);

        Long writeBufferSize = configuration.getLong(Key.WRITE_BATCH_SIZE, Constant.DEFAULT_WRITE_BATCH_SIZE);
        configuration.set(Key.WRITE_BATCH_SIZE, writeBufferSize);

        Long mutationBufferSpace = configuration.getLong(Key.MUTATION_BUFFER_SPACE, Constant.DEFAULT_MUTATION_BUFFER_SPACE);
        configuration.set(Key.MUTATION_BUFFER_SPACE, mutationBufferSpace);

        Boolean isSkipFail = configuration.getBool(Key.SKIP_FAIL, false);
        configuration.set(Key.SKIP_FAIL, isSkipFail);
        LOG.info("==validate parameter complete！");
    }

    public static void truncateTable(Configuration configuration) {
        String kuduConfig = configuration.getString(Key.KUDU_CONFIG);
        String userTable = configuration.getString(Key.TABLE);
        LOG.info(String.format("Because you have configured truncate is true,KuduWriter begins to truncate table %s .", userTable));
        KuduClient kuduClient = Kudu11xHelper.getKuduClient(kuduConfig);

        try {
            if (kuduClient.tableExists(userTable)) {
                kuduClient.deleteTable(userTable);
                LOG.info(String.format("table  %s has been deleted.", userTable));
            }
        } catch (KuduException e) {
            throw DataXException.asDataXException(Kudu11xWriterErrorcode.DELETE_KUDU_ERROR, e);
        } finally {
            Kudu11xHelper.closeClient(kuduClient);
        }

    }
}
