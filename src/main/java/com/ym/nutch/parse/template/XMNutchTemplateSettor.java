package com.ym.nutch.parse.template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ym.framework.util.XmlUtil;


public class XMNutchTemplateSettor implements Serializable, Cloneable {

	private static final long serialVersionUID = 5783803307701976735L;

	public static final Logger Log = LoggerFactory.getLogger(XMNutchTemplateSettor.class);

	/**
	 * 解析商品详情模板
	 */
	public static HashMap<String, Ant> templateSettor = new HashMap<String, Ant>();

	/**
	 * 解析标准商品名称模板
	 */
	public static HashMap<String, Ant> templateNameSettor = new HashMap<String, Ant>();
	
	/**
	 * 解析商品code值
	 */
	public static HashMap<String, Ant> codeNameSettor = new HashMap<String, Ant>();

	public static void loadNutchTemplateXmlFile() {
		SAXReader xmlReader = new SAXReader();
		Document xDoc = null;
		try {
			xDoc = xmlReader.read(XMNutchTemplateSettor.class.getClassLoader()
					.getResourceAsStream("merchantsettor.xml"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Element rootByElement = xDoc.getRootElement();
		for (Iterator<Element> j = rootByElement.elementIterator(); j.hasNext();) {
			Element itemElement = j.next();
			
			// 先过滤取消商家的抓取
			if ("0".equals(XmlUtil.getElement(itemElement.element("isUse"),
					"int"))) {
				continue;
			}

			Ant ant = new Ant();
			ant.setDomain(XmlUtil.getElement(itemElement.element("domain"), "String"));
			ant.setHost(XmlUtil.getElement(itemElement.element("host"), "String"));	
			ant.setName(XmlUtil.getElement(itemElement.element("name"),"String"));
			ant.setCode(XmlUtil.getElement(itemElement.element("code"),"String"));
			ant.setUrl_charset(XmlUtil.getElement(itemElement.element("url_charset"), "String"));
			ant.setContent_charset(XmlUtil.getElement(itemElement.element("content_charset"), "String"));
			ant.setParserImpl(XmlUtil.getElement(itemElement.element("parserImpl"), "String"));
			ant.setParserStandartNameImpl(XmlUtil.getElement(itemElement.element("parserStandartNameImpl"), "String"));
			ant.setMparas(XmlUtil.getElement(itemElement.element("mparas"), "String"));
			ant.setIsUse(XmlUtil.getElement(itemElement.element("isUse"),"String"));

			if(!"".equals(ant.getCode())){
				codeNameSettor.put(ant.getCode(), ant);
			}
			if(!"".equals(ant.getHost())){
				   String[] hosts=ant.getHost().split(";");
				   for(int k=0;k<hosts.length;k++){
					   templateSettor.put(hosts[k],ant);
					   if(ant.parserStandartNameImpl!=null && !ant.getParserStandartNameImpl().isEmpty()){
						   templateNameSettor.put(hosts[k],ant);
					   }
				   }
			   }
			   
			   if(!"".equals(ant.getDomain())){
				   String[] domains = ant.getDomain().split(";");
				   for(int k=0;k<domains.length;k++){
					   templateSettor.put(domains[k], ant);
					   if(ant.parserStandartNameImpl!=null && !ant.getParserStandartNameImpl().isEmpty()){
						   templateNameSettor.put(domains[k],ant);
					   }
				   }
			   }
		}
		Log.info("load xm-nutch-template config ok");
		// 20130816：加载完配置文件，加载模板类到内存，缓存
		ParserTemplateFactory.loadTemplateClass();
	}

	public static HashMap<String, Ant> getCodeName_settor() {
		loadNutchTemplateXmlFile();
		return codeNameSettor;
	}

	public static class Ant implements Cloneable {
		private String host;
		private String domain;
		private String name;
		private String code;
		private String grade;
		private String url_charset;
		private String content_charset;
		private String parserImpl;
		private String parserStandartNameImpl;
		private String mparas;
		private String isUse;

		public Ant() {
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getGrade() {
			return grade;
		}

		public void setGrade(String grade) {
			this.grade = grade;
		}

		public String getUrl_charset() {
			return url_charset;
		}

		public void setUrl_charset(String urlCharset) {
			url_charset = urlCharset;
		}

		public String getContent_charset() {
			return content_charset;
		}

		public void setContent_charset(String contentCharset) {
			content_charset = contentCharset;
		}

		public String getParserImpl() {
			return parserImpl;
		}

		public void setParserImpl(String parserImpl) {
			this.parserImpl = parserImpl;
		}

		public String getParserStandartNameImpl() {
			return parserStandartNameImpl;
		}

		public void setParserStandartNameImpl(String parserStandartNameImpl) {
			this.parserStandartNameImpl = parserStandartNameImpl;
		}
		
		public String getMparas() {
			return mparas;
		}

		public void setMparas(String mparas) {
			this.mparas = mparas;
		}
		
		public String getIsUse() {
			return isUse;
		}

		public void setIsUse(String isUse) {
			this.isUse = isUse;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Map map = XMNutchTemplateSettor.templateSettor;
		if (map.size() == 0) {
			XMNutchTemplateSettor xm = new XMNutchTemplateSettor();
			xm.loadNutchTemplateXmlFile();
		}
		Iterator it = map.keySet().iterator();
		String key = "";
		Ant value = null;
		while (it.hasNext()) {
			key = (String) it.next();
			value = (Ant) map.get(key);
			System.out.println(key + "==" + value.name);
		}
		System.out.println(map.size());

		// Ant ant = (Ant) map.get("book.dangdang.com");
		/*
		 * System.out.println(ant.getMparas());
		 */
		// System.out.println(to_requestUrls.get("0001").getReq_urls().get(1));

	}

}
