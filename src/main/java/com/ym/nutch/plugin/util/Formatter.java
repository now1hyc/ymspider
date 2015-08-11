package com.ym.nutch.plugin.util;

import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONObject;

public class Formatter {

	private static final Log logger = LogFactory.getLog(Formatter.class);
	
	public static String align(String str, int len, char fill, boolean leftAlign) {

		if (str == null) {
			str = "";
		}
		byte[] buf = new byte[len];
		byte[] bstr = str.getBytes();
		int str_len = bstr.length;
		int fill_len = len - str_len;
		if (leftAlign) {
			if (fill_len > 0)
				System.arraycopy(bstr, 0, buf, 0, str_len);
			else {
				System.arraycopy(bstr, 0, buf, 0, len);
			}
		} else if (fill_len > 0)
			System.arraycopy(bstr, 0, buf, fill_len, str_len);
		else {
			System.arraycopy(bstr, -fill_len, buf, 0, len);
		}

		for (int i = 0; i < fill_len; ++i) {
			if (leftAlign)
				buf[(i + str_len)] = (byte) fill;
			else {
				buf[i] = (byte) fill;
			}
		}
		return new String(buf);
	}

	public static String trim(String str) {

		return ((str == null) ? "" : str.trim());
	}

	public static String ralign(String str, int len) {

		return ralign(str, len, '0');
	}

	public static String ralign(String str, int len, char fill) {

		return align(str, len, fill, false);
	}
	/**
	 * 翻转url，将http://www.360buy.com/product/123.html  反转为 http://com.360buy.www/product/123.html
	 * @param url
	 * @return
	 */
	public static String reverseUrl(String url){
		String host = "";
		String uri = "";
		String protocal = "";
		int index = url.indexOf("://");
		if(index>=0){
			protocal = url.substring(0,index+3);
			url = url.substring(index+3);
		}
		index = url.indexOf("/");
		if(index==-1){
			 host = url;
		}else{
			 host = url.substring(0,index);
			 uri = url.substring(index);
		}

		String[] urlFanArr = host.split("\\.");
		StringBuffer sb = new StringBuffer(protocal);
		for(int i=urlFanArr.length-1;i>=0;i--){
			sb.append(urlFanArr[i]);
			if(i!=0){
				sb.append(".");
			}
		}
		sb.append(uri);
		return sb.toString();
	}
	/**
	 * 从字符串中解析json字符串
	 */
public static String getJson(String message){
		
		Stack<String> stack = new Stack<String>();
		int a1 = message.indexOf("{");
		int b1 = message.indexOf("}");
		int a = message.indexOf("{");
		int c = a1;
		stack.add("{");
		if(a1 < 0 || b1 < 0){
			logger.debug("Json解析异常");
		}else{
			while(stack.size()>0){
				a1 = message.indexOf("{",c+1);
				b1 = message.indexOf("}",c+1);
				if(a1==-1){
					if(stack.size()>=1){
						for(int i=1;i<stack.size();i++){
							b1 = message.indexOf("}",b1+1);
						}
					}
					return message.substring(a,b1+1);
				}
				if(a1>b1){//} {
					c = b1;
					if(stack.size()==0){
						break;
					}else{
						if(stack.peek().equals("{")){
							stack.pop();
						}else{
							stack.add("}");
						}
					}
					continue;
				}else if(a1<b1){//{ }
					c = a1;
					if(stack.size()==0){
						break;
					}else{
						if(stack.peek().equals("}")){
							stack.pop();
						}else{
							stack.add("{");
						}
					}
					continue;
				}
			}
		}
		return message.substring(a,c+1);
	}

	public static JSONObject getJson(DocumentFragment root,String jsonXpath,String jsonName,String jsonKey,String jsonNamevalue){
		String jsonValue = "";
		String value = "";
		int index1 = 0;
		int index2 = 0;
		Node jsonNode = null; 
		NodeList nodeList = TemplateUtil.getNodeList(root,jsonXpath); 
		if(nodeList.getLength()>0){
			for(int i=0;i<nodeList.getLength();i++){
				Node node = nodeList.item(i);
				value = node.getTextContent().replaceAll("[\\s]*","");
				index1 = value.indexOf(jsonName);
				index2 = value.indexOf(jsonKey);
				if(index1>=0 && index2>=0){
					jsonNode = node;
					break;
				}
			}
		}
		
		if(jsonNode==null){
			return null;
		}else{
			jsonValue = jsonNode.getTextContent().replaceAll("[\\s]{3,}"," ");
			index1 = jsonValue.indexOf(jsonNamevalue);
			jsonValue = jsonValue.substring(index1);//得到json字符串
			jsonValue = Formatter.getJson(jsonValue);
		}
		if(!jsonValue.equals("")){
			JSONObject productObject = JSONObject.parseObject(jsonValue);
			return productObject;
		}else{
			return null;
		}
	}
}
