package com.ym.nutch.exception;



/**
 * 
 * 功能 自定义异常 ProductIDIsNullException
 *
 *@author yangchao
 *@date 创建日期 Jul 25, 2012
 *	
 *@version 1.0
 */
public class ProductIDIsNullException extends Throwable{
    public ProductIDIsNullException() {
        super();
     }
    
    public ProductIDIsNullException(String message) {
        super(message);
     }
    
    public ProductIDIsNullException(String message, Throwable cause) {
        super(message, cause);
     }
    
}
