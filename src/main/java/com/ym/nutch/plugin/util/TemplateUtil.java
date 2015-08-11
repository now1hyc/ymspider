package com.ym.nutch.plugin.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ym.nutch.parse.template.TemplateParser;


/**
 * 模板解析中，常用方法放于此
 * */
public class TemplateUtil {
	private static Logger LOG = LoggerFactory.getLogger(TemplateUtil.class);


	/**
	 * 获取类实例
	 * @param className
	 * @return 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static TemplateParser getInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className);
		return (TemplateParser) clazz.newInstance();

	}
	
	/**
	 * 获取url的baseUrl
	 * @param url
	 * @return
	 */
	protected static String getBaseUrl(String url) {
		url = url.substring(url.indexOf("//") + 2);
		if (url.contains("/")) {
			url = "http://" + url.substring(0, url.lastIndexOf("/")) + "/";
		} else {
			url = "http://" + url + "/";
		}
		return url;
	}
	

	/**
	 * 过滤开头和结尾的空格
	 * @param str
	 * @return
	 */
	public static String filterSpace(String str) {

		if (str == null || str.equals("")) {
			return str;
		} else {
			// return leftTrim(rightTrim(str));
			return str.replaceAll("^[　 ]+|[　 ]+$", "");
		}
	}

	
	/**
	 * 获取符合路径的节点列表
	 * @param doc
	 * @param Xpath
	 * @return NodeList
	 */
	public static NodeList getNodeList(DocumentFragment doc, String Xpath) {
		NodeList nodeList = null;
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new XMNamespaceContext("http://www.w3.org/1999/xhtml"));
		XPathExpression expr;
		try {
			expr = xpath.compile(Xpath);
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// XXX Auto-generated catch block
			e.printStackTrace();
		}
		return nodeList;
	}

	
	/**
	 * 获取符合路径要求的节点
	 * @param doc
	 * @param Xpath
	 * @return
	 */
	public static Node getNode(DocumentFragment doc, String Xpath) {
		Node node = null;
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new XMNamespaceContext("http://www.w3.org/1999/xhtml"));
		XPathExpression expr = null;
		try {
			expr = xpath.compile(Xpath);
			node = (Node) expr.evaluate(doc, XPathConstants.NODE);
		} catch (Exception e) {
			LOG.error("@!getNode+Xpath" + Xpath, e);
		} 
		return node;
	}
	
	
	/**
	 * 自动补全对于不是http开头的url，自动补全
	 * @param parentUrl
	 * @param tmpUrl
	 * @return
	 */
	public static String autoCompleteUrl(String parentUrl, String tmpUrl) {

		if (!tmpUrl.startsWith("http")) {
			URL u = null;
			try {
				u = new URL(parentUrl);
			} catch (MalformedURLException e) {
				// XXX Auto-generated catch block
				e.printStackTrace();
			}
			String startUrl = getBaseUrl(parentUrl);
			String host = u.getHost();

			if (tmpUrl.startsWith("/") && tmpUrl.length() > 1) {
				tmpUrl = "http://" + host + tmpUrl;
			} else if (tmpUrl.length() > 1) {
				tmpUrl = startUrl + tmpUrl;
			} else {
				tmpUrl = null;
			}
		}
		return tmpUrl;
	}

	
	
	/**
	 * 该方法还有待完善
	 * 
	 * @param doc
	 * @param xpath
	 * @return
	 */
	public static String getMap (DocumentFragment doc,String xpath){
		//TODO 如何去除空节点的影响。
		
		Map map = new LinkedHashMap();
		Node node_path = TemplateUtil.getNode(doc,xpath);
		NodeList trList = node_path.getChildNodes();
		
		for (int j = 0; j < trList.getLength(); j++) {
			Node tr = trList.item(j);
			NodeList tdNodes = tr.getChildNodes();
			
			if (tdNodes.getLength() %2 == 0) {
				for (int k = 0; k < tdNodes.getLength(); k = k + 2) {
					Node key = tdNodes.item(k);
					Node value = tdNodes.item(k + 1);
					if (!"".equals(key.getTextContent())) {
						map.put(key.getTextContent(), value.getTextContent());
					}
				}
			} else if (tdNodes.getLength() %2 == 1) {
				String key = tdNodes.item(0).getTextContent();
				map.put(key, Constant.XMTAG);
			}
		}
		
		String value="";
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			value = (String) map.get(key);
			value += key + Constant.XMTAG + value;
		}
		return value;
	  }


	/**
	 * 获取一个参数规格表
	 * @param doc
	 * @param mparasXpath
	 * @return 
	 */
	public static String getMParas(DocumentFragment doc, String mparasXpath) {
		String key = "";
		Node node;
		NodeList nodeList;
		nodeList = TemplateUtil.getNodeList(doc, mparasXpath);
		String mparams = "";
		for (int i = 0; i < nodeList.getLength(); i++) {
			node = nodeList.item(i);
			if (node.getNodeName().toLowerCase().equals("script")
					| node.getNodeName().toLowerCase().equals("style") | node.getNodeType()==Node.COMMENT_NODE) {
				continue;
			}
			if (node.hasChildNodes()) {
				NodeList childNodeList = node.getChildNodes();
				if (i > 0) {
					mparams += Constant.XMTAG;
				}
				List<String> list = getNodeList(childNodeList);
				StringBuffer values = new StringBuffer();
				if(list.size()>0){
					if(list.size()==1){
						key = list.get(0).replaceFirst("：|:", "");
						values.append("null");
					}else{
						key = list.get(0).replaceFirst("：|:", "");
						for(int k=1;k<list.size();k++){
							values.append(list.get(k).replaceFirst("：|:", ""));
						}
					}
				}
				mparams += " ";
				mparams += key;
				mparams += Constant.XMTAG;
				mparams += values.toString();
			} else {
				mparams += node.getTextContent().replaceAll("[ \n|\r|\t|\\s]+"," ");
				mparams += Constant.XMTAG;
			}
		}
		return  mparams;
	}

	private static List<String> getNodeList(NodeList childNodeList){
		List<String> nodeList = new ArrayList<String>();
		String str = "";
		for (int j = 0; j < childNodeList.getLength(); j++) {
			Node node = childNodeList.item(j);
			if (node != null && !node.getNodeName().toLowerCase().equals("script")
					&& !node.getNodeName().toLowerCase().equals("style") 
					&& node.getNodeType()!=Node.COMMENT_NODE) {		
				str = node.getTextContent().replaceAll("[　\\s]+"," ");
				str = str.replaceAll("：", ":");
				if (str != null && !isBlankString(str) ) {
					nodeList.add(str);
				}
			} else {
				continue;
			}
		}
		return nodeList;
	}

	public static boolean isBlankString(String str) {
		return str.isEmpty() | str.matches("[ \\s　]+");
	}

	
	/**
	 * unicodeToString while reverse the chinese's Unicode to chinese
	 * but this method use the pattern to do, it will cost many time;
	 * @param str
	 * @return
	 */
	public static String unicodeToString(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			ch = (char) Integer.parseInt(matcher.group(2), 16);
			str = str.replace(matcher.group(1), ch + "");
		}
		return str;
	}
	
	
	/**
	 * unicodeToString while reverse the chinese's Unicode to chinese
	 * use StringBuilder and String[] to do;
	 * @param str
	 * @return
	 */
	public static StringBuilder unicode2String(String str){
		String[] strings = str.split("\\\\u|\n");
		StringBuilder sBu=new StringBuilder();
		int i =0;
		while (i <strings.length) {
			if(strings[i].isEmpty()|strings[i].matches("\\W+")){
				sBu.append(strings[i]);
				i++;continue;	
			}
			if(strings[i].length()==4 && Pattern.matches("\\p{XDigit}{4}.*", strings[i])){
				sBu.append((char) Integer.parseInt(strings[i],16));
				i++;
			}else if(strings[i].length()>4 && Pattern.matches("\\p{XDigit}{4}.*", strings[i])){
				sBu.append((char) Integer.parseInt(strings[i].substring(0, 4),16)+strings[i].substring(4));
				i++;
			}else {
				sBu.append(strings[i]);
				i++;continue;	
			}
		}
		return sBu;
	}
	
	public static String unicode3String(String str){
		String[] strings = str.split("\\\\u|\n");
		String sBu="";
		int i =0;
		while (i <strings.length) {
			if(strings[i].length()==4){
				sBu +=((char) Integer.parseInt(strings[i],16));
				i++;
			}else if(strings[i].length()>4){
				sBu +=((char) Integer.parseInt(strings[i].substring(0, 4),16)+strings[i].substring(4));
				i++;
			}else {
				sBu +=(strings[i]);
				i++;continue;	
			}
		}
		return sBu.toString();
	}
	
	public static String unicode4String(String str) {
		String[] strings = str.split("\\\\u|\n");
		StringBuffer sBu = new StringBuffer();
		int i =0;
		while (i <strings.length) {
			if(strings[i].length()==4){
				sBu.append((char) Integer.parseInt(strings[i],16));
				i++;
			}else if(strings[i].length()>4){
				sBu.append((char) Integer.parseInt(strings[i].substring(0, 4),16)+strings[i].substring(4));
				i++;
			}else {
				sBu.append(strings[i]);
				i++;continue;	
			}
		}
		return sBu.toString();
	}
	
	
	/**
	 * 逆向匹配对应关系, [xm99]分割符
	 * @param mparams
	 * @return
	 */
	public static String formatMparams(String mparams) {
		String[] strings= mparams.split("\\s*\\[xm99\\]\\s*");
		StringBuilder mString = new StringBuilder();
		if(strings.length==1){
			return strings[0];
		}
		for (int i = strings.length-1; i >= 0; i--) {
			if(i==0&&!strings[i].isEmpty()){
				mString.insert(0, strings[i] + "=" + "null");
				continue;
			}
			if(strings[i].isEmpty()&i>0){
				continue;
			}
			
			if (!strings[i - 1].isEmpty()&i>0) {
				if((i-1)==0)
					mString.insert(0,strings[i - 1] + "=" + strings[i]);
				else {
					mString.insert(0, Constant.XMTAG+strings[i - 1] + "=" + strings[i]);
				}
			} else if(i-1!=0){
				mString.insert(0, Constant.XMTAG+strings[i] + "=" + "null");
			}else{
				mString.insert(0, strings[i-1] + "=" + "null");
			}
			i--;
		}
		return mString.toString();
	}


	/**
	 * 将冒号替换为“=”
	 * @param mparams
	 * @return
	 */
	public static StringBuilder formatMparamsColon(String mparams) {
		String[] strings=mparams.split("\\s*\\[xm99\\]\\s*");
		StringBuilder mString = new StringBuilder();
		for(int i=0;i<strings.length;i++){
			if(strings[i].isEmpty()){mString.append(Constant.XMTAG);continue;}
			if(strings[i].matches(".*[^\\d]+[：:]+.*")){
				strings[i]=strings[i].replaceFirst("[：:]+", "=");
				mString.append(strings[i]).append(Constant.XMTAG);
			}else{
				mString.append(strings[i]).append(Constant.XMTAG);
			}
		}
		return mString;
	}
	
	public static String formatBookMparams(String mStr){
		String rs ="";
		String[] strings=mStr.toString().split("\\s*\\[xm99\\]\\s*");
		for(int i=0;i<strings.length;i++){
//			是否为空
			if(strings[i].isEmpty()){
//				结果内容长度>6 && 结果内容的结尾不为[xm99],在结果末尾加上[xm99];否则反之;
				if(rs.length()>6 && !rs.substring(rs.length()-6, rs.length()).toString().equals("[xm99]")){
					rs+=(Constant.XMTAG );
				}
				continue;
			}
//			不为空，是否到末尾，到了，结果直接加上末尾内容；
			if(i==strings.length-1){
				if(!strings[i].contains("=")||rs.isEmpty()||(rs.length()>6 && rs.substring(rs.length()-6, rs.length()).toString().equals("[xm99]")))
					rs += strings[i];
				else if(rs.length()>6 && !rs.substring(rs.length()-6, rs.length()).toString().equals("[xm99]")){
					rs += Constant.XMTAG + strings[i];
				}
//			}
//			不是末尾，同时i的内容是“=”结尾 && i+1在数组编号范围内&&  i+1的内容中不包含‘=’ && i+1是有内容的
//			else if(strings[i].endsWith("=") && i+1<strings.length && !strings[i+1].contains("=") && strings[i+1].length()>0){
////				结果内容长度>6 && 结果内容的结尾是[xm99],直接在结果内容末尾加上 i和i+1的内容
//				if(rs.length()>6 && rs.substring(rs.length()-6, rs.length()).toString().equals("[xm99]")){
//					rs+=(strings[i] + strings[i + 1]);
////				不是[xm99]结尾 && i>0，直接加上 [xm99]+ i和i+1的内容
//				}else if(i>0){
//					rs+=(Constant.XMTAG+strings[i]+strings[i+1]);
////				i=0，直接开始+
//				}else{
//					rs+=(strings[i] + strings[i + 1]);
//				}
//				i++;
////			不是末尾，同时i的内容是“=”结尾 
			}else if(strings[i].endsWith("=")){
				if(strings[i+1].isEmpty()){
					if(rs.isEmpty()||(rs.length()>6 && "[xm99]".equals(rs.substring(rs.length()-6, rs.length()))))
						rs += strings[i] + "null";
					else {
						rs += Constant.XMTAG + strings[i] + "null";
					}
				}else if (strings[i+1].contains("=")&&(rs.isEmpty()||rs.length()>6 && "[xm99]".equals(rs.substring(rs.length()-6, rs.length())))){
					rs += strings[i] + "null";
				}else{
					if(rs.isEmpty()||(rs.length()>6 && "[xm99]".equals(rs.substring(rs.length()-6, rs.length())))){
						rs += strings[i] + strings[i + 1];
						i++;
					 }else
						rs += Constant.XMTAG + strings[i]+"null";
				}
				
//			不是末尾，同时i的内容含"=" 且不是“=”结尾 	
			}else if(strings[i].contains("=")){
				if(rs.isEmpty()||(rs.length()>6 && rs.substring(rs.length()-6, rs.length()).toString().equals("[xm99]"))){
					rs += strings[i];
				}else{
					rs+=Constant.XMTAG + strings[i];
				}
//			i的内容不含“=”， 	
			}else{
//				if(rs.isEmpty()||(rs.length()>6 && "[xm99]".equals(rs.substring(rs.length()-6, rs.length()))))
					rs += " " + strings[i];
//				else {
//					rs +=Constant.XMTAG+strings[i];
//				}
			}
		}
		return rs;
		
	}
	
	
	/**
	 * @param sb
	 * @param node
	 * @param abortOnNestedAnchors "true" get the anchoes,otherwise "false"
	 * @param anchorDepth 
	 * @return
	 */
	
	public static boolean getTextHelper(StringBuffer sb, Node node) {
		boolean abort = false;
		NodeWalker walker = new NodeWalker(node);

		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			if ("script".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if ("style".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if (nodeType == Node.COMMENT_NODE) {
				walker.skipChildren();
			}
			if (nodeType == Node.TEXT_NODE) {
				// cleanup and trim the value
				String text = currentNode.getNodeValue();
				text = text.replaceAll("\\s+", " ");
				text = text.trim();
				if (text.length() > 0) {
					if (sb.length() > 0)
						sb.append('\n');
					sb.append(text);
				}
			}
		}
		return abort;
	}
	
	public static List<Node> getTextHelper(Node node,String chooseNodeName) {
		
		List<Node> list = new LinkedList<Node>();
		
		NodeWalker walker = new NodeWalker(node);

		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			if ("script".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if ("style".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if (chooseNodeName.equalsIgnoreCase(nodeName)) {
				list.add(currentNode);
				walker.skipChildren();
			}
			if (nodeType == Node.COMMENT_NODE) {
				walker.skipChildren();
			}
			if (nodeType == Node.TEXT_NODE) {
			}
		}
		return list;
	}
	
	
	/**
	 * 
	 * @param String
	 * @return
	 */
	private static class XMNamespaceContext implements NamespaceContext {
		private String ns;
		
		public XMNamespaceContext(String ns){
			this.ns = ns;
		}
		@Override
		public String getNamespaceURI(String prefix) {
			if(prefix == null){
				throw new NullPointerException("Null prefix");
			}else if(prefix.equals("pre")){
				return this.ns;
			}else if(prefix.equals("xml")){
				return XMLConstants.XML_NS_URI;
			}
			return XMLConstants.XML_NS_URI;
		}

		@Override
		public String getPrefix(String namespaceURI) {
			return null;
		}

		@Override
		public Iterator<?> getPrefixes(String namespaceURI) {
			return null;
		}
	}

	
	//为转化成json，将String中的js代码删除，(传入的String 保持换行，以此来判断）
	public static String filterJson(String jsonString,String... deleltKey){
		StringBuilder temp = new StringBuilder(jsonString);
		for (int i = 0; i < deleltKey.length; i++) {
			int beginIndex = temp.indexOf(deleltKey[i]);
			int endIndex = temp.indexOf("\n", beginIndex);
			temp.delete(beginIndex, endIndex);
		}
		
		return temp.toString();
	}
	

	// 将url连接中的参数字段，转换成map，可以直接以键获取值
	public Map<String, String> formMap(String[] str, String flag) {
		Map<String, String> pmap = new HashMap<String, String>();
		for (String tmp : str) {
			String[] kv = tmp.split(flag);
			if (kv.length == 2) {
				pmap.put(kv[0], kv[1]);
			}
		}
		return pmap;
	}
}
