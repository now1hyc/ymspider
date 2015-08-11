package com.ym.framework.util;

import org.dom4j.Element;

public class XmlUtil {

	
	
	/**
	 * 检查xml 节点元素
	 * @param Element
	 * @return String
	 */
	public static String getElement(Element element,String type){
		
		   if(element==null){
			   if("string".equals(type.toLowerCase())){
				   return ""; 
			   }else if("float".equals(type.toLowerCase())){
				   return "0.0";
			   }else if("int".equals(type.toLowerCase())){
				   return "0";
			   }
			   
		   }else{
			   return element.getTextTrim();
		   }
		   return "";
	}
	

	/**
	 * 检查xml 节点元素
	 * @param Element
	 * @return String
	 */
	public static String getElement(Element element,String type,String initVal){
		
		   if(element==null){
			   
			   if(initVal!=null&&!"".equals(initVal)){
				   return initVal;
			   }
			   if("string".equals(type.toLowerCase())){
				   return ""; 
			   }else if("float".equals(type.toLowerCase())){
				   return "0.0";
			   }else if("int".equals(type.toLowerCase())){
				   return "0";
			   }
			   
		   }else{
			   String tmpText = element.getTextTrim();
			   if("".equals(tmpText)){
				   if(initVal!=null&&!"".equals(initVal)){
					   return initVal;
				   }
			   }else{
				   return tmpText;
			   }
		   }
		   return "";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
