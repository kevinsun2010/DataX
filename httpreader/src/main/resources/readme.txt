规范
1 请求方法，目前主要支持get方式

2 安全认证方式（apikey+密钥加密方式）

3 数据格式标准json

4  分页问题
并发数：
总的分页数：
根据请求返回的的分页数进行查询：

如何获取总的分页数问题。




"jsonTypy":"OBJECT|ARRAY",

OBJECT 直接转为Map<String,String>,
ARRAY 转为List<Map<String,String>>

jsonPath :

List<Map<String,String>> titlessss = JsonPath.read(json, "$.store.book");//数组类型
for (Map<String,String> map : titlessss) {
		System.out.println(map);
				
}
		  

		  
Map<String,String> store=JsonPath.read(json, "$.store");  //对象类型
System.out.println(store);