package com.ym.nutch.plugin.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.cyberneko.html.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.InputSource;

public class RequestUtil {

	public static final Logger LOG = LoggerFactory.getLogger(RequestUtil.class);
	
	public static StringBuilder getOfHttpClient(String url){
		HttpClient client = new HttpClient(new HttpClientParams(),new SimpleHttpConnectionManager(true) );
		GetMethod get = new GetMethod(url);
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try{
			HttpConnectionManagerParams managerParams =client.getHttpConnectionManager().getParams() ;
			managerParams.setConnectionTimeout(10000);
			managerParams.setSoTimeout(10000);
			client.executeMethod(get);
		      
			String line = null;

			br = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));

			while ((line = br.readLine()) != null) {
				// log.debug("line:"+line);
				sb.append(line);
				sb.append("\n");
			}
		    br.close();
		    //sb.append(get.getResponseBodyAsString());
	    } catch(Exception e){
	    	LOG.error("getOfHttpClient error:" + e);
	    } finally{
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
	    }
		
		return sb;
	}
	
	/**
	 * get方式发送请求数据
	 * 
	 * @param url
	 * @param charset
	 * @return StringBuilder
	 */
	public static StringBuilder getOfHttpURLConnection(String url, String charset){
		HttpURLConnection uc = null;
		StringBuilder sb = null;
		BufferedReader br = null;
		String code = "";
		try {
			sb = new StringBuilder();
			
			URL u = new URL(url);
		    uc = (HttpURLConnection) u.openConnection();

			uc.setConnectTimeout(10000);
			uc.setReadTimeout(10000);
			uc.setDoOutput(true);
			uc.setRequestMethod("GET");
			uc.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded; charset=" + charset);

			uc.setRequestProperty("accept","application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
			uc.setRequestProperty("user-agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10");

			uc.setRequestProperty("accept-language","zh-CN,zh;q=0.8");
			uc.setRequestProperty("accept-charset","GBK,utf-8;q=0.7,*;q=0.3");

			String line = null;

			br = new BufferedReader(new InputStreamReader(uc.getInputStream(),
					charset));

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (Exception e) {
			if(uc != null){
				try {
					code = uc.getResponseCode() + "";
				} catch (Exception e2) {
			    	LOG.error("getOfHttpURLConnection got http-code error");
				}
			}
	    	LOG.error("getOfHttpURLConnection error,http-code=" + code + ",error=" + e);
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return sb;
	}
	
	/**
	 * get方式发送请求数据
	 * @
	 * @param url
	 * @param charset
	 * @return StringBuilder 
	 * @throws IOException 
	 */
	public static BufferedReader getConnectByHttpUrl(String url,String charset) throws IOException{

		BufferedReader br = null;

		URL u = new URL(url);
		HttpURLConnection uc = null;
		uc = (HttpURLConnection) u.openConnection();

		uc.setConnectTimeout(20000);
		uc.setReadTimeout(20000);
		uc.setRequestMethod("GET");
		uc.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded; charset=GBK");

		uc.setRequestProperty(
				"accept",
				"application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		uc.setRequestProperty(
				"user-agent",
				"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10");
		uc.setRequestProperty("accept-language", "zh-CN,zh;q=0.8");
		uc.setRequestProperty("accept-charset", "GBK,utf-8;q=0.7,*;q=0.3");

		String contentType = uc.getContentType();
		String[] conEmt = null;
		if (contentType != null) {
			conEmt = contentType.toLowerCase().split("charset=");
		}
		if (conEmt != null && conEmt.length >= 2) {
			charset = conEmt[1].trim();
		}

		br = new BufferedReader(new InputStreamReader(uc.getInputStream(),
				charset));
		return br;
	}
	
	/**
	 * 调用request.getConnectByHttpUrl方法
	 * 通过neko工具，获取符合w3c标准的Document
	 * 
	 * @param url
	 * @param charset2
	 * @return DocumentFragment
	 */
	public static DocumentFragment getDocumentFragment(String url,String charset){

		HTMLDocumentImpl doc = new HTMLDocumentImpl();
		doc.setErrorChecking(false);
		DocumentFragment document =doc.createDocumentFragment();
		DocumentFragment frag = doc.createDocumentFragment();
		DOMFragmentParser parser = new DOMFragmentParser();
		BufferedReader in = null;
		try {
			//
			parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charset);
			parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
			parser.setFeature("http://cyberneko.org/html/features/report-errors", LOG.isTraceEnabled());

			in = RequestUtil.getConnectByHttpUrl(url, charset);
			
			InputSource input = new InputSource(in);
			parser.parse(input, frag);
			document.appendChild(frag);
			
			while (true) {
				frag = doc.createDocumentFragment();
				parser.parse(input, frag);
				if (!frag.hasChildNodes())
					break;
				document.appendChild(frag);
			}
		} catch (Exception e) {
			e.printStackTrace();
			document=null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return document;
	}
	
	public static DocumentFragment getDocumentFragmentByString(String message,String charset){

		HTMLDocumentImpl doc = new HTMLDocumentImpl();
		doc.setErrorChecking(false);
		DocumentFragment document =doc.createDocumentFragment();
		DocumentFragment frag = doc.createDocumentFragment();
		DOMFragmentParser parser = new DOMFragmentParser();
		BufferedReader in = null;
		try {
			//
			parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charset);
			parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
			parser.setFeature("http://cyberneko.org/html/features/report-errors", LOG.isTraceEnabled());

			in = new BufferedReader(new StringReader(message));
			InputSource input = new InputSource(in);
			parser.parse(input, frag);
			document.appendChild(frag);
			
			while (true) {
				frag = doc.createDocumentFragment();
				parser.parse(input, frag);
				if (!frag.hasChildNodes())
					break;
				document.appendChild(frag);
			}
		} catch (Exception e) {
			e.printStackTrace();
			document=null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return document;
	}
	
	/**
	 * 调用request.getConnectByHttpUrl方法
	 * 通过neko工具，获取符合w3c标准的Document
	 * 
	 * @param url
	 * @param charset2
	 * @return Document
	 */
	public static Document getDocument(String url,String charset){
		Document document = null;
		DOMParser parser = new DOMParser();
		BufferedReader in = null;
		try {
			//
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding",
					charset);
			parser.setFeature("http://xml.org/sax/features/namespaces", false);

			in = RequestUtil.getConnectByHttpUrl(url, charset);

			parser.parse(new InputSource(in));
			document = parser.getDocument();
		} catch (Exception e) {
			e.printStackTrace();
			document=null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return document;
	}
	
	public static String sendPost(String url, String param) {
		String result = "";
		try {
			URL httpurl = new URL(url);
			HttpURLConnection httpConn = (HttpURLConnection) httpurl.openConnection();
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			PrintWriter out = new PrintWriter(httpConn.getOutputStream());
			out.print(param);
			out.flush();
			out.close();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					httpConn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
			System.out.println("请求返还码:" + httpConn.getResponseCode());
			in.close();
		} catch (Exception e) {
			System.out.println("没有结果！" + e);
		}
		return result;
	}
	
	
	/**
	 * 从指定地址获取网页源代码
	 * 
	 * @param url
	 *            网页地址
	 * @param charset
	 *            网页编码-字符集
	 * @return 网页源代码
	 * @throws Exception
	 */
	public static String getPageContent(String url, String charset)
			throws Exception {
		InputStream ins = null;
		try {
			URL javaUrl = new URL(url);
			HttpURLConnection http = (HttpURLConnection) javaUrl
					.openConnection();
			http.setRequestMethod("GET");
			http.setConnectTimeout(60 * 1000);
			http.setReadTimeout(60 * 1000);
			http.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.20 (KHTML, like Gecko) Chrome/25.0.1337.0 Safari/537.20");
			int code = http.getResponseCode();
//			System.out.println("返回码=" + code);
			if (code == 200) {
				ins = http.getInputStream();
				return new String(parseStream(ins), charset);
			} else if (code == 403) {
				throw new Exception("HTTP返回：" + code + ",IP被封，暂时无法访问");
			}
		}catch (Exception e) {
			System.out.println("url="+ url + "#" + e.getMessage());
		}
		finally {
			try {
				if (ins != null) {
					ins.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return "";
	}
	
	/**
	 * 从输入流中解析数据【此方法不对传入的输入流做释放操作！！！】
	 * 
	 * @param ins
	 *            输入流
	 * @return 元数据
	 * @throws Exception
	 */
	public static byte[] parseStream(InputStream ins) throws Exception {
		int len = -1;
		byte[] data = null;
		byte[] buffer = new byte[1024];
		ByteArrayOutputStream outs = null;
		try {
			outs = new ByteArrayOutputStream();
			while ((len = ins.read(buffer)) != -1) {
				outs.write(buffer, 0, len);
			}
			outs.flush();
			data = outs.toByteArray();
		} finally {
			if (outs != null) {
				outs.close();
			}
		}
		return data;
	}
	
	/**
	 * get方式发送请求数据
	 * 根据京东商品评论请求参数特殊
	 * 
	 * @param url
	 * @param charset
	 * @return StringBuilder
	 */
	public static StringBuilder getOfHttpURLConnectionByJDComments(String url, String charset){
		HttpURLConnection uc = null;
		StringBuilder sb = null;
		BufferedReader br = null;
		String code = "";
		try {
			sb = new StringBuilder();
			
			URL u = new URL(url);
		    uc = (HttpURLConnection) u.openConnection();

			uc.setConnectTimeout(10000);
			uc.setReadTimeout(10000);
			uc.setDoOutput(true);
			uc.setRequestMethod("GET");
			uc.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded; charset=" + charset);

			uc.setRequestProperty("accept","application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
			uc.setRequestProperty("user-agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10");

			uc.setRequestProperty("accept-language","zh-CN,zh;q=0.8");
			uc.setRequestProperty("accept-charset","GBK,utf-8;q=0.7,*;q=0.3");

			uc.setRequestProperty("Referer",url);
			
			String line = null;

			br = new BufferedReader(new InputStreamReader(uc.getInputStream(),
					charset));

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (Exception e) {
			if(uc != null){
				try {
					code = uc.getResponseCode() + "";
				} catch (Exception e2) {
			    	LOG.error("getOfHttpURLConnection got http-code error");
				}
			}
	    	LOG.error("getOfHttpURLConnection error,http-code=" + code + ",error=" + e);
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return sb;
	}
	
	/**
	 * get方式发送请求数据
	 * 根据京东商品评论请求参数特殊
	 * 
	 * @param url
	 * @param charset
	 * @return StringBuilder
	 */
	public static StringBuilder getOfHttpURLConnectionByAmazon(String url, String charset){
		HttpURLConnection uc = null;
		StringBuilder sb = null;
		BufferedReader br = null;
		String code = "";
		try {
			sb = new StringBuilder();
			
			URL u = new URL(url);
		    uc = (HttpURLConnection) u.openConnection();

			uc.setConnectTimeout(10000);
			uc.setReadTimeout(10000);
			uc.setDoOutput(true);
			uc.setRequestMethod("GET");
			uc.setRequestProperty("Content-Type",
					"image/jpeg");

			uc.setRequestProperty("accept","image/webp,*/*;q=0.8");
			uc.setRequestProperty("user-agent","Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");

			uc.setRequestProperty("accept-language","gzip,deflate,sdch");
			uc.setRequestProperty("Accept-Encoding","zh-CN,zh;q=0.8");

			uc.setRequestProperty("Referer",url);
			
			String line = null;

			br = new BufferedReader(new InputStreamReader(uc.getInputStream(),charset));

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (Exception e) {
			if(uc != null){
				try {
					code = uc.getResponseCode() + "";
				} catch (Exception e2) {
			    	LOG.error("getOfHttpURLConnection got http-code error");
				}
			}
	    	LOG.error("getOfHttpURLConnection error,http-code=" + code + ",error=" + e);
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return sb;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String urlString= "http://www.lusen.com/MallService.axd";
//		String urlString= "http://www.lusen.com";
		String paramString ="encoding=UTF-8&action=GetParams&ProductId=2429&ProductId&GoodsId=536";
//		String paramString ="";
		String result= sendPost(urlString, paramString);
		System.out.println(result);
		
//		
//			if(Pattern.compile("^[A-Za-z0-9]\\.?[A-Za-z0-9|\\-]\\.{1}[A-Za-z]+").matcher("javascript:regist();").find()){
//				System.out.println("1OK!!!");
//			}
//			if(Pattern.compile("^[A-Za-z0-9]+[\\/+|\\.+]+[A-Za-z0-9]+").matcher("products/1320-5019-5020.html").find()){
//				System.out.println("2OK!!!");
//			}
//			
//			if(Pattern.compile("\\(|\\,|\\)|\\;|\\+|\\=").matcher("str.substring(0,(str.length-6));str1+=RndNum(6);document.getElementById(eleId).href=str1;}function").find()){
//				System.out.println("3OK!!!");
//			}
//			//
//			DocumentFragment sb = RequestUtil.getDocumentFragment("http://www.360buy.com/product/497851.html","UTF-8");
//			
//			String pathString="//BODY";
//			Node node= (Node) TemplateUtil.getNode(sb, pathString);
//			System.out.println("getTextContent:==="+node.getTextContent());
//			System.out.println("getName:==="+node.getNamespaceURI());
//
//
//			
//			
//			System.out.println(sb.toString());
//			String url = "http://www.360buy.com/product/1000394618.html";
//		    if(Pattern.compile("360buy\\.com/product/\\d+\\.html").matcher(url).find()||
//		    		Pattern.compile("mvd\\.360buy\\.com/\\d+\\.html").matcher(url).find()||
//		    		Pattern.compile("book\\.360buy\\.com/\\d+\\.html").matcher(url).find()||
//		    		Pattern.compile("product\\.dangdang\\.com/product\\.aspx").matcher(url).find()){//匹配京东、当当网详情页面
//		    	
//		    	System.out.println("4OK!!!");
//		    }
	}

}
