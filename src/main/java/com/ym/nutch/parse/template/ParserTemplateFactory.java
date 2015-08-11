package com.ym.nutch.parse.template;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

import com.ym.nutch.parse.template.XMNutchTemplateSettor.Ant;
import com.ym.nutch.plugin.util.URLUtil;


public class ParserTemplateFactory {

	public static final Logger Log = LoggerFactory.getLogger(ParserTemplateFactory.class);
	
	private static Map<String,Ant> map = XMNutchTemplateSettor.templateSettor;	
	private static Map<String,Ant> mapStdProductName = XMNutchTemplateSettor.templateNameSettor;
	
	/**
	 * 存放商品详情解析模板
	 */
	private static Map<String,Class> templateParseProduct = new HashMap<String,Class>();
	
	/**
	 * 存放获取标准商品名称解析模板
	 */
	private static Map<String,Class> templateParseProductName = new HashMap<String,Class>();
	
	
	// 加载模板实现类
	public static void loadTemplateClass(){
		for(Entry<String, Ant> en : map.entrySet()){
			String clasz = en.getValue().getParserImpl();
			try {
				Class tmp = Class.forName(clasz);
				templateParseProduct.put(clasz, tmp);
			} catch (Exception e) {
				Log.error("load template class not found:" + clasz);
				continue;
			}
		}
		Log.info("load template class ok");
	}
	
	/**
	 * 获取商品网站解析模板
	 * @param base
	 * @param root
	 * @param content
	 * @return
	 */
	public static TemplateParser getTemplateParser(URL base,DocumentFragment root,String content){
		String domain = null, host = null;
		Ant ant = null;
		Class class1 = null;
		TemplateParser templateParser = null;
		/**解析模板，得到class对象**/
		try{
			if(map.size()==0){
				XMNutchTemplateSettor.loadNutchTemplateXmlFile();
//				Log.warn("getTemplateParser load and init parser error");
			}
			
			domain = URLUtil.getDomainName(base);
			Log.info("domain=" + domain);
			ant = (Ant) map.get(domain.toLowerCase());			
			if(ant==null){
				host = base.getHost().toLowerCase();
				Log.info("host=" + host);
				ant = (Ant) map.get(host);	
			}
		    class1=templateParseProduct.get(ant.getParserImpl());

			if(class1==null){
				class1=Class.forName(ant.getParserImpl());
				templateParseProduct.put(ant.getParserImpl(), class1);
			}
		} catch(Exception ex){
			Log.error("getTemplateParser error host=" + host + ",domain=" + domain + " no suitable parser", ex);
			return null;
		}
		
		/**通过class对象加载实现类**/
		try{
			if("DocumentFragment".equals(ant.getMparas())){
				Constructor sConstructor=class1.getConstructor(DocumentFragment.class);
				templateParser = (TemplateParser) sConstructor.newInstance(root);
				return templateParser;
			}
			return null;
		}catch(Exception ex){
			Log.error("getTemplateParser create TemplatParser error",ex);
			return null;
		}
	}
	
