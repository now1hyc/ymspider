package com.ym.framework.interfaces;

/**
 * 商品列表页接口
 * @author Administrator
 *
 */
public interface ICrawlerList {

	//rawUrl  网页地址     sellerCode  电商编码
	public String isCrawlProduct(String sellerCode,String rawUrl);
}
