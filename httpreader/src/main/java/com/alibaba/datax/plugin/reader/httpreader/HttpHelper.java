package com.alibaba.datax.plugin.reader.httpreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.reader.ColumnEntry;
import com.alibaba.datax.plugin.unstructuredstorage.reader.UnstructuredStorageReaderUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

public class HttpHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HttpReader.Job.class);


    public HttpHelper(Configuration taskConfig) {
    	
    	LOG.info(String.format("taskConfig details:%s", JSON.toJSONString(taskConfig)));
	}

    private static Map<String, Object> getUrlParams(String url) {
		Map<String, Object> map = new HashMap<String, Object>(0);
		
		if (StringUtils.isBlank(url)) {
			return map;
		}
		String param=url.substring(url.lastIndexOf("?")+1);
		
		if (StringUtils.isBlank(param)) {
			return map;
		}
		String[] params = param.split("&");
		for (int i = 0; i < params.length; i++) {
			String[] p = params[i].split("=");
			if (p.length == 2) {
				map.put(p[0], p[1]);
			}
		}
		return map;
    }

    
    private  String auth(String url,String method,String signParam) {
    	
    	Map<String,Object> requestParamMap=getUrlParams(url);
    	JSONObject  jsonObject=JSONObject.parseObject(signParam);
		String appId=jsonObject.getString("appId");
		String key=jsonObject.getString("key");
		
		//String method="GET";//是否区分大小写
		String keyTime=System.currentTimeMillis()+100000+"";//100s有效期
		
		String sign=encry(requestParamMap,method,keyTime,appId,key);
		
		Map<String,Object> authMap=new HashMap<String,Object>();
		//Map 对象存入 用户名,密码,电话号码
		authMap.put("sign", sign);
		authMap.put("keyTime",keyTime );
		authMap.put("appId", appId);
		authMap.putAll(requestParamMap);
		
		//Map 转成  JSONObject 字符串
		JSONObject jsonObj=new JSONObject(authMap);
		String auth=jsonObj.toString();
		return  auth;
    }
    
    
    private  String encry(Map<String,Object> param,String method,String keyTime,String appId,String key) {
		String signKey=HmacUtils.hmacSha1Hex(key, keyTime);
		String httpString =method + "\n" +getParamStr(param);
		String toSign=keyTime+"\n"+DigestUtils.sha1Hex(httpString);
		String sign= HmacUtils.hmacSha1Hex(signKey, toSign);
		return sign;
	}
	
	
	private  String getParamStr(Map<String,Object> param) {
		Set<String> set=param.keySet();
        Object[] arr= set.toArray();
        Arrays.sort(arr);
		StringBuffer sbParams=new StringBuffer();
		 for(Object key:arr)
		 {
			 sbParams.append(key + "=" + param.get(key) +"&");
	     }
		return sbParams.toString().substring(0, sbParams.length()-1);
	}
	
    
	public   List<String> getAllHttpRequests(String url, String requestType,String requestParams,Configuration readerOriginConfig) {
		List<String>   allHttpRequests=new ArrayList<String>();
		//String requestParams="[{\"name\":\"name\",\"value\":\"zhangsan\"},{\"name\":\"age\",\"value\":\"25\"}]";
		
		StringBuffer sbHttpRequestUrl=new StringBuffer();
		sbHttpRequestUrl.append(url);
		sbHttpRequestUrl.append("?");
		
		StringBuffer sbParams=new StringBuffer();
		requestType=requestType.toUpperCase();
		if(Constant.GET.equalsIgnoreCase(requestType)) 
		{
			if(StringUtils.isBlank(requestParams)==false) 
			{
				JSONArray  paramsArray=JSONObject.parseArray(requestParams);
				// 遍历JSONArray
				for (Iterator<Object> iterator = paramsArray.iterator(); iterator.hasNext(); ) {
					JSONObject next = (JSONObject) iterator.next();
					String parmasName=next.getString("name");
					String parmasValue=next.getString("value");
					sbParams.append(parmasName);
					sbParams.append("=");
					sbParams.append(parmasValue);
					sbParams.append("&");
				}
			}
			boolean havePage=Boolean.parseBoolean(readerOriginConfig.getString(Key.HAVE_PAGE));
			if(havePage) 
			{
				int pageNo=Constant.DEFAULT_FRIST_PAGE_NO;
				int pageSize=Integer.parseInt(readerOriginConfig.getString(Key.PAGE_SIZE,Constant.DEFAULT_PAGE_SIZE));
				sbParams.append("pageNo");
				sbParams.append("=");
				sbParams.append(pageNo);
				sbParams.append("&");
				sbParams.append("pageSize");
				sbParams.append("=");
				sbParams.append(pageSize);
				sbParams.append("&");
			}
			sbHttpRequestUrl.append(sbParams);
			String httpRequestUrl=sbHttpRequestUrl.toString().substring(0, sbHttpRequestUrl.length()-1);
			if(havePage) 
			{
				String signType=readerOriginConfig.getString(Key.SIGN_TYPE,Constant.DEFAULT_SIGN_TYPE);
				String signParam=readerOriginConfig.getString(Key.SIGN_PARAM);
				String  resultjson=null;
				if(Constant.SIGN_TYPE_01.equals(signType)) {
					String auth=auth(httpRequestUrl,requestType,signParam);
					Map<String,String>   headerMap=new HashMap<String,String>();
					headerMap.put(Constant.HTTP_AUTH_KEY, auth);
					resultjson=doGet(httpRequestUrl,headerMap);
				}else 
				{
			      resultjson=doGet(httpRequestUrl,null);
				}
				//首页请求，获取totalPage
	    		Configuration   responseResultConfig=Configuration.from(resultjson);    
	    		int pageTotal=Integer.parseInt(responseResultConfig.getString(Key.PAGE_TOTAL));
	    		for (int i = 1; i <= pageTotal; i++) 
	    		{
	    			String urlForPage=httpRequestUrl.replaceFirst("pageNo=1", "pageNo="+i);
	    			allHttpRequests.add(urlForPage);
				}
				
			}
			else 
			{
				allHttpRequests.add(httpRequestUrl);
			}
		}
		return allHttpRequests;
	}
    
	
    
    public static String doGet(String url,Map<String,String> headerMap) {
        try {
            HttpClient client = new DefaultHttpClient();
            //发送get请求
            HttpGet request = new HttpGet(url);
            
            if(headerMap !=null) 
            {
    			  for(Map.Entry<String, String> entry : headerMap.entrySet())
    			  {
    				  String mapKey =entry.getKey(); 
    				  String mapValue =  entry.getValue();
    				  request.addHeader(mapKey, mapValue);
    			      //System.out.println(mapKey+":"+mapValue); 
    			   }
            }
            HttpResponse response = client.execute(request);
            /**请求发送成功，并得到响应**/
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                /**读取服务器返回过来的json字符串数据**/
                String strResult = EntityUtils.toString(response.getEntity());
               // System.out.println("Http Get Result===="+strResult);
                return strResult;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    
    
    /**
     * post请求(用于key-value格式的参数)
     * @param url
     * @param params
     * @return
     */
    public static String doPost(String url, Map params){

        BufferedReader in = null;
        try {
            // 定义HttpClient  
            HttpClient client = new DefaultHttpClient();
            // 实例化HTTP方法  
            HttpPost request = new HttpPost();
            request.setURI(new URI(url));

            //设置参数
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                String value = String.valueOf(params.get(name));
                nvps.add(new BasicNameValuePair(name, value));

                //System.out.println(name +"-"+value);
            }
            request.setEntity(new UrlEncodedFormEntity(nvps,HTTP.UTF_8));

            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if(code == 200){    //请求成功
                in = new BufferedReader(new InputStreamReader(response.getEntity()
                        .getContent(),"utf-8"));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }

                in.close();

                return sb.toString();
            }
            else{    //
                System.out.println("状态码：" + code);
                return null;
            }
        }
        catch(Exception e){
            e.printStackTrace();

            return null;
        }
    }
    
    
    
    /**
     * post请求（用于请求json格式的参数）
     * @param url
     * @param params
     * @return
     */
    public static String doPost(String url, String params) throws Exception {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);// 创建httpPost   
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");
        String charSet = "UTF-8";
        StringEntity entity = new StringEntity(params, charSet);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = null;

        try {

            response = httpclient.execute(httpPost);
            StatusLine status = response.getStatusLine();
            int state = status.getStatusCode();
            if (state == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                String jsonString = EntityUtils.toString(responseEntity);
                return jsonString;
            }
            else{
                //logger.error("请求返回:"+state+"("+url+")");
            }
        }
        finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

	public void doPostStartRead(String httpRequest, Configuration taskConfig, RecordSender recordSender,
			TaskPluginCollector taskPluginCollector) {
		//数据拉取大部分是GET请求，暂时不支持PUT请求 
		
	}

	public void doGetStartRead(String httpRequest, Configuration taskConfig, RecordSender recordSender,
			TaskPluginCollector taskPluginCollector) {
		String columnConfig=taskConfig.getNecessaryValue(Key.COLUMN, HttpReaderErrorCode.COLUMN_NOT_FIND_ERROR);
		JSONArray  paramsArray=JSONObject.parseArray(columnConfig);
		List<String>  columnNameList=new ArrayList<String>();
		//注意 后续要考虑columnNameList 需要根据index排序
		for (Iterator<Object> iterator = paramsArray.iterator(); iterator.hasNext(); ) {
				JSONObject next = (JSONObject) iterator.next();
				String columnName=next.getString("name");
				columnNameList.add(columnName);
		}
		
		List<ColumnEntry> column = UnstructuredStorageReaderUtil
				.getListColumnEntry(taskConfig, com.alibaba.datax.plugin.unstructuredstorage.reader.Key.COLUMN);
		// warn: no default value '\N'
				String nullFormat = taskConfig.getString(com.alibaba.datax.plugin.unstructuredstorage.reader.Key.NULL_FORMAT);
		
		String jsonType = taskConfig.getString(Key.JSON_TYPE);
		String jsonPath = taskConfig.getString(Key.JSON_PATH);
				
		// Get请求数据 读取，解析json数据
		String  resultJsonData=null;
		String signType=taskConfig.getString(Key.SIGN_TYPE,Constant.DEFAULT_SIGN_TYPE);
		if(Constant.SIGN_TYPE_01.equals(signType)) {
			String signParam=taskConfig.getString(Key.SIGN_PARAM);
			String auth=auth(httpRequest,Constant.GET,signParam);
			
			Map<String,String>   headerMap=new HashMap<String,String>();
			headerMap.put("auth", auth);
			resultJsonData=doGet(httpRequest,headerMap);
		}
		else 
		{
			resultJsonData=doGet(httpRequest,null);
		}
		JSONObject jsonObject = JSON.parseObject(resultJsonData);
		String jsonPathContext =  JSONPath.eval(jsonObject, jsonPath).toString();
		if(Constant.JSON_TYPE_ARRAY.equalsIgnoreCase(jsonType)) {
			JSONArray  dataJsonArray=JSON.parseArray(jsonPathContext);
			List<String>  parseRowsList=null;
			for (Iterator<Object> iterator = dataJsonArray.iterator(); iterator.hasNext(); ) {
				JSONObject next = (JSONObject) iterator.next();
				parseRowsList=new ArrayList<String>();
				for (String columnName : columnNameList) {
					String columnValue=next.getString(columnName);
					if(columnValue==null) {
						columnValue="null";
					}
					parseRowsList.add(columnValue);
				}
				String [] parseRowsArray=parseRowsList.toArray(new String[parseRowsList.size()]);
				UnstructuredStorageReaderUtil.transportOneRecord(recordSender,
						column, parseRowsArray, nullFormat, taskPluginCollector);
			}
		}
		else if(Constant.JSON_TYPE_OBJECT.equalsIgnoreCase(jsonType)) 
		{
			JSONObject dataJsonObject = JSON.parseObject(jsonPathContext);
			List<String>  parseRowsList=null;
			parseRowsList=new ArrayList<String>();
			for (String columnName : columnNameList) {
				String columnValue=dataJsonObject.getString(columnName);
				parseRowsList.add(columnValue);
			}
			String [] parseRowsArray=parseRowsList.toArray(new String[parseRowsList.size()]);
			UnstructuredStorageReaderUtil.transportOneRecord(recordSender,
					column, parseRowsArray, nullFormat, taskPluginCollector);
			
		}
	}
}
