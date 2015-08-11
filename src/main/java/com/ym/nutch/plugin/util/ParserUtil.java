package com.ym.nutch.plugin.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParserUtil {

	//String encoding = "utf-8";
	String encoding = "gbk";
	
	public DocumentFragment getRoot(byte[] contentInOctets){
		DocumentFragment root = null;
		try {
			InputSource input = new InputSource(new ByteArrayInputStream(contentInOctets));
			input.setEncoding(encoding);
			root = parse(input);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return root;
	}
	
	public DocumentFragment getRoot(File file){
		DocumentFragment root;
		try {
			FileInputStream stream = new FileInputStream(file);
			InputSource input = new InputSource(stream);
			input.setEncoding(encoding);
			return root = parse(input);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public DocumentFragment getRoot(File file,String ecoding){
		this.encoding = ecoding;
		DocumentFragment root;
		try {
			FileInputStream stream = new FileInputStream(file);
			InputSource input = new InputSource(stream);
			input.setEncoding(encoding);
			return root = parse(input);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private DocumentFragment parse(InputSource input) throws Exception {
		return parseNeko(input);
	}
	
	private DocumentFragment parseNeko(InputSource input) throws Exception {

		DOMFragmentParser parser = new DOMFragmentParser();
		try {
			parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", encoding);
			parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
		} catch (SAXException e) {
			
		}
		// convert Document to DocumentFragment
		HTMLDocumentImpl doc = new HTMLDocumentImpl();
		doc.setErrorChecking(false);
		DocumentFragment res = doc.createDocumentFragment();
		DocumentFragment frag = doc.createDocumentFragment();
		parser.parse(input, frag);
		res.appendChild(frag);

		try {
			while (true) {
				frag = doc.createDocumentFragment();
				parser.parse(input, frag);
				if (!frag.hasChildNodes())
					break;
			
				res.appendChild(frag);
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return res;
	}
	
	/**
	 * 读取本地文件，生成字符串
	 */
	public static String read(File file){
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader br = IOUtil.getBufferedReader(file);
			String temp = "";
			while((temp=br.readLine())!=null){
				sb.append(temp.trim()+"\n");
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
		
	}
	
	/**
	 * 根据Node节点生成List
	 * @param tableNode
	 * @return
	 */
	public static List<Map<String,String>> parseTable(Node tableNode){
		
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();
		NodeList trNodeList = tableNode.getChildNodes();
		
		for(int i=0;i<trNodeList.getLength();i++){
			Node trNode = trNodeList.item(i);
			if("TR".equals(trNode.getNodeName())){
				NodeList tdNodeList = trNode.getChildNodes();
				if(tdNodeList.getLength()==1){
					Map<String,String> map = new HashMap<String,String>();
					Node tdNode = tdNodeList.item(0);
					map.put(tdNode.getTextContent(),"null");
					list.add(map);
				}else if(tdNodeList.getLength()%2==0){
					for(int j=0;j<tdNodeList.getLength();j=j+2){
						Map<String,String> map = new HashMap<String,String>();
						Node tdNode1 = tdNodeList.item(j);
						Node tdNode2 = tdNodeList.item(j+1);
						if("TD".equals(tdNode1.getNodeName()) || "TH".equals(tdNode1.getNodeName()) ){
							map.put(tdNode1.getTextContent(),tdNode2.getTextContent());
						}
						list.add(map);
					}
				}
			}
		}
		
		return list;
	}
	public static List<Map<String,String>> parseMamabbTable(Node tableNode){
		
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();
		NodeList nodeList = tableNode.getFirstChild().getChildNodes();
		if(list!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node = nodeList.item(i);
				if(node.getNodeName().equalsIgnoreCase("#text")){
					Map<String,String> map = new HashMap<String,String>();
					String[] values = node.getTextContent().split(":|：");
					if(values.length==2){
						map.put(values[0],values[1]);	
					}else{
						map.put(values[0],"null");
					}
					
					list.add(map);
				}
			}
		}
		return list;
	}
	/**
	 * 根据table的html代码解析成List
	 * @param tableHtml
	 */
	public static List<Map<String,String>> parseTable(String tableHtml){
		
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();
		
		/**
		 * 处理tableHtml数据源，将数据源都小写，同时去除tbody，将th转换成td
		 */
		tableHtml = tableHtml.toLowerCase().replaceAll("th","td").replaceAll("tbody","");
		
		PatternUtil util = new PatternUtil("<tr.*?>","</tr>",".*?",tableHtml);
				
		List<String> trList = util.getList();
		
		for(String tr:trList){
			
			PatternUtil utilTd = new PatternUtil("<td.*?>","</td>",".*?",tr);
			List<String> tdList = utilTd.getList();	
			
			if(tdList.size()==1){
				String tdKey = tdList.get(0).trim().replaceAll("<.*?>","");
				if(!"".equals(tdKey)){
					Map<String,String> map = new HashMap<String,String>();
					map.put(tdKey,"null");
					list.add(map);
				}
				
			}else if(tdList.size()%2==0){
				for(int i=0;i<tdList.size();i=i+2){
					String tdKey = tdList.get(i).trim().replaceAll("<.*?>","");
					String tdValue = tdList.get(i+1).trim().replaceAll("<.*?>","");
					if(!"".equals(tdKey) && !"".equals(tdValue)){
						Map<String,String> map = new HashMap<String,String>();
						map.put(tdKey,tdValue);
						list.add(map);	
					}
				}
			}
		}
		return list;
	}
	
	public static List<Map<String,String>> parseYihaodianTable(Node tableNode){
	
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();
		
		NodeList nodeList = tableNode.getChildNodes();

		for(int table=0;table<nodeList.getLength();table++){
			Node nodeTop = nodeList.item(table);
			if("dl".equalsIgnoreCase(nodeTop.getNodeName())){
				
				NodeList trNodeList = nodeTop.getChildNodes();
				
				for(int i=0;i<trNodeList.getLength();i++){
					Node node = trNodeList.item(i);
					if("dd".equalsIgnoreCase(node.getNodeName())){
						Map<String,String> map = new HashMap<String,String>();
						NodeList keyValueNode = node.getChildNodes();
						String result = "";
						for(int kv =0;kv<keyValueNode.getLength();kv++){
							Node keyValue = keyValueNode.item(kv);
							String temp = converterString(keyValue.getTextContent().replaceAll("\n",""));
							if(!temp.equals("")){
								result = result + temp+"[xm]";	
							}
							
						}
						String[] resultArr = result.split("\\[xm\\]");
						if(resultArr.length>=2){
							map.put(resultArr[0],resultArr[1]);
							list.add(map);
						}
					}
				}
				
			}
		}
		return list;
	}
	public static List<Map<String,String>> parseHuaqiangbeiTable(String tableNode){
		//<DIV class='prod_d_gray_info_bar'><UL><LI>单反品牌：Canon/佳能</LI><LI>型号：EOS 550D套机(含18-55IS镜头)</LI><LI>上市时间：2010年</LI><LI>单反屏幕尺寸：3寸及以上</LI><LI>防抖性能：机身防抖</LI><LI>像素：1600万及以上</LI><LI>储存介质：SD卡</LI><LI>电池类型：专用锂电</LI><LI>颜色分类：18-55 18-135镜头</LI><LI>机身马达：支持</LI><LI>机身除尘：支持</LI><LI>套餐：官方标配</LI><LI>单反级别：中级</LI><LI>单反价格区间：3001-6000元</LI></UL><DIV class='cb'></DIV></DIV>
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();

		PatternUtil liListUtil = new PatternUtil("<li>","</li>",".*?",tableNode);
		List<String> liList = liListUtil.getList();
		String[] resultArr = null;
		for(String li:liList){
			Map<String,String> map = new HashMap<String,String>();
			resultArr = li.split(":");
			if(resultArr.length>=2){
				map.put(resultArr[0],resultArr[1]);
				list.add(map);
			}
		}
		return list;
	}
	public static List<Map<String,String>> parseUlliTable(String tableNode,String splitFlag){

		List<Map<String,String>> list = new LinkedList<Map<String,String>>();

		PatternUtil liListUtil = new PatternUtil("<li.*?>","</li>",".*?",tableNode);
		List<String> liList = liListUtil.getList();
		String[] resultArr = null;
		for(String li:liList){
			Map<String,String> map = new HashMap<String,String>();
			if(splitFlag!=null){
				li = li.replaceAll("<[^>]*>","");//去除所有包含的html标签
				resultArr = li.split(splitFlag);
				if(resultArr.length>=2){
					map.put(resultArr[0],resultArr[1]);
					list.add(map);
				}
			}
		}
		return list;
	}
/**
 * 解析table节点。生成字符串直接存入数据库
 * 目前支持两种方式：
 * 1、传入table的Node节点
 * 2、传入table对应的Html页面，flag参数为html
 * 3、flag参数为yihaodianTable表示为一号店。
 *    格式为:<dd><label>key </label>value</dd>
 * @param tableNode
 * @param flag
 */
	public static String parseTableToString(Node tableNode,String... flag){
		
		List<Map<String,String>> list = null;
		if(flag==null || flag.length==0){
			list = parseTable(tableNode);	
		}else if(flag[0].equals("html")){//解析Table的Html
			String tableHtml = nodeToString(tableNode);
			tableHtml = tableHtml.replaceAll("\n","");
			list = parseTable(tableHtml);
		}else if(flag[0].equals("yihaodianTable")){//比较特殊，只是适合一号店，dl标签解析
			list = parseYihaodianTable(tableNode);
		}else if(flag[0].equals("huaqiangbei")){//ul li 解析
			String tableHtml = nodeToString(tableNode);
			tableHtml = tableHtml.replaceAll("[\\s]{3,}","").trim().toLowerCase();
			tableHtml = tableHtml.replaceAll("：",":");
			list = parseHuaqiangbeiTable(tableHtml);
		}else if(flag[0].equals("zouxiu")){//走秀 解析ul li
			String tableHtml = nodeToString(tableNode);
			tableHtml = tableHtml.replaceAll("[\\s]{3,}","").trim().toLowerCase();
			tableHtml = tableHtml.replaceAll("：",":");
			list = parseUlliTable(tableHtml,flag[1]);
		}else if(flag[0].equals("mamabb")){//比较特殊，只是针对mamabb，解析<br>标签
			list = parseMamabbTable(tableNode);	
		}
		
		StringBuffer sb = new StringBuffer();
		
		for(Map<String,String> map:list){
			for(Iterator<String> it = map.keySet().iterator();it.hasNext();){
				String key = it.next();
				String value = map.get(key);
				sb.append(key+"="+value).append(Constant.XMTAG);
			}
		}
		if(flag!=null && flag.length>0){
			if(flag[0].equals("yihaodianTable")){
				String result = sb.toString();
				result = result.replaceAll("\\[xm99\\] =",";");
				return result;
			}
		}
		return sb.toString();
	}

	public static String converterString(String str){
		if(str==null || str.equals(" ") || str.equals("&nbsp;")){
			return "";
		}else{
			return str.trim();
		}
	}
	/**
	 * 属性过滤器，用于nodeToString方法，通过给定的Node，返回该Node下的html代码
	 */
	static List<String> attributeFilterList = new ArrayList<String>();
	static{
		attributeFilterList.add("style");
		attributeFilterList.add("face");
		attributeFilterList.add("border");
		attributeFilterList.add("cellspacing");
		attributeFilterList.add("cellpadding");
		attributeFilterList.add("width");
		attributeFilterList.add("heigh");
	}
	
	/**
	 * 给定Node返回该Node中HTML代码。
	 * 注意：HTML代码是网站原有格式，使用时，需要进行必要的转换，例如去除空格或者回车，一般去除回车即可，因为属性中可能包含空格，不应该去除属性中的空格
	 * Node的attribute属性可以配置过滤器进行过滤
	 * @param node
	 * @param filter 包含在数组中的标签是需要生成的。
	 * @return
	 */
	public static String nodeToString(Node node,String... filter){
		List<String> filterList = Arrays.asList(filter);
		StringBuffer str = new StringBuffer();
		if(node.getNodeName().equalsIgnoreCase("#text")){
			return node.getTextContent();
		}else{
			str.append("<"+node.getNodeName());
			//添加属性
			NamedNodeMap attrbutes = node.getAttributes();
			if(attrbutes!=null){
				for(int i=0;i<attrbutes.getLength();i++){
					Node n = attrbutes.item(i);
					//标签在filter中是需要生成的。attributeList中是不需要生成的
					if(filterList.contains(n.getNodeName().toLowerCase()) ||  !attributeFilterList.contains(n.getNodeName().toLowerCase()) ){
						str.append(" ").append(n.getNodeName()).append("='").append(n.getNodeValue()).append("'");
					}
				}
			}
			str.append(">");
			//添加子节点
			NodeList nodeList = node.getChildNodes();
			if(nodeList!=null && nodeList.getLength()>0){
				for(int i=0;i<nodeList.getLength();i++){
					Node n = nodeList.item(i);
					str.append(nodeToString(n));
				}
			}
			str.append("</"+node.getNodeName()+">");
		}
		return str.toString();
	}
	
	
}
