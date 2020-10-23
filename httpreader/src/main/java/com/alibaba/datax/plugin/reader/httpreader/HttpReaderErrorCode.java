package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.spi.ErrorCode;

public enum HttpReaderErrorCode implements ErrorCode {
	
	
	REQUEST_URL_NOT_FIND_ERROR("HttpReader-01", "您未配置url值"),
	REQUEST_TYPE_NOT_FIND_ERROR("HttpReader-02", "您未配置requestType值"),
	REQUEST_PARAM_NOT_FIND_ERROR("HttpReader-03", "您未配置requestParam值"),
	CONFIG_INVALID_EXCEPTION("HttpReader-04", "参数配置错误"),
	
	REQUEST_TYPE_UNSUPPORT("HttpReader-07", "请求方式配置错误"),
	COLUMN_NOT_FIND_ERROR("HttpReader-08", "数据列配置错误"),
	
	JSON_TYPE_NOT_FIND_ERROR("HttpReader-09", "您未配置jsonType值"),
	JSON_PATH_NOT_FIND_ERROR("HttpReader-10", "您未配置jsonPath值"),
	
	
	ILLEGAL_VALUE("HttpReader-05", "值错误"),
	REQUIRED_VALUE("HttpReader-06", "您缺失了必须填写的参数值."),
    BAD_CONFIG_VALUE("HttpReader-00", "您配置的值不合法.");
	
	
	/*
	 * PATH_NOT_FIND_ERROR("HdfsReader-01", "您未配置path值"),
	 * DEFAULT_FS_NOT_FIND_ERROR("HdfsReader-02", "您未配置defaultFS值"),
	 * REQUIRED_VALUE("HdfsReader-05", "您缺失了必须填写的参数值."),
	 * NO_INDEX_VALUE("HdfsReader-06","没有 Index" ),
	 * MIXED_INDEX_VALUE("HdfsReader-07","index 和 value 混合" ),
	 * EMPTY_DIR_EXCEPTION("HdfsReader-08", "您尝试读取的文件目录为空."),
	 * PATH_CONFIG_ERROR("HdfsReader-09", "您配置的path格式有误"),
	 * READ_FILE_ERROR("HdfsReader-10", "读取文件出错"),
	 * MALFORMED_ORC_ERROR("HdfsReader-10", "ORCFILE格式异常"),
	 * FILE_TYPE_ERROR("HdfsReader-11", "文件类型配置错误"),
	 * KERBEROS_LOGIN_ERROR("HdfsReader-13", "KERBEROS认证失败"),
	 * READ_SEQUENCEFILE_ERROR("HdfsReader-14", "读取SequenceFile文件出错"),
	 * READ_RCFILE_ERROR("HdfsReader-15", "读取RCFile文件出错"),;
	 */

    private final String code;
    private final String description;

    private HttpReaderErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s]. ", this.code,
                this.description);
    }
}