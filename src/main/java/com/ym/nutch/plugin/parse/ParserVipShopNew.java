package com.ym.nutch.plugin.parse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.plugin.util.Constant;
import com.ym.nutch.plugin.util.TemplateUtil;

public class ParserVipShopNew extends ParserParent{

	public static final Logger LOG = LoggerFactory.getLogger(ParserVipShopNew.class);

	
    //	电脑版商品url：http://www.vip.com/detail-51308-5947277.html
	private Pattern itemFlag1 = Pattern.compile("http://www.vip.com/detail-([0-9]+)-([0-9]+)\\.html.*?");
	
	//手机端商品url：http://m.vip.com/product-136126-17935477.html
	private Pattern itemFlag2 = Pattern.compile("http://m.vip.com/product-([0-9]+)-([0-9]+)\\.html.*?");
		
	public ParserVipShopNew() {
		super();
		init();
	}

	public ParserVipShopNew(DocumentFragment doc) {
		super(doc);
		init();
	}
	
	public void init() {
		 sellerCode = "1071";
		 seller = "唯品会";
    	 classicXpath = "//DIV[@class='M_class']";
    	 keywordXpath = "//META[@name='keywords']/@content";
    	 imgXpath = "(//A[@class='J_mer_bigImgZoom'])[1]/@href";
    	 contentDetailXpath = "//DIV[@class='M_detailCon']";
    	 marketPriceXpath="//SPAN[@class='pbox_market']/DEL";
    	 priceXpath="//SPAN[@class='pbox_price']/EM";
    	 productNameXpath="//P[@class='pib_title']"+" | //P[@class='pib_title_detail']";
    	 
    	 titleFilter.add("_唯品会");
    	 productFlag = itemFlag1;
    	
    	 productUrlList.add(itemFlag1);
    	 productUrlList.add(itemFlag2);
	}
	
	@Override
	public void setId() throws ProductIDIsNullException {
		String pid = null;
		url="http://www.vip.com/detail-136134-17909164.html";
		Matcher ma1 = productFlag.matcher(url.toLowerCase());
		if (ma1.find()) {
			pid = ma1.group(2);
		}
		if (pid == null || pid.isEmpty()) {
			throw new ProductIDIsNullException(url + ":未能获取商品id！！");
		} else {
			String id = sellerCode + pid;
			super.product.setPid(id);
			super.product.setOpid(pid);
		}
	}
	
	@Override 
	public void setClassic() {
		Node node_location = TemplateUtil.getNode(root,classicXpath); 
		if(node_location!=null){
			String value = node_location.getTextContent().replaceAll("[\\s]{2,}"," ");
			value = value.replaceAll("[\\r\\n]+","");
			if(classFilter.size()>0){
				for(String filterString:classFilter){
					value = value.replaceAll(filterString, "");
				}
			}
			value=value.replaceAll("首页(\\s{0,}) >", "");
			value = value.replaceAll(">",Constant.XMTAG);
			product.setClassic(value);
			
		}
	}
	
}
