package org.httpreader;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSONArray;

public class JsonTest {
	public static void main(String[] args) {
		//String requestParams="[{\"name\":\"name\",\"value\":\"zhangsan\"},{\"name\":\"age\",\"value\":\"25\"}]";
		//String url=getFullHttpRequestUrl("http://www.baidu.com","GET",requestParams);
		//getAllHttpRequests(url,"GET");
		
		//String jsonresult=doGet("http://127.0.0.1:8080/users1?pageNo=1&pageSize=10");
		//System.out.println("jsonresult=="+jsonresult);
		
		/*
		 * String column="[\"id\",\"name\",\"age\"]"; JSONArray
		 * paramsArray=JSONObject.parseArray(column); // 遍历JSONArray for
		 * (Iterator<Object> iterator = paramsArray.iterator(); iterator.hasNext(); ) {
		 * String next = (String) iterator.next(); System.out.println(next.toString());
		 * }
		 */
		//String url="http://127.0.0.1:8080/users?name=zhangsan&age=25&pageNo=1&pagesize=1000";
		//doGet(url);
		
		String resultJsonData="{	\"list\": [{			\"owner\": \"SYS\",			\"tableName\": \"C_OBJ#\",			\"columnName\": \"OBJ#\",			\"dataType\": \"NUMBER\"		},		{			\"owner\": \"SYS\",			\"tableName\": \"TAB$\",			\"columnName\": \"FILE#\",			\"dataType\": \"NUMBER\"		},		{			\"owner\": \"SYS\",			\"tableName\": \"TAB$\",			\"columnName\": \"DEGREE\",			\"dataType\": \"NUMBER\"		},		{			\"owner\": \"SYS\",			\"tableName\": \"TAB$\",			\"columnName\": \"OBJ#\",			\"dataType\": \"NUMBER\"		}	],	\"pageTotal\": 2014,	\"total\": 0,	\"pageSize\": 1000,	\"pageNo\": 1}";
		Configuration   resultJsonDataConfig=Configuration.from(resultJsonData);
		JSONArray  paramsArray=(JSONArray) resultJsonDataConfig.get("list");
		System.out.println(paramsArray);
	}
}
