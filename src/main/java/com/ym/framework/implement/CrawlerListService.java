package com.ym.framework.implement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ym.framework.interfaces.ICrawlerList;
import com.ym.nutch.parse.template.ParserTemplateFactory;
import com.ym.nutch.parse.template.TemplateParser;
import com.ym.nutch.parse.template.XMNutchTemplateSettor;
import com.ym.nutch.parse.template.XMNutchTemplateSettor.Ant;
import com.ym.nutch.plugin.util.StrUtil;

public class CrawlerListService implements ICrawlerList {

	public static final Logger LOG = LoggerFactory.getLogger(CrawlerListService.class);
	
	//加载配置文件
	private static HashMap<String, Ant> antByMerchantSettor=XMNutchTemplateSettor.getCodeName_settor();
	
	//判断是否是商品页 isCdt：1为非商品，2为商品
	@Override
	public String isCrawlProduct(String sellerCode,String rawUrl) {
		String isCdt="1";
		LOG.info("Determine Product Page Start! rawUrl="+rawUrl);
		long beginTime=System.currentTimeMillis();
		if(!StrUtil.isEmpty(sellerCode)){
		 try {
			Ant ant=antByMerchantSettor.get(sellerCode);
			if(ant!=null){
				if(!StrUtil.isEmpty(rawUrl)){
					try {
						isCdt = getParse(rawUrl,ant);
						long endTime=System.currentTimeMillis();
						LOG.info("Determine Product success,timecost="+(endTime-beginTime)+"ms");
					} catch (Exception ex) {
					  LOG.error("Crawl Product Details Is ERROR ! url=" + rawUrl);
					  LOG.error("ERROR：" + ex);
					}
				}else{
					LOG.info("Request the network fails! content = null! url-->"+rawUrl);
				}
			}else{
				LOG.info("The Fetcher Url Get Failure By UrlQueue !");
			}
	      } catch (Exception e) {
		    e.printStackTrace();
		    return isCdt;
	      }
		}else{
			LOG.info("code is null ,fetcher over! url-->"+rawUrl);
		}
		return isCdt;
	}
	
	public static String getParse(String url,Ant ant) {
		URL base=null;
		try {
			base = new URL(url);
		} catch (MalformedURLException e) {
			LOG.error("URL Return BaseUrl is erro!!", e.getCause());
			return "1";
		}
		LOG.info("enter HtmlParser ,url=" + url);
		TemplateParser templateParser = null;
		try {//TODO 获取解析模板  提取商品详情信息并入库
			templateParser = ParserTemplateFactory.getTemplateParserByIsProduct(base);
			if(templateParser==null){
				throw new RuntimeException("templateParser not found");
			}
			try{
				// 全部由子类实现：获取页面的
				String isProduct = templateParser.isCrawlProduct(url);
				return isProduct;
			}catch(Exception e){
				return "1";
		    }
		} catch (Exception e) {
			LOG.error("templateParser is erro!!", e.getCause());
			return "1";
		}
	}
	
	public static void main(String[] args) {
		CrawlerListService crawlerListService= new CrawlerListService();
		String sellerCode="1001";
		String rawUrl="http://m.suning.com/product/104083059.html";
		String aa = crawlerListService.isCrawlProduct(sellerCode, rawUrl);
		System.out.println("是否是商品页面(1为非商品，2为商品):"+aa);
	}
}
