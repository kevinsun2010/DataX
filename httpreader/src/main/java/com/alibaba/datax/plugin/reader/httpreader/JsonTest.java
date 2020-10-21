package com.alibaba.datax.plugin.reader.httpreader;


import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.Map;

/**
* @author wbw
* @date 2019/7/18 9:55
*/
public class JsonTest {
   private static String json = "{ \"store\": {\n" +
           "    \"book\": [ \n" +
           "      { \"category\": \"reference\",\n" +
           "        \"author\": \"Nigel Rees\",\n" +
           "        \"title\": \"Sayings of the Century\",\n" +
           "        \"price\": 8.95\n" +
           "      },\n" +
           "      { \"category\": \"fiction\",\n" +
           "        \"author\": \"Evelyn Waugh\",\n" +
           "        \"title\": \"Sword of Honour\",\n" +
           "        \"price\": 12.99\n" +
           "      },\n" +
           "      { \"category\": \"fiction\",\n" +
           "        \"author\": \"Herman Melville\",\n" +
           "        \"title\": \"Moby Dick\",\n" +
           "        \"isbn\": \"0-553-21311-3\",\n" +
           "        \"price\": 8.99\n" +
           "      },\n" +
           "      { \"category\": \"fiction\",\n" +
           "        \"author\": \"J. R. R. Tolkien\",\n" +
           "        \"title\": \"The Lord of the Rings\",\n" +
           "        \"isbn\": \"0-395-19395-8\",\n" +
           "        \"price\": 22.99\n" +
           "      }\n" +
           "    ],\n" +
           "    \"bicycle\": {\n" +
           "      \"color\": \"red\",\n" +
           "      \"price\": 19.95\n" +
           "    }\n" +
           "  }\n" +
           "}";

   
   /**
    * json字段值可以为空，但是字段名称 不能为空
    * 
    * 
    * 
    * */
   public static void main(String[] args) {
       //  获取json中store下book下的所有title值
       List<String> titless = JsonPath.read(json, "$.store.book[*].title");
       System.out.println("$.store.book.title \n " + titless);
       System.out.println();

       // 获取json中所有title的值
		/*
		 * List<String> title = JsonPath.read(json, "$..title");
		 * System.out.println("$..title \n" + title); System.out.println();
		 */

		/*
		 * // 获取json中book数组中包含isbn的所有值 List<Book> isbn = JsonPath.read(json,
		 * "$.store.book[?(@.isbn)]"); System.out.println("$.store.book[?(@.isbn)] \n" +
		 * isbn); System.out.println(); // 获取json中book数组中不包含isbn的所有值 isbn =
		 * JsonPath.read(json, "$.store.book[?(!@.isbn)]");
		 * System.out.println("$.store.book[?(!@.isbn)] \n" + isbn);
		 * System.out.println();
		 * 
		 * 
		 * // 获取json中book数组中price<10的所有值 List<Double> prices = JsonPath.read(json,
		 * "$.store.book[?(@.price < 10)].price");
		 * System.out.println("$.store.book[?(@.price < 10)].price \n" + prices);
		 * System.out.println();
		 * 
		 * // 获取json中book数组中的title等于“高效Java”的对象 List<Book> titles = JsonPath.read(json,
		 * "$.store.book[?(@.title == 'The Lord of the Rings')]");
		 * System.out.println("$.store.book[?(@.title == 'The Lord of the Rings')] \n" +
		 * titles); System.out.println();
		 * 
		 * // 获取json中store下所有price的值 prices = JsonPath.read(json, "$..price");
		 * System.out.println("$..price \n" + prices); System.out.println();
		 */

       // 获取json中book数组的前两个区间值
		/*
		 * List<Book> book = JsonPath.read(json, "$.store.book[0:4]");
		 * System.out.println("$.store.book[2:4] \n" + book); System.out.println();
		 */
       // 获取书个数
		
		  int size = JsonPath.read(json, "$.store.book.size()");
		  System.out.println("$.store.book.size() \n" + size); 
		  System.out.println();
		 
		  
		  List<String> titiles= JsonPath.read(json, "$.store.book[2:4].title");
		  
		  String str=JsonPath.read(json, "$.store.book[1].title");
		  
		  
		  System.out.println("========"+str);
		  
		  System.out.println("$.store.book[3:4].title \n " + titiles);
		  
		  
		  List<String> titlesss = JsonPath.read(json, "$.store.book[*]");
		  System.out.println(titlesss);
		  
		  List<Map<String,String>> titlessss = JsonPath.read(json, "$.store.book");//数组类型
		  for (Map<String,String> map : titlessss) {
			  System.out.println(map);
				/*
				 * String title=JsonPath.read(string, "$.title"); System.out.println(title);
				 */
		}
		  System.out.println(titlessss);

		  
		  System.out.println(JsonPath.read(json, "$.store").toString());
		  System.out.println(JsonPath.read(json, "$.store.book").toString());
		  
		 String jsonObj= JsonPath.read(json, "$.store").toString();
		 JSONObject next=(JSONObject) JSONObject.parse(jsonObj);
		 System.out.println(next.get("book").toString());
		 System.out.println(next.get("bicycle"));
		 
		 Configuration   resultJsonDataConfig=Configuration.from(jsonObj);
		 System.out.println(resultJsonDataConfig.get("book"));
		 System.out.println(resultJsonDataConfig.get("bicycle"));
		 
		 String jsonArray=JsonPath.read(json, "$.store.book").toString();
		//  Map<String,String> store=JsonPath.read(json, "$.store");  //对象类型
       // 获取store中bicycle的颜色
		/*
		 * List<String> color = JsonPath.read(json, "$.store..color");
		 * System.out.println("$.store..color \n" + color.get(0));
		 */
		  
		  //单个json 对象支持 $.store.book.title 形式获取数据
		  //jsonArray对象，支持通过List<String> titlessss = JsonPath.read(json, "$.store.book");获取jsonArray数组
   }
}
