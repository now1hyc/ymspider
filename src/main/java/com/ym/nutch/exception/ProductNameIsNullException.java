package com.ym.nutch.exception;


/**
 * 
 * 功能 自定义异常 ProductNameIsNullException
 *
 *@author yangchao
 *@date 创建日期 Jul 26, 2012
 *	
 *@version 1.0
 */
public class ProductNameIsNullException extends Exception {
	public ProductNameIsNullException() {
		super();
	}

	public ProductNameIsNullException(String message) {
		super(message);
	}
	
	public ProductNameIsNullException(String message, Throwable cause) {
		super(message, cause);
	}
}
