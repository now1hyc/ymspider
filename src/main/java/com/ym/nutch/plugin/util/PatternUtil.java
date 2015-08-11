package com.ym.nutch.plugin.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PatternUtil {

	public final static String TEXTTEGEX = "(.*?)"; //匹配任意值

	public final static String W = "\\W*?";

	public final static String N = "";

	public final static String TEXTEGEXANDNRT = "[\\s\\S]*?";
	
	private  String beginRegex;
	
	private  String endRegex;
	
	private Matcher matcher;
	
	public PatternUtil(){
		
	}
	/**
	 * 
	 * @param beginRegex 开始正则
	 * @param endRegex  结束正则
	 * @param textRegex  中间正则内容
	 * @param message 需要匹配的正则内容
	 */
	public PatternUtil(String beginRegex,String endRegex,String textRegex,String message){
		this.beginRegex = beginRegex.toLowerCase();
		this.endRegex = endRegex.toLowerCase();
		String regexStr = beginRegex+textRegex+endRegex;
		Pattern pattern = Pattern.compile(regexStr,Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(message);
	}

	/**
	 * @param textRegex  正则内容
	 * @param message 需要匹配的正则内容
	 */
	public PatternUtil(String textRegex,String message){
		this.beginRegex = "";
		this.endRegex = "";
		Pattern pattern = Pattern.compile(textRegex,Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(message);
	}

	public Matcher getMatcher(){
		return matcher;
	}
	/**
	 * 仅仅获得一次匹配的内容,默认没有分组，就是0个分组
	 */
	public String getText() {
		return getText(0);
	}
	/**
	 * 仅仅获得一次匹配的内容，具体到某个匹配的分组
	 * site表示第几个分组
	 */
	public String getText(int site) {
		if (matcher.find()) {
			return matcher.group(site).trim().replaceFirst(beginRegex, N).replaceAll(endRegex, N).replaceAll("\n"," ");
		}
		return "";
	}
	/**
	 * 循环获得匹配的内容，最终得到一个list集合
	 */
	public List<String> getList() {
		List<String> list = new LinkedList<String>();
		String str = "";
		while(matcher.find()) {
			str = matcher.group().trim().replaceFirst(beginRegex, N).replaceAll(endRegex, N).replaceAll("\n"," ");
			list.add(str);
		}
		return list;
	}

	/**
	 * 解析超链接a,得到超链接的Text节点信息
	 * 意义：正则匹配<a>(内容)</a>  
	 * 其中内容不允许为<
	 * a中属性必须包括href='[^\"]+' [^>]* 表示任意字符
	 */
	public static String parseTagA(String tagHtml){
		Pattern p = Pattern.compile("<a[^>]*href=[\"']{1}[^\"]+[\"']{1}[^>]*>(.*?)</a>");
		Matcher m = p.matcher(tagHtml);
		if(m.find()){ 
			return m.group(1);
		}else{
			return "";	
		}
	}

}
