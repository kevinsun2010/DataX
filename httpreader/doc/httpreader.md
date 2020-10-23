# DataX HttpReader 插件文档


------------

## 1 快速介绍

HttpReader提供了读取Http接口数据的能力。在底层实现上，HttpReader调用http接口获取数据，并转换为DataX传输协议传递给Writer。

**目前HttpReader支持的数据格式为标准json类型格式的数据，支持将json对象或者json数组解析成结构化数据，即一张逻辑意义上的二维表。**

**HttpReader需要Jdk1.8及以上版本的支持。**


## 2 功能与限制

HttpReader实现了从http接口读取数据并转为DataX协议的功能。支持分页读取和非分页读取，目前HttpReader支持的功能如下：

1. 支持json数据格式，且要求文件内容存放的是一张逻辑意义上的二维表。

2. 支持多种类型数据读取(使用String表示)，支持列裁剪，支持列常量

3. 分页读取可以支持并发读取。

4. 当前支持接口认证配置 

我们暂时不能做到：

1. 对于不同接口规范，主要是认证方式需要定制化开发，无法做到兼容所有。

## 3 功能说明


### 3.1 配置样例

```json
{
    "job": {
        "setting": {
            "speed": {
                "channel": 3
            }
        },
        "content": [
            {
                "reader": {
					"name": "httpreader",
					"parameter": {
						"url": "http://test.srv.jarvis-api.idc.oa.com/api/PCGVideoService/1.0/projectBaseInfoList",
						"requestType": "GET",
						"jsonType":"ARRAY",
						"jsonPath":"$.data",
						"column": [{"name":"traceId","index":0,"type":"string"},
                                   {"name":"dataScope","index":1,"type":"string"},
                                   {"name":"profileId","index":2,"type":"string"},
                                   {"name":"positionId","index":3,"type":"string"},
                                   {"name":"organizationId","index":4,"type":"string"},
                                   {"name":"userId","index":5,"type":"string"}
],
						"nullFormat":"null",
						"signType": "01",
						"signParam": {"appId":"20201021","key":"20201021CAIWUBAOBIAO"},
						"havePage": "true",
						"pageSize": "1000",
						"pageNoColumn": "pageNo",
						"pageSizeColumn": "pageSize",
						"pageTotalColumn": "pageTotal"
						
					}
				},,
                "writer": {
                    "name": "streamwriter",
                    "parameter": {
                        "print": true
                    }
                }
            }
        ]
    }
}
```

### 3.2 参数说明（各个配置项值前后不允许有空格）

* **url**

	* 描述：要获取数据的http请求路径，如果要分页读取，需要接口支持分页查询，并配置分页相关配置信息。。 <br />

		**特别需要注意的是，DataX会将一个作业下同步的接口返回数据视作同一张数据表。用户必须自己保证所有的json数据能够适配同一套schema信息。**


	* 必选：是 <br />

	* 默认值：无 <br />

* **requestType**

	* 描述：http请求方法类型参数，支持GET、PUT两种。 <br />


	* 必选：是 <br />

	* 默认值：GET <br />

* **requestParam**

	* 描述：请求参数列表，必须是json格式。 <br />

		比如：[{"name":"updatetime","value":"20201010"},{"name":"age","value":"25"}]

	* 必选：否 <br />

	* 默认值：无 <br />


* **column**

	* 描述：读取字段列表，type指定源数据的类型，index指定当前列来自于文本第几列(以0开始)，value指定当前类型为常量，不从源头文件读取数据，而是根据value值自动生成对应的列。 <br />

		默认情况下，用户可以全部按照String类型读取数据，配置如下：

		```json
			"column": ["*"]
		```

		用户可以指定Column字段信息，配置如下：

		```json
{
  "type": "long",
  "index": 0    //从本地文件文本第一列获取int字段
},
{
  "type": "string",
  "value": "alibaba"  //HdfsReader内部生成alibaba的字符串字段作为当前字段
}
		```

		对于用户指定Column信息，type必须填写，index/value必须选择其一。

	* 必选：是 <br />

	* 默认值：全部按照string类型读取 <br />

* **jsonType**

	* 描述：接口返回数据json类型，目前支持OBJECT，ARRAY两种 <br />

	**OBJECT类型，会将json数据解析为一行数据，对应数据表中的一条数据。ARRAY将json数据解析成多行数据，对应数据表中多条数据**

	* 必选：否 <br />

	* 默认值：, <br />


* **jsonPath**

	* 描述：读取json数据的路径path。<br /> 比如：$.data，$.data.list

 	* 必选：否 <br />

 	* 默认值：无 <br />


* **nullFormat**

	* 描述：文本文件中无法使用标准字符串定义null(空指针)，DataX提供nullFormat定义哪些字符串可以表示为null。<br />

		 例如如果用户配置: nullFormat:"\\N"，那么如果源头数据是"\N"，DataX视作null字段。

 	* 必选：否 <br />

 	* 默认值：无 <br />

* **signType**

	* 描述：认证方式<br />
 
		 01代表影视分析数据接口认证方式，00代表不需要认证

 	* 必选：否 <br />
 
 	* 默认值：00 <br />

* **signParam**

	* 描述：接口认证相关参数<br />

 	* 必选：否 <br />
 
 	* 默认值：无 <br />

* **havePage**

	* 描述：是否分页查询 <br />

 	* 必选：否 <br />
 
 	* 默认值：false <br />

* **pageSize**

	* 描述：分页查询，一次分页查询数据条数。建议1000-10000之间<br />

 	* 必选：havePage为true时，必选 <br />
 
 	* 默认值：无 <br />
	
* **pageNoColumn**

	* 描述：页码字段字段名称。<br />

	* 必选：否 <br />
 
 	* 默认值：pageNo <br />

* **pageSizeColumn**

	* 描述：单页数据条数字段名称。<br />

 	* 必选：否 <br />
 
 	* 默认值：pageSize <br />
 	
* **pageTotalColumn**

	* 描述：总的分页页数字段名称。<br />

 	* 必选：否 <br />
 
 	* 默认值：pageTotal <br />
        






### 3.4 按分分页读取




## 4 性能报告



## 5 约束限制

略

## 6 FAQ



