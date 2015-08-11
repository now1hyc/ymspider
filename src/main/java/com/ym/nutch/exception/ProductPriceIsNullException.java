package com.ym.nutch.exception;

/**
 * 
 * 功能 自定义异常 ProductPriceIsNullException
 *
 *@author yangchao
 *@date 创建日期 Jul 26, 2012
 *	
 *@version 1.0
 */
public class ProductPriceIsNullException extends Exception {
	public ProductPriceIsNullException() {
		super();
	}

	public ProductPriceIsNullException(String message) {
		super(message);
	}
	
	public ProductPriceIsNullException(String message, Throwable cause) {
		super(message, cause);
	}
}
