package com.alibaba.datax.plugin.reader.httpreader;


import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;

import java.util.List;
import java.util.Map;

/**
* @author wbw
* @date 2019/7/18 9:55
*/
public class FastJsonTest {
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

       JSONObject jsonObject = JSON.parseObject(json);

       JSONObject storeJsonObject = (JSONObject) JSONPath.eval(jsonObject, "$.store");
       String book=storeJsonObject.getString("book").toString();
       System.out.println(book);
       //System.out.println(jsonObject.toString());
       String storestr= JSONPath.eval(jsonObject, "$.store").toString();
       System.out.println(storestr);
       
       String str= JSONPath.eval(jsonObject, "$.store.book").toString();
       System.out.println(str);
      
   }
}