	/**
	 * 获取非商品网站解析模板
	 * @param base
	 * @param root
	 * @param content
	 * @return
	 */
	public static TemplateParser getTemplateParserByIsProduct(URL base){
		String domain = null, host = null;
		Ant ant = null;
		Class class1 = null;
		TemplateParser templateParser = null;
		/**解析模板，得到class对象**/
		try{
			if(map.size()==0){
				XMNutchTemplateSettor.loadNutchTemplateXmlFile();
//				Log.warn("getTemplateParser load and init parser error");
			}
			
			domain = URLUtil.getDomainName(base);
			Log.info("domain=" + domain);
			ant = (Ant) map.get(domain.toLowerCase());			
			if(ant==null){
				host = base.getHost().toLowerCase();
				Log.info("host=" + host);
				ant = (Ant) map.get(host);	
			}
		    class1=templateParseProduct.get(ant.getParserImpl());

			if(class1==null){
				class1=Class.forName(ant.getParserImpl());
				templateParseProduct.put(ant.getParserImpl(), class1);
			}
		} catch(Exception ex){
			Log.error("getTemplateParser error host=" + host + ",domain=" + domain + " no suitable parser", ex);
			return null;
		}
		
		/**通过class对象加载实现类**/
		try{
			if("DocumentFragment".equals(ant.getMparas())){
				Constructor sConstructor=class1.getConstructor();
				templateParser = (TemplateParser) sConstructor.newInstance();
				return templateParser;
			}
			return null;
		}catch(Exception ex){
			Log.error("getTemplateParser create TemplatParser error",ex);
			return null;
		}
	}
	
	
	/**
	 * 获取网站解析模板,走默认的构造函数
	 * @param base
	 * @param root
	 * @param content
	 * @return
	 */
	public static TemplateParser getTemplateParserDefaultConstructor(URL base){
		String domain = null, host = null;
		Ant ant = null;
		Class class1 = null;
		TemplateParser templateParser = null;
		/**解析模板，得到class对象**/
		try{
			if(map.size() == 0){
				XMNutchTemplateSettor.loadNutchTemplateXmlFile();
//				Log.warn("getTemplateParserDefaultConstructor load and init parser error");
			}

			domain = URLUtil.getDomainName(base);
			Log.info("domain=" + domain);
			ant = (Ant) map.get(domain.toLowerCase());
			
			if(ant == null){
				host = base.getHost().toLowerCase();
				Log.info("host=" + host);
				ant = (Ant) map.get(host);
			}
			
		    class1=templateParseProduct.get(ant.getParserImpl());

			if(class1==null){
				class1=Class.forName(ant.getParserImpl());
				templateParseProduct.put(ant.getParserImpl(), class1);
			}
		}catch(Exception ex){
			Log.error("getTemplateParserDefaultConstructor error host=" + base.getHost() + ",domain=" + domain + " no suitable parser", ex);
			return null;
		}
		
		/**通过class对象加载实现类**/
		try{
			templateParser = (TemplateParser) class1.newInstance();
			/*if("DocumentFragment".equals(ant.getMparas())){
				Constructor sConstructor=class1.getConstructor(DocumentFragment.class);
				templateParser = (TemplateParser) sConstructor.newInstance(root);
			}else if("String".equals(ant.getMparas())){
				Constructor sConstructor=class1.getConstructor(String.class);
				templateParser = (TemplateParser) sConstructor.newInstance(content.getContent());
			}*/
			return templateParser;
		}catch(Exception ex){
			Log.error("getTemplateParserDefaultConstructor create TemplatParser error",ex);
			return null;
		}
	}
	
	
	/**
	 * 获取标准商品名称解析模板
	 * @param base
	 * @param root
	 * @param content
	 * @return
	 */
	public static TemplateParser getTemplateForProductName(URL base,DocumentFragment root){
		
		Ant ant = null;
		Class class1 = null;
		TemplateParser templateParser = null;
		/**解析模板，得到class对象**/
		try{
			if(mapStdProductName.size()==0){
				XMNutchTemplateSettor xm = new XMNutchTemplateSettor();
				xm.loadNutchTemplateXmlFile();
			}
			
			ant = (Ant) mapStdProductName.get(base.getHost().toLowerCase());
			
		    class1=templateParseProductName.get(ant.getParserStandartNameImpl());
			
			if(class1==null){
				class1=Class.forName(ant.getParserStandartNameImpl());
				templateParseProductName.put(ant.getParserStandartNameImpl(), class1);
			}
		}catch(Exception ex){
			Log.warn(ParserTemplateFactory.class.getName()+"-->getTemplateForProductName parse error!!",ex.getCause());
			return null;
		}
		
		/**通过class对象加载实现类**/
		try{
			if("DocumentFragment".equals(ant.getMparas())){
				Constructor sConstructor = class1.getConstructor(DocumentFragment.class);
				templateParser = (TemplateParser) sConstructor.newInstance(root);
				return templateParser;
			}
//			else if("String".equals(ant.getMparas())){
//				Constructor sConstructor=class1.getConstructor(String.class);
//				templateParser = (TemplateParser) sConstructor.newInstance(content.getContent());
//			}
			return null;
		}catch(Exception ex){
			Log.warn(ParserTemplateFactory.class.getName()+" -->getTemplateForProductName create TemplatParser error!!",ex.getCause());
			return null;
		}
	}
	
}
