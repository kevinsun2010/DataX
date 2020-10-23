package org.httpreader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;

public class SignTest {

	public static Map<String,String>   keyMap=new HashMap<String,String>();
	public static void main(String[] args) {
		//appId:20201021   key:20201021CAIWUBAOBIAO
		
		// TODO Auto-generated method stub
//{sign:'3b658189b279cf96195e0b6094d97131a41d6b8c',keyTime:'1603266826514',appId:'20201021'}
		//http://localhost:18012/api/project/baseInfo/list?date=2020-10-20&pageNo=1&pageSize=20  
		String appId="20201021";
		String key="20201021CAIWUBAOBIAO";
		keyMap.put(appId, key);
		
		
		String method="GET";//是否区分大小写
		String keyTime=System.currentTimeMillis()+100000+"";//100s有效期
		Map<String,Object> param=new HashMap<String,Object>(); 
		param.put("pageNo", "1");
		param.put("pageSize", "20");
		String sign=encry(param,method,keyTime,appId);
		
		Map<String,Object> authMap=new HashMap<String,Object>();
		//Map 对象存入 用户名,密码,电话号码
		authMap.put("sign", sign);
		authMap.put("keyTime",keyTime );
		authMap.put("appId", appId);
		authMap.putAll(param);
		
		//Map 转成  JSONObject 字符串
		JSONObject jsonObj=new JSONObject(authMap);
		String auth=jsonObj.toString();
		
		String url="http://test.srv.jarvis-api.idc.oa.com/api/PCGVideoService/1.0/projectBaseInfoList?pageNo=1&pageSize=20";
		doGet(url,auth);
		
	}
	public static String encry(Map<String,Object> param,String method,String keyTime,String appId) {
		
		String signKey=HmacUtils.hmacSha1Hex(keyMap.get(appId), keyTime);
		String httpString =method + "\n" +getParamStr(param);//怎么转
		String toSign=keyTime+"\n"+DigestUtils.sha1Hex(httpString);
		String sign= HmacUtils.hmacSha1Hex(signKey, toSign);
		return sign;
	}
	
	
	public static String getParamStr(Map<String,Object> param) {
		Set set=param.keySet();
        Object[] arr= set.toArray();
        Arrays.sort(arr);
		
		StringBuffer sbParams=new StringBuffer();
		 for(Object key:arr){
			 sbParams.append(key + "=" + param.get(key) +"&");
	        }
		
			/*
			 * for(Map.Entry<String, Object> entry : param.entrySet()){ String mapKey =
			 * entry.getKey(); String mapValue = (String) entry.getValue();
			 * System.out.println(mapKey+":"+mapValue); sbParams.append(mapKey);
			 * sbParams.append("="); sbParams.append(mapValue); sbParams.append("&"); }
			 */
		
		return sbParams.toString().substring(0, sbParams.length()-1);
		
	}
	 public static String doGet(String url,String sign) {
	        try {
	            HttpClient client = new DefaultHttpClient();
	            client = HttpClients.createDefault();
	            
	            //发送get请求
	            HttpGet request = new HttpGet(url);
	            request.addHeader("auth", sign);
	            HttpResponse response = client.execute(request);

	            /**请求发送成功，并得到响应**/
	            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
	                /**读取服务器返回过来的json字符串数据**/
	                String strResult = EntityUtils.toString(response.getEntity());
	                System.out.println("Http Get Result===="+strResult);
	                return strResult;
	            }
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }

	        return null;
	    }
	
}
