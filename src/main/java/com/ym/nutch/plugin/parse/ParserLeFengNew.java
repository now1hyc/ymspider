package com.ym.nutch.plugin.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductNameIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.parse.template.PriceTool;
import com.ym.nutch.plugin.util.GetImgPriceUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

/**
 * 2013年5月23日 修改productFlag正则和id
 * 
 * @author Administrator
 */
public class ParserLeFengNew extends ParserParent {

	public static final Logger				LOG				= LoggerFactory.getLogger(ParserLeFengNew.class);

	// http://product.lefeng.com/coat/111986.html
	// http://product.lefeng.com/product/10968.html 部分商品页会跳转至/coat/下
	// http://product.lefeng.com/goods/productDetail_coat.jsp?productId=189275&wt.ct=link&wt.s_pg=navi&wt.s_pf=huanxing&biid=43933
	// http://product.lefeng.com/pklist/185039.html?productId=185039
	// 商品页正则
	private Pattern							itemFlag1		= Pattern
																	.compile("http://product.lefeng.com/product/([0-9]+).html.*?");						// 标准
	private Pattern							itemFlag2		= Pattern
																	.compile("http://product.lefeng.com/coat/([0-9]+).html.*?");
	private Pattern							itemFlag3		= Pattern
																	.compile("http://product.lefeng.com/.*productid=([0-9]+).*?");
	private Pattern							itemFlag4		= Pattern
																	.compile("http://track.lefeng.com/.*productid=([0-9]+).*");							// 未知域名但可以提取商品ID

	// 手机版商品url：http://m.lefeng.com/index.php/product/index/pid/1804462
	private Pattern							itemFlag5		= Pattern
																	.compile("http://m.lefeng.com/index.php/product/index/pid/([0-9]+).*?");

	private Pattern							listFlag1		= Pattern
																	.compile("http://s.lefeng.com/(?:directory|coat|sweater)/([_\\d%A-Za-z]+).html.*");

	private static Map<String, JSONArray>	JSONArrayMap	= new HashMap<String, JSONArray>();

	public ParserLeFengNew() {
		super();
		init();
	}

	public ParserLeFengNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	public void init() {
		sellerCode = "1030";
		seller = "乐蜂网";
		priceXpath = "//INPUT[@id='uprice']/@value";
		brandXpath = "//DIV[@class='qijian']/A/B";
		classicXpath = "//P[@class='path']//descendant::A[position()>1]";
		titleXPath = "//TITLE";
		productNameXpath = "//SPAN[@class='pname']";
		keywordXpath = "//META[@name='keywords']/@content";
		imgXpath = "//DL[@id='imgshow']//descendant::DT/IMG/@src" + " | //DIV[@class='pic']/IMG/@src"
				+ " | //DIV[@class='spec-item']/A/@bigsrc";
		mparasXpath = "//DIV[@class='cont tabc']/DIV/TABLE[@style]//TBODY//descendant::TR";
		contentDetailXpath = "//DIV[@class='cont tabc']" + "| //DIV[@id='detail']/H3/TABLE";
		marketPriceXpath = "//INPUT[@id='mprice']/@value";
		nextPageXpath = "//DIV[@class='nextPageClass']/A/@href";
		totalCommentXpath = "//DIV[@class='tag_wm wrap_tag']/UL/LI";
		commentPercentXpath = "//DIV[@class='pj_percent']/P";
		oriCatCodeXpath = "//P[@class='path']/A[last()]/@href";
		// 合法url规则(只接受，商品页+列表页)
		productFlag = itemFlag1;
		// 商品页地址规则
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		productUrlList.add(itemFlag3);
		productUrlList.add(itemFlag4);
		productUrlList.add(itemFlag5);
	}

	@Override
	public void setId() throws ProductIDIsNullException {
		String pid = "";
		// url="http://product.lefeng.com/product/180281.html";
		String tempUrl = url.toLowerCase();
		for (int i = 0; i < productUrlList.size(); i++) {
			Matcher m = productUrlList.get(i).matcher(tempUrl);
			if (m.find()) {
				pid = m.group(1);
				break;
			}
		}
		if (pid == null || pid.isEmpty()) {
			throw new ProductIDIsNullException(url + ":未能获取商品id！！");
		} else {
			String id = sellerCode + pid;
			super.product.setPid(id);
			super.product.setOpid(pid);
		}
	}

	// 通过图片解析价格
	@Override
	public void setPrice() {
		String price = null;
		Node priceNode = TemplateUtil.getNode(root, priceXpath);
		if (priceNode != null) {
			price = trimPrice(priceNode.getTextContent()).trim();
		} else {
			Node priceNode2 = TemplateUtil.getNode(root, "//P[@class='specials']/IMG/@src");
			if (priceNode2 != null) {
				String urlprice = priceNode2.getTextContent();
				price = GetImgPriceUtil.getPriceAsStr(urlprice);
			}
		}
		if (!StrUtil.isEmpty(price)) {
			price = trimPrice(price).trim();
			this.product.setPrice(price);
		} else {
			this.product.setPrice("0");
		}
	}

	// 品牌
	@Override
	public void setBrand() {
		String brands = "";
		Node brandNode2 = TemplateUtil.getNode(root, brandXpath);
		if (brandNode2 != null) {
			brands = brandNode2.getTextContent().trim();
		}
		if (!"".equals(brands)) {
			this.product.setBrand(brands);
		}
	}

