package com.ym.nutch.parse.template;

import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductNameIsNullException;
import com.ym.nutch.exception.ProductPriceIsNullException;

public interface XMParser {
	
	public boolean urlFilter(String target);
	
	public boolean isProductPage(String url);
	
	public String isCrawlProduct(String url);   //判断是否是商品url：1为非商品，2为商品
	
	public String formatUrl(String url);
	
//-----------------------------------------------------------------------------------------------
	public void setId()throws ProductIDIsNullException;//product.setId and product.setPid
	
	public void setBrand() ;
	
	public void setClassic();//product.setClassic .setClassicCode;
	
	public void setShortName( );
	
	public void setTitle( ) ;
	
	public void setProductName( ) throws ProductNameIsNullException;
	
	public void setKeyword();
	
	public void setOrgPic() ;//product.setBigpic .setSmallpic ,启动LoadingPicServer.addPicUrl();
	
	public void setPrice() throws ProductPriceIsNullException;
	
	public void setMaketPrice();
	
	public void setDiscountPrice() ;
	
	public void setMparams();

	public void setContents(); 

	public void setSeller() ; //product.setSeller   and .setSellerCode 
	
	public void setCreatTime();
	
	public void setUpdateTime() ;
	
	public void setStatus() ;
	
	public void setIsCargo();//是否有货   有货1  无货0
	
	public void secondDeal();//二次处理,仅仅处理 分类(category)、产类型号、产品品牌、短名称
	
	public void setCategory(); //处理原始分类
	
	public void setOriCatCode(); // 20130923修改，处理商品原始分类码
	
	public void setCheckOriCatCode();// 20131029修改，处理商品中文编码混合原始分类码
	
	public void setshortNameDetail();// 20131106修改，处理商品的第二种短名称
	
	public void setD1ratio();//20131113添加，活动售价的折扣
	
	public void setD2ratio();//20131113添加，折扣活动截止时间
	
	public void setLimitTime();//20131113添加，当前销售价的折扣
	
	public void setTotalComment();//20131120添加，处理商品的总评论数
	
	public void setCommentStar();//20131120添加，处理商品的评论级别：星级标准
	
	public void setCommentPercent();//20131120添加，处理商品的评论级别：百分比标准
	
	public void setCommentList();
	
}
