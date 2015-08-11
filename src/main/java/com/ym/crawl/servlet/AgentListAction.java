package com.ym.crawl.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONObject;
import com.ym.framework.implement.CrawlerListService;
import com.ym.framework.interfaces.ICrawlerList;
import com.ym.framework.util.RetCode;
import com.ym.nutch.plugin.util.StrUtil;


@Controller
public class AgentListAction {

	private static final long serialVersionUID = -6844704788488508423L;
	protected static Logger	log= Logger.getLogger(AgentListAction.class);
	
	protected void printKeyHeaders(HttpServletRequest request) {
		log.info("===============================");
		log.info("Content-type:" + request.getContentType());
		int len = request.getContentLength();
		log.info("Content-length:" + len);
		log.info("getCharacterEncoding:" + request.getCharacterEncoding());
		log.info("Content-Disposition:" + request.getHeader("Content-Disposition"));
		log.info("===============================");
	}

	@RequestMapping(value = "/1001")
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String busiCode="1001";
		String resCode=RetCode.SUCCESS;
		String resMsg="";
		String isProductPage="1";  //默认为非商品：1
		JSONObject json=new JSONObject();
		
		request.setCharacterEncoding("UTF-8");
		String sellerCode=request.getParameter("sellerCode");
		String productUrl=request.getParameter("rawUrl");
		log.info("AgentListAction Start! sellerCode="+sellerCode+"  , productUrl="+productUrl);
		if(!StrUtil.isEmpty(sellerCode) && !StrUtil.isEmpty(productUrl)){
			ICrawlerList carawList=new CrawlerListService();
			try{
				isProductPage=carawList.isCrawlProduct(sellerCode,productUrl);
				resMsg="操作成功";
			}catch(Exception e){
				resMsg="操作失败";
				resCode=RetCode.FAILURE;
			}
			json.put("isCdt", isProductPage);
			json.put("busiCode", busiCode);
			json.put("resCode", resCode);
			json.put("resMsg", resMsg);
			writeJson(json, response);
		}else{
			log.info(this.getClass().getSimpleName()+": End of program:sellerCode Is Null Or  productUrl Is Null !");
		}
	}
	
	//返回json数据
	public void writeJson(JSONObject json, HttpServletResponse response) {
		try {
			response.setCharacterEncoding("utf8");
			response.setContentType("text/html;charset=utf-8");
			response.getWriter().write(json.toString());
			response.getWriter().flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