	@Override
	public void setProductName() throws ProductNameIsNullException {
		Node node = TemplateUtil.getNode(root, productNameXpath);
		if (node != null) {
			String productName = node.getTextContent().replaceAll("[（]", "(").replaceAll("[）]", ")")
					.replaceAll("[\\s]{2,}", " ");
			if (productName == null || productName.isEmpty()) {
				new ProductNameIsNullException(url + "商品名称为空！" + productNameXpath + "不匹配 ");
			} else {
				super.product.setProductName(productName);
			}
		} else {
			new ProductNameIsNullException(url + "商品名称为空！" + productNameXpath + "不匹配 ");
		}
	}

	@Override
	public void setShortName() {
		String shortName = super.product.getProductName();
		if (shortName != null && !"".equals(shortName)) {
			this.product.setShortName(shortName);
			this.product.setShortNameDetail(shortName);
		} else {
			LOG.debug("无法正常获取商品的短名称！ " + url);
		}
	}

	@Override
	public void setClassic() {
		StringBuffer sb = new StringBuffer();
		NodeList node_locationList = TemplateUtil.getNodeList(root, classicXpath);
		if (node_locationList.getLength() > 0) {
			for (int i = 0; i < node_locationList.getLength(); i++) {
				String value = node_locationList.item(i).getTextContent().replaceAll("[\\s]{1,}", "");
				if (value != null && !"".equals(value)) {
					sb.append(value);
				}
				if (i < node_locationList.getLength() - 1) {
					sb.append("[xm99]");
				}
			}
			String classicValue = sb.toString();
			if (classicValue != null && !"".equals(classicValue)) {
				product.setClassic(classicValue);
				product.setCategory(classicValue);
			}
		}
	}

	/**
	 * 20131122添加：当前销售价格的折扣=电商的当前销售价格除以市场价格后，精确到小数点后两位。
	 */
	@Override
	public void setD2ratio() {
		String d2ratio = "";
		double d2ratioByDouble = 0.00;
		String price = this.product.getPrice();
		String marketPrice = this.product.getMaketPrice();
		double curPrice = StrUtil.isEmpty(price) ? 0 : Double.parseDouble(price);
		double curMarketPrice = StrUtil.isEmpty(marketPrice) ? 0 : Double.parseDouble(marketPrice);
		if (curPrice > 0 && curMarketPrice > curPrice) {// 只保留小数点后两位
			d2ratioByDouble = curPrice / curMarketPrice;
		}
		d2ratioByDouble = PriceTool.getdRatio(d2ratioByDouble);
		d2ratio = String.valueOf(d2ratioByDouble);
		this.product.setD2ratio(d2ratio);
	}

	// 20131122添加
	@Override
	public void setOriCatCode() {
		String oriCatCode = "";
		StringBuffer sb = new StringBuffer();
		if (!oriCatCodeXpath.isEmpty()) {
			Node oriCatCodeNode = TemplateUtil.getNode(root, oriCatCodeXpath);
			if (oriCatCodeNode != null) {
				String oriCatCodeByHref = oriCatCodeNode.getTextContent().trim();
				if (oriCatCodeByHref != null && !"".equals(oriCatCodeByHref)) {
					Matcher productFlagMatch = listFlag1.matcher(oriCatCodeByHref.toLowerCase());
					if (productFlagMatch.find()) {
						String oriCatCodeValue = productFlagMatch.group(1);
						String[] oriCateCodeArry = oriCatCodeValue.split("_");
						if (oriCateCodeArry.length > 0) {
							for (int i = 0; i < oriCateCodeArry.length; i++) {
								if (i != 0) {
									sb.append("#");
								}
								oriCatCode = oriCateCodeArry[i];
								sb.append(oriCatCode);
							}
						}
					}
				}
			}
		}
		if (sb.length() > 0) {
			oriCatCode = sb.toString();
			this.product.setOriCatCode(oriCatCode);
		}
	}

	public void initCommentContent() {
		try {
			String commentPid = this.product.getPid();
			if (!StrUtil.isEmpty(commentPid)) {
				String commentSecondUrl = "http://review.lefeng.com/interface/query_comment_ajax.jsp?pid=" + commentPid
						+ "&type=0&sort=0";
				String commentContentValue = RequestUtil.getOfHttpURLConnection(commentSecondUrl, "utf-8").toString();
				if (!StrUtil.isEmpty(commentContentValue)) {
					JSONObject jsonPrim = JSONObject.parseObject(commentContentValue);
					String data = jsonPrim.getString("data");
					JSONArray jsonArray = JSONArray.parseArray(data);
					if (jsonArray.size() > 0) {
						JSONArrayMap.put("jsonArray", jsonArray);
					}
				}
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName() + ": The initCommentContent Is Parser Error! " + url);
		}
	}

	@Override
	public void setCommentList() {
		// 初始化评论内容文本
		initCommentContent();
		try {
			JSONArray jsonArray = JSONArrayMap.get("jsonArray");
			int arraySize = jsonArray == null ? 0 : jsonArray.size();
			if (0 < arraySize) {
				List<Comment> clist = new ArrayList<Comment>();
				for (int i = 0; i < arraySize; i++) {
					JSONObject o = jsonArray.getJSONObject(i);
					if (o != null) {
						String cid = StringUtils.trimToEmpty(o.getString("id"));
						if (StringUtils.isNotEmpty(cid)) {
							String name = StringUtils.trimToEmpty(o.getString("userName"));
							String content = StringUtils.trimToEmpty(o.getString("reply"));
							if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(content)) {
								String c = name + "[xm99]" + content;
								Comment cm = new Comment();
								cm.setCommentId(cid);
								cm.setCommentContent(c);
								clist.add(cm);
							}
						}
					}
				}
				this.product.setClist(clist);
			}

		} catch (Exception e) {
		}
	}
}
