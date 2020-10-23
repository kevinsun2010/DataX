package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HttpReader extends Reader {

    /**
     * Job 中的方法仅执行一次，Task 中方法会由框架启动多个 Task 线程并行执行。
     * <p/>
     * 整个 Reader 执行流程是：
     * <pre>
     * Job类init-->prepare-->split
     *
     * Task类init-->prepare-->startRead-->post-->destroy
     * Task类init-->prepare-->startRead-->post-->destroy
     *
     * Job类post-->destroy
     * </pre>
     */
    public static class Job extends Reader.Job {
        private static final Logger LOG = LoggerFactory
                .getLogger(Job.class);

        private Configuration readerOriginConfig = null;
        private List<String> sourceHttpRequests;
        private HttpHelper httpHelper = null;
        private String requestType = null;
        private String url = null;
        private String requestParams=null;

        @Override
        public void init() {

            LOG.info("init() begin...");
            this.readerOriginConfig = super.getPluginJobConf();
            this.validate();   
            httpHelper = new HttpHelper(this.readerOriginConfig);
            LOG.info("init() ok and end...");

        }

        public void validate(){
        	url = this.readerOriginConfig.getNecessaryValue(Key.URL,HttpReaderErrorCode.REQUEST_URL_NOT_FIND_ERROR);
        	requestType=this.readerOriginConfig.getNecessaryValue(Key.REQUEST_TYPE,HttpReaderErrorCode.REQUEST_TYPE_NOT_FIND_ERROR);
        	this.readerOriginConfig.getNecessaryValue(Key.JSON_TYPE,HttpReaderErrorCode.JSON_TYPE_NOT_FIND_ERROR);
        	this.readerOriginConfig.getNecessaryValue(Key.JSON_PATH,HttpReaderErrorCode.JSON_PATH_NOT_FIND_ERROR);
        	this.readerOriginConfig.getNecessaryValue(Key.COLUMN,HttpReaderErrorCode.COLUMN_NOT_FIND_ERROR);
        	String havePage=this.readerOriginConfig.getUnnecessaryValue(Key.HAVE_PAGE,"false",HttpReaderErrorCode.CONFIG_INVALID_EXCEPTION);
        	if(Boolean.parseBoolean(havePage)) {
        		this.readerOriginConfig.getNecessaryValue(Key.PAGE_SIZE,HttpReaderErrorCode.CONFIG_INVALID_EXCEPTION);
        	}
        }
        
        
		

        @Override
        public void prepare() {
            LOG.info("prepare(), start to getAllHttpRequests...");
            
            //一次请求获取总的分页数，传递参数，生产请求url列表
            this.sourceHttpRequests = httpHelper.getAllHttpRequests(url,requestType, requestParams,this.readerOriginConfig);
            LOG.info(String.format("您即将访问的请求url数为: [%s], 列表为: [%s]",
                    this.sourceHttpRequests.size(),
                    StringUtils.join(this.sourceHttpRequests, ",")));
        }

        @Override
        public List<Configuration> split(int adviceNumber) {

            LOG.info("split() begin...");
            List<Configuration> readerSplitConfigs = new ArrayList<Configuration>();
            // warn:每个slice拖且仅拖一个文件,
            int splitNumber = adviceNumber;
            if (0 == splitNumber) {
                throw DataXException.asDataXException(HttpReaderErrorCode.REQUEST_URL_NOT_FIND_ERROR,
                        String.format("未能找到待請求的地址,请确认您的配置项url: %s", this.readerOriginConfig.getString(Key.URL)));
            }

            List<List<String>> splitedSourceHttpRequests = this.splitSourceHttpRequests(new ArrayList<String>(this.sourceHttpRequests), splitNumber);
            for (List<String> requests : splitedSourceHttpRequests) {
                Configuration splitedConfig = this.readerOriginConfig.clone();
                splitedConfig.set(Constant.SOURCE_HTTP_REQUESTS, requests);
                readerSplitConfigs.add(splitedConfig);
            }
            return readerSplitConfigs;
        }


        private <T> List<List<T>> splitSourceHttpRequests(final List<T> sourceList, int adviceNumber) {
            List<List<T>> splitedList = new ArrayList<List<T>>();
            int averageLength = sourceList.size() / adviceNumber;
            averageLength = averageLength == 0 ? 1 : averageLength;
            for (int begin = 0, end = 0; begin < sourceList.size(); begin = end) {
                end = begin + averageLength;
                if (end > sourceList.size()) {
                    end = sourceList.size();
                }
                splitedList.add(sourceList.subList(begin, end));
            }
            return splitedList;
        }


        @Override
        public void post() {

        }

        @Override
        public void destroy() {

        }

    }

    public static class Task extends Reader.Task {

        private static Logger LOG = LoggerFactory.getLogger(Reader.Task.class);
        private Configuration taskConfig;
        private List<String> sourceHttpRequests;
        private String specifiedRequestType;
        private HttpHelper httpHelper = null;

        @Override
        public void init() {

            this.taskConfig = super.getPluginJobConf();
            this.sourceHttpRequests = this.taskConfig.getList(Constant.SOURCE_HTTP_REQUESTS, String.class);
            this.specifiedRequestType = this.taskConfig.getString(Key.REQUEST_TYPE);
            this.httpHelper = new HttpHelper(this.taskConfig);
			
        }

        @Override
        public void prepare() {

        }

        @Override
        public void startRead(RecordSender recordSender) {

            LOG.info("read start");
            for (String httpRequest : this.sourceHttpRequests) {
                LOG.info(String.format("calling httpRequest : [%s]", httpRequest));

                if(specifiedRequestType.equalsIgnoreCase(Constant.GET)) {
                	httpHelper.doGetStartRead(httpRequest,this.taskConfig, recordSender, this.getTaskPluginCollector());
                }
                else if(specifiedRequestType.equalsIgnoreCase(Constant.PUT))
                {
                	httpHelper.doPostStartRead(httpRequest,this.taskConfig, recordSender, this.getTaskPluginCollector());
                }
                else 
                {

                    String message = "HttpReader插件目前支持GET, PUT两种HTTP请求方式," +
                            "请将requestType选项的值配置为GET, PUT";
                    throw DataXException.asDataXException(HttpReaderErrorCode.REQUEST_TYPE_UNSUPPORT, message);
                }

                if(recordSender != null){
                    recordSender.flush();
                }
            }

            LOG.info("end read source files...");
        }

        @Override
        public void post() {

        }

        @Override
        public void destroy() {

        }

    }

}