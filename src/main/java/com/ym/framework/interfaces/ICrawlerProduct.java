package com.ym.framework.interfaces;

import com.ym.nutch.obj.OriProduct;


/**
 * 商品详情接口
 * @author Administrator
 *
 */
public interface ICrawlerProduct {
	
	public OriProduct crawlerProduct(String sellerCode,String productUrl);
	
	public void productStorage(OriProduct product);  //商品入库
}
