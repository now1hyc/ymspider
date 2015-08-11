package com.ym.crawl.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONObject;
import com.ym.framework.implement.CrawlerProductService;
import com.ym.framework.interfaces.ICrawlerProduct;
import com.ym.framework.util.RetCode;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.plugin.util.StrUtil;

@Controller
public class AgentProductAction {

	private static final long	serialVersionUID	= -6844704788488508423L;
	protected static Logger		log					= Logger.getLogger(AgentProductAction.class);

	protected void printKeyHeaders(HttpServletRequest request) {
		log.info("===============================");
		log.info("Content-type:" + request.getContentType());
		int len = request.getContentLength();
		log.info("Content-length:" + len);
		log.info("getCharacterEncoding:" + request.getCharacterEncoding());
		log.info("Content-Disposition:" + request.getHeader("Content-Disposition"));
		log.info("===============================");
	}

	@RequestMapping(value = "/1002")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String busiCode = "1002";
		String resCode = RetCode.SUCCESS;
		String resMsg = "";
		JSONObject json = new JSONObject();
		request.setCharacterEncoding("UTF-8");
		String sellerCode = request.getParameter("sellerCode");
		String productUrl = request.getParameter("oriUrl");
		log.info("AgentProductAction Start! sellerCode=" + sellerCode + "  , productUrl=" + productUrl);
		if (!StrUtil.isEmpty(sellerCode) && !StrUtil.isEmpty(productUrl)) {
			ICrawlerProduct carawlProduct = new CrawlerProductService();
			OriProduct product = carawlProduct.crawlerProduct(sellerCode, productUrl);
			try {
				if (product != null) {
					resMsg = "操作成功";
					String id = product.getPid(); // 商品编码 sellercode+电商商品编码
					if (!StrUtil.isEmpty(id)) {
						json.put("id", id);
					}
					String bigPic = product.getOrgPic(); // 商品图片
					if (!StrUtil.isEmpty(bigPic)) {
						json.put("bigPic", bigPic);
					}
					String price = product.getPrice(); // 商品价格
					if (!StrUtil.isEmpty(price)) {
						json.put("price", price);
					}
					String productName = product.getProductName(); // 商品名称
					if (!StrUtil.isEmpty(productName)) {
						json.put("productName", productName);
					}
				} else {
					resMsg = "操作失败";
					resCode = RetCode.FAILURE;
				}
			} catch (Exception e) {
				resMsg = "操作失败";
				resCode = RetCode.FAILURE;
			}
			json.put("busiCode", busiCode);
			json.put("resCode", resCode);
			json.put("resMsg", resMsg);
			writeJson(json, response);

			// 返回json数据之后，在对数据进行入库操作；
			try {
				if (product != null) {
					carawlProduct.productStorage(product);
				}
			} catch (Exception e) {
				log.error("Product Data Storage Is Error!");
			}
		} else {
			log.info(this.getClass().getSimpleName() + ": End of program:sellerCode Is Null Or  productUrl Is Null !");
		}
	}

	// 返回json数据
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

	// public static void main(String[] args) throws ServletException, IOException {
	// AgentProductAction aa=new AgentProductAction();
	// HttpServletRequest request=null;
	// HttpServletResponse response=null;
	// aa.doPost(request, response);
	// }
}
