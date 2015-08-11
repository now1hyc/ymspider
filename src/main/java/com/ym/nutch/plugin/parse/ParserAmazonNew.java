package com.ym.nutch.plugin.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONObject;
import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.parse.template.PriceTool;
import com.ym.nutch.plugin.util.Constant;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

public class ParserAmazonNew extends ParserParent {

	public static final Logger	LOG			= LoggerFactory.getLogger(ParserAmazonNew.class);

	// 商品页正则 http://www.amazon.cn/dp/B009EODQZE
	private Pattern				itemFlag1	= Pattern.compile("(?:^http://www.amazon.cn.*/dp/)([a-zA-Z0-9]+).*?");

	// 手机端分类列表url http://www.amazon.cn/gp/aw/d/B00FFVIPN8/
	private Pattern				itemFlag2	= Pattern.compile("http://www.amazon.cn/gp/aw/d/([0-9a-zA-Z]+)/.*?");

	public ParserAmazonNew() {
		super();
		init();
	}

	public ParserAmazonNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	public void init() {
		sellerCode = "1011";
		seller = "亚马逊";
		idXPath = "//INPUT[@id='ASIN']/@value" + " | //INPUT[@name='ASIN.0']/@value";
		brandXpath = "//DIV[@class='buying']/A" + "| //SPAN[@class='brandLink']/A";
		classicXpath = "//DIV[@class='bucket']/H2";
		priceXpath = "//SPAN[@class='priceLarge']" + " | //B[@class='priceLarge']";
		marketPriceXpath = "//TD[@class='listprice']" + "| //SPAN[@id='listPriceValue']"
				+ " | //SPAN[@class='listprice']";
		discountPriceXpath = "//SCRIPT";
		isCargoXpath = "//SPAN[@class='availRed']";
		productNameXpath = "//SPAN[@id='btAsinTitle']/SPAN";
		shortNameXpath = "//TABLE/TR/TD[H2[contains(text(),'基本信息')]]/DIV/UL/LI[B[contains(text(),'型号')]]";
		shortNameDetailXpath = "//DIV[@class='aplus']";
		keywordXpath = "//META[@name='keywords']/@content";
		imgXpath = "//IMG[@id='original-main-image']/@src";
		contentDetailXpath = "//DIV[@class='productDescriptionWrapper']" + " | //DIV[@id='importantInformation']"
				+ " | //TABLE/TR/TD[H2[contains(text(),'基本信息')]]//descendant::LI";
		mparasXpath = "//TABLE/TR/TD[H2[contains(text(),'基本信息')]]//descendant::LI";
		oriCatCodeXpath = "//DIV[H2[contains(text(),'查找其它相似商品')]]//descendant::LI[1]/A/@href";
		checkOriCatCodeXpath = "//DIV[H2[contains(text(),'查找其它相似商品')]]/DIV[@class='content']";
		keywordsFilter.add("卓越亚马逊|卓越网|卓越|亚马逊|joyo|amazon|,|、");
		titleFilter.add("卓越亚马逊|卓越网|卓越|亚马逊|joyo|amazon|,");
		totalCommentXpath = "//DIV[@class='tiny']/B[1]";
		commentStarXpath = "//DIV[@class='gry txtnormal acrRating']";

		productFlag = itemFlag1;
		throughList.add(itemFlag1);
		throughList.add(itemFlag2);
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);

		// outLinkXpath.add("//DIV[normalize-space(@class='toTheEdge productList')]/TABLE//descendant::TR/TD/A");
		// outLinkXpath.add("//A[IMG[contains(@src,'.jpg')]]");
	}

	/**
	 * 适配部分书类商品：http://www.amazon.cn/dp/B008Z8VN4G
	 */
	@Override
	public void setId() throws ProductIDIsNullException {
		String pid = null;
		Matcher ma1 = productFlag.matcher(url);
		if (ma1.find()) {
			pid = ma1.group(1);
		}
		if (pid == null || pid.isEmpty()) {
			throw new ProductIDIsNullException("-->Error---Product Id Is Null！,url=" + url);
		} else {
			String id = sellerCode + pid;
			product.setPid(id);
			product.setOpid(pid);
		}
	}

	/**
	 * 增加了对书的品牌字段解析，一般书是没有品牌的，用出版社作为品牌字段。 http://www.amazon.cn/%E5%9B%BE%E4%B9%A6/dp/B00E3966SY
	 */
	@Override
	public void setBrand() {
		NodeList nodeList = TemplateUtil.getNodeList(root, brandXpath);
		Node brandNode = null;
		String value = "";
		if (nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node tempNode = nodeList.item(i);
				int index = tempNode.getAttributes().getNamedItem("href").getTextContent().indexOf("search-type=ss");
				if (index >= 0) {
					brandNode = tempNode;
					break;
				}
			}
			if (brandNode != null) {
				value = brandNode.getTextContent().trim();
				value = value.replaceAll("[\\s]{1,}", "\\[xm99\\]");

			}
		}
		// 对于图书类适配，以出版社名称作为品牌信息。
		if ("".equals(value)) {
			String pressNameXpth = "//TABLE/TR/TD[H2[contains(text(),'基本信息')]]/DIV/UL/LI[1]";
			Node pressNameNode = TemplateUtil.getNode(root, pressNameXpth);
			if (pressNameNode != null) {
				value = pressNameNode.getTextContent().trim();
			}
		}
		value = value.replaceAll("[\\s]{1,}", "").replaceAll("出版社:", "").replaceAll("\\(.*?\\)", "").trim();
		product.setBrand(value);
	}

	@Override
	public void setIsCargo() {
		if (!isCargoXpath.isEmpty()) {
			Node isCargoNode = TemplateUtil.getNode(root, isCargoXpath);
			if (isCargoNode != null) {
				String isCargValue = isCargoNode.getTextContent().replaceAll("[\\s]{1,}", "");
				if (isCargValue.contains("无货")) {
					this.product.setIsCargo(0);
					return;
				}
			}
		}
		this.product.setIsCargo(1);
	}

	@Override
	public void setClassic() {
		Node nodeClass = null;
		NodeList list = TemplateUtil.getNodeList(root, classicXpath);
		if (list.getLength() > 0) {
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				int index = node.getTextContent().indexOf("查找其它相似商品");
				if (index >= 0) {
					nodeClass = node.getParentNode();
					break;
				}
			}
			String value = "";
			if (nodeClass != null) {
				List<Node> listResult = TemplateUtil.getTextHelper(nodeClass, "li");
				if (listResult.size() > 0) {
					value = listResult.get(0).getTextContent();
					value = value.replaceAll(">", Constant.XMTAG).replaceAll("[\\s]{1,}", "");
				}
			}
			product.setClassic(value);
		}
	}

	/**
	 * 商品的短名称：品牌+型号
	 */
	@Override
	public void setShortName() {
		String modelValue = "";
		String brandValue = this.product.getBrand();
		if (brandValue != null || !"".equals(brandValue)) {
			if (brandValue.contains("[xm99]")) {
				String[] brandValueArry = brandValue.split("\\[xm99\\]");
				for (int i = 0; i < brandValueArry.length; i++) {
					String brandValueStr = brandValueArry[i];
					char c = brandValueStr.charAt(0);
					if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
						brandValue = brandValueStr;
						break;
					}
				}
			}
		}
		if (!shortNameXpath.isEmpty()) {
			Node nodelNode = TemplateUtil.getNode(root, shortNameXpath);
			if (nodelNode != null) {
				modelValue = " " + nodelNode.getTextContent().replaceAll("型号:", "").replaceAll("[\\s]{1,}", "");
			}
		}
		String shortName = brandValue + modelValue;
		if (shortName != null && !"".equals(shortName)) {
			this.product.setShortName(shortName);
		}
	}

	@Override
	public void setOriCatCode() {
		NodeList categoryNodeList = TemplateUtil.getNodeList(root, oriCatCodeXpath);
		if (categoryNodeList.getLength() > 0) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < categoryNodeList.getLength(); i++) {
				if (i != 0) {
					sb.append("#");
				}
				String href = categoryNodeList.item(i).getTextContent().trim();
				href = href.substring(href.lastIndexOf("=") + 1, href.length()).replaceAll("[n%3A]", "")
						.replaceAll("[\\s]{1,}", "");
				sb.append(href);
			}
			product.setOriCatCode(sb.toString());
		} else {
			LOG.info("未能正常解析商品原始分类！  " + url);
		}
	}

	// 处理亚马逊书分类无货商品价格问题
	@Override
	public void setPrice() throws ProductPriceIsNullException {
		String price = "";
		try {
			Node priceNode = TemplateUtil.getNode(root, priceXpath);
			if (priceNode != null) {
				price = trimPrice(priceNode.getTextContent().replaceAll("[\\s-]{1,}", "")).trim();
				if (price == null || price.isEmpty()) {
					throw new ProductPriceIsNullException(url + "-->未能获取商品销售价格！setPrice异常不匹配");
				}
			} else {
				// 商品的价格提取xpath定义
				String hasPriceXpath = "//DIV[@class='buying' and @id='priceBlock']";
				Node priceNode2 = TemplateUtil.getNode(root, hasPriceXpath);
				if (priceNode2 == null) { // 无法提取价格，设默认值
					price = "0";
					LOG.info(url + "-->product price take null and set price=0");
				}
			}
			product.setPrice(price);
		} catch (Exception ex) {
			LOG.info(url + "-->未能获取商品销售价格！setPrice异常不匹配!!", ex);
		}
	}

	/**
	 * 保存中文编码 中文分类跟我们的导航所保存的中文分类一致，所以可以直接拿classic来填充category
	 */
	@Override
	public void setCategory() {
		String classicCategory = this.product.getClassic();
		if (classicCategory != null && !"".equals(classicCategory)) {
			this.product.setCategory(classicCategory);
		}
	}

	/**
	 * 由于第二种短名称没有很正规，暂时分三种页面布局处理
	 */
	@Override
	public void setshortNameDetail() {
		String modelValue = "";
		String modelValue2 = "";
		String shortName = "";
		String brandValue = "";
		try {
			brandValue = this.product.getBrand();
			if (brandValue != null || !"".equals(brandValue)) {
				if (brandValue.contains("[xm99]")) {
					String[] brandValueArry = brandValue.split("\\[xm99\\]");
					for (int i = 0; i < brandValueArry.length; i++) {
						String brandValueStr = brandValueArry[i];
						char c = brandValueStr.charAt(0);
						if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
							brandValue = brandValueStr;
							break;
						}
					}
				}
			}
			if (!shortNameDetailXpath.isEmpty()) {
				Node shortNameDetailNode1 = TemplateUtil.getNode(root, shortNameDetailXpath);
				if (shortNameDetailNode1 != null) {
					modelValue = shortNameDetailNode1.getTextContent().replaceAll("[\\s]{2,}", " ").trim();
					if (modelValue != null && !"".equals(modelValue)) {
						if (modelValue.indexOf("详细参数") >= 0) {
							String[] modelArry = modelValue.split("详细参数");
							modelValue = modelArry[1];
						}
						if (modelValue.indexOf("型号") >= 0) {
							if (modelValue.indexOf("外观设计") >= 0) {
								modelValue2 = modelValue.substring(modelValue.indexOf("型号") + 3,
										modelValue.indexOf("外观设计"));
							}
						}
					}
				} else {
					String shortNameDetailXpath2 = "//DIV[@class='info_content']/UL/LI[contains(text(),'型号')]";
					Node shortNameDetailNode2 = TemplateUtil.getNode(root, shortNameDetailXpath2);
					if (shortNameDetailNode2 != null) {
						modelValue = shortNameDetailNode2.getTextContent().replaceAll("[\\s]{1,}", "").trim();
						if (modelValue != null && !"".equals(modelValue)) {
							modelValue2 = modelValue.replaceAll("：", ":").replaceAll("型号:", "");
						}
					}
				}
			}
			if ("".equals(modelValue2)) {
				String shortNameDetailXpath3 = "//TABLE/TR[TD[contains(text(),'产品型号')]]";
				Node shortNameDetailNode3 = TemplateUtil.getNode(root, shortNameDetailXpath3);
				if (shortNameDetailNode3 != null) {
					modelValue = shortNameDetailNode3.getTextContent().replaceAll("[\\s]{1,}", "").trim();
					if (modelValue != null && !"".equals(modelValue)) {
						modelValue2 = modelValue.substring(modelValue.indexOf("产品型号") + 4, modelValue.length());
					}
				}
			}
		} catch (Exception e) {
			LOG.info(ParserAmazonNew.class.getSimpleName() + ":setshortNameDetail() Parser Is ERROR! " + url);
		}
		if (modelValue2 != null && !"".equals(modelValue2)) {
			modelValue2 = " " + modelValue2;
			shortName = brandValue + modelValue2;
		} else {
			shortName = this.product.getShortName();
		}
		if (shortName != null && !"".equals(shortName)) {
			this.product.setShortNameDetail(shortName);
		}
	}

	/**
	 * 折扣活动截止时间,只限活动商品
	 */
	@Override
	public void setLimitTime() {
		String endDate = "";
		String limitTimeXpath = "//SCRIPT";
		NodeList limitTimeNodeList = TemplateUtil.getNodeList(root, limitTimeXpath);
		if (limitTimeNodeList.getLength() > 0) {
			for (int i = 0; i < limitTimeNodeList.getLength(); i++) {
				String limitTimeValue = limitTimeNodeList.item(i).getTextContent().trim();
				if (limitTimeValue.contains("endDate")) {
					int index = limitTimeValue.indexOf("\"deals\" : [");
					try {
						if (index >= 0) {
							limitTimeValue = limitTimeValue.substring(index); // 得到json字符串
							limitTimeValue = Formatter.getJson(limitTimeValue);
							if (!"".equals(limitTimeValue)) {
								JSONObject jsonObj = JSONObject.parseObject(limitTimeValue);
								endDate = jsonObj.getString("endDate");
								long endDateByDouble = Long.parseLong(endDate) * 1000;
								endDate = String.valueOf(endDateByDouble);
								if (endDate != null || !"".equals(endDate)) {
									this.product.setLimitTime(endDate);
								} else {
									this.product.setDiscountPrice("0");
									this.product.setLimitTime("0");
								}
							}
						}
					} catch (Exception e) {
						this.product.setDiscountPrice("0");
						this.product.setLimitTime("0");
						LOG.info(url + "-->setLimitTime:" + e);
					}
				}
			}
		}
	}

	/**
	 * 秒杀价格
	 */
	@Override
	public void setDiscountPrice() {
		double discountPriceByDouble = 0.00;
		NodeList discountPriceNodeList = TemplateUtil.getNodeList(root, discountPriceXpath);
		if (discountPriceNodeList.getLength() > 0) {
			for (int i = 0; i < discountPriceNodeList.getLength(); i++) {
				String discountPriceValue = discountPriceNodeList.item(i).getTextContent().trim();
				if (discountPriceValue.contains("dealPrice")) {
					int index = discountPriceValue.indexOf("\"dealPrice\" : {");
					if (index >= 0) {
						discountPriceValue = discountPriceValue.substring(index); // 得到json字符串
						discountPriceValue = Formatter.getJson(discountPriceValue);
						if (!"".equals(discountPriceValue)) {
							JSONObject jsonObj = JSONObject.parseObject(discountPriceValue);
							if (jsonObj != null) {
								String discountPrice = jsonObj.getString("price");
								if (discountPrice != null || !"".equals(discountPrice)) {
									discountPriceByDouble = Double.parseDouble(discountPrice);
									discountPrice = String.valueOf(discountPriceByDouble);
									this.product.setDiscountPrice(discountPrice);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 活动销售价格的折扣 电商的当前打折销售价格除以市场价格后，精确到小数点后两位
	 */
	@Override
	public void setD1ratio() {
		String d1ratio = "";
		double d1ratioByDouble = 0.00;
		String dicountPrice = this.product.getDiscountPrice();
		String marketPrice = this.product.getMaketPrice();
		double curPrice = StrUtil.isEmpty(dicountPrice) ? 0 : Double.parseDouble(dicountPrice);
		double curMarketPrice = StrUtil.isEmpty(marketPrice) ? 0 : Double.parseDouble(marketPrice);
		if (curPrice > 0 && curMarketPrice > curPrice) {// 只保留小数点后两位
			d1ratioByDouble = curPrice / curMarketPrice;
		}
		d1ratioByDouble = PriceTool.getdRatio(d1ratioByDouble);
		if (d1ratioByDouble > 0) {
			System.out.println(d1ratioByDouble);
			d1ratio = String.valueOf(d1ratioByDouble);
			if (!StrUtil.isEmpty(d1ratio)) {
				this.product.setD1ratio(d1ratio);
				return;
			}
		}
		this.product.setD1ratio("");

	}

	/**
	 * 当前销售价格的折扣=电商的当前销售价格除以市场价格后，精确到小数点后两位。
	 */
	@Override
	public void setD2ratio() {
		String d2ratio = "";
		double d2ratioByDouble = 0.00;
		String discountPriceXpth = "//SPAN[@id='youSaveValue']" + " | //TR[@class='youSavePriceRow']/TD[2]/SPAN";
		String marketPrice = this.product.getMaketPrice();
		double curMarketPrice = StrUtil.isEmpty(marketPrice) ? 0 : Double.parseDouble(marketPrice);
		if (curMarketPrice > 0) {
			String savePrice = "";
			Node savePriceNode = TemplateUtil.getNode(root, discountPriceXpth);
			if (savePriceNode != null) {
				savePrice = savePriceNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();
				String[] savePriceArry = savePrice.split("\\(");
				if (savePriceArry.length >= 2) {
					// 当前销售价格的折扣
					d2ratio = trimPrice(savePriceArry[1]).replaceAll("[折)(]", "");
					if (d2ratio != null || !"".equals(d2ratio)) {
						d2ratioByDouble = Double.parseDouble(d2ratio) / 10;
						d2ratioByDouble = PriceTool.getdRatio(d2ratioByDouble);
						d2ratio = String.valueOf(d2ratioByDouble);
						this.product.setD2ratio(d2ratio);
						return;
					}
				}
			}
		}
		this.product.setD2ratio("");
	}

	/**
	 * 亚马逊评论内容分按有用程度排序和按时间排序，页面每种方式最多显示10条数据。 1-10 解析有用程度的评论； 11-20 解析时间排序的评论内容
	 */

	@Override
	public void setCommentList() {
		String id[] = new String[20];
		String name[] = new String[20];
		String content[] = new String[20];
		// 1-10
		for (int i = 1; i <= 10; i++) {
			id[i - 1] = "//DIV[@id='revMHRL']/DIV[" + i + "]/@id";
			name[i - 1] = "//DIV[@id='revMHRL']/DIV[" + i + "]/DIV/SPAN/SPAN[@class='txtsmall']/A";
			content[i - 1] = "//DIV[@id='revMHRL']/DIV[" + i + "]/DIV/DIV[@class='drkgry']";
		}
		// 11-20
		for (int i = 1; i <= 10; i++) {
			id[10 + i - 1] = "//DIV[@id='revMRRL']/A[" + i + "]/DIV[1]/@id";
			name[10 + i - 1] = "//DIV[@id='revMRRL']/A[" + i + "]//descendant::DIV[@class='clearboth mt3 pbl']/SPAN";
			content[10 + i - 1] = "//DIV[@id='revMRRL']/A[" + i
					+ "]//descendant::DIV[@class='reviewText']/DIV[@class='drkgry']";

		}

		try {
			List<Comment> clist = new ArrayList<Comment>();
			for (int i = 0; i < 20; i++) {
				Comment c = getComment(id[i], name[i], content[i]);
				if (c != null)
					clist.add(c);
			}

			product.setClist(clist);

		} catch (Exception e) {
		}
	}

	private Comment getComment(String idXpath, String nameXpath, String contentXpath) {
		Node idNode = TemplateUtil.getNode(root, idXpath);
		Node nameNode = TemplateUtil.getNode(root, nameXpath);
		Node contentNode = TemplateUtil.getNode(root, contentXpath);
		if (idNode == null || nameNode == null || contentNode == null) {
			return null;
		}

		String cid = idNode.getTextContent();
		cid = cid.substring(cid.lastIndexOf("-") + 1);
		String name = nameNode.getTextContent().replaceAll("[\\s]{1,}", "");
		String content = contentNode.getTextContent().replaceAll("[\\s]{1,}", "");

		if (StringUtils.isNotEmpty(cid) && StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(content)) {
			String c = name + "[xm99]" + content;
			Comment cm = new Comment();
			cm.setCommentId(cid);
			cm.setCommentContent(c);
			return cm;
		}

		return null;
	}

	@Override
	public void setOrgPic() {
		Node node = TemplateUtil.getNode(root, imgXpath);
		if (node != null) {
			String img = imgPrefix + node.getNodeValue();
			if ("http://ec8.images-amazon.com/images/G/28/ui/loadIndicators/loading-large._V201045688_.gif".equals(img)) {
				this.product.setIsCargo(0);
			}
			if (img != null && img.contains("\\")) {
				img = img.replace("\\", "/");
			}
			try {
				this.product.setOrgPic(img);
			} catch (Exception ex) {
				LOG.error("图片解析错误");
			}
		}
	}

	// *******************************************************************************************************************

	public static void main(String[] args) throws Exception {
		// String url = "http://www.amazon.cn/dp/B00F4Q6Y50";
		// HtmlParser parser = new HtmlParser();
		// Product p = parser.testParse(url);
		// System.out.println(p);

		// ParserAmazonNew amazon=new ParserAmazonNew();
		// // testInvalidUrlFormat(amazon);
		// try {
		// amazon.test1();
		// } catch (ProductIDIsNullException e) {
		// }

		String url = "http://www.amazon.cn/gp/aw/r.html/ref=me_amb_60578652_19/480-8046302-4770468?aid=aw_Categories&apid=60578652&arc=1201&arid=1BWMH67M9AMZSJQV2SME&asn=center-10&br=746776051&sn=Cosmetics";
		String aa = getOfHttpURLConnectionByAmazon(url, "utf-8").toString();
		System.out.println(aa);
		// System.out.println((char)32654 + "" + (char)23481);

	}

	/**
	 * get方式发送请求数据 根据京东商品评论请求参数特殊
	 * 
	 * @param url
	 * @param charset
	 * @return StringBuilder
	 */
	public static StringBuilder getOfHttpURLConnectionByAmazon(String url, String charset) {
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
			uc.setRequestProperty("Content-Type", "image/jpeg");

			uc.setRequestProperty("accept", "image/webp,*/*;q=0.8");
			uc.setRequestProperty(
					"user-agent",
					"Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");

			uc.setRequestProperty("accept-language", "gzip,deflate,sdch");
			uc.setRequestProperty("Accept-Encoding", "zh-CN,zh;q=0.8");

			uc.setRequestProperty("Referer", url);

			String line = null;

			br = new BufferedReader(new InputStreamReader(uc.getInputStream(), charset));

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (Exception e) {
			if (uc != null) {
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

	public void test1() throws Exception, ProductIDIsNullException {
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\amazon.html"), "utf-8");
		ParserAmazonNew test = new ParserAmazonNew(root);
		test.product = new OriProduct();
		// test.setCommentOne();
		// System.out.println("商品评论One======>:"+ test.product.getCommentOne().getCommentId()+">>>>"
		// + test.product.getCommentOne().getCommentContent());
		//
		// test.setCommentTwo();
		// System.out.println("商品评论Two======>:"+ test.product.getCommentTwo().getCommentId()+">>>>"
		// + test.product.getCommentTwo().getCommentContent());
		//
		// test.setCommentThree();
		// System.out.println("商品评论Three======>:"+
		// test.product.getCommentThree().getCommentId()+">>>>" +
		// test.product.getCommentThree().getCommentContent());
		//
		// test.setCommentFour();
		// System.out.println("商品评论Four======>:"+
		// test.product.getCommentFour().getCommentId()+">>>>" +
		// test.product.getCommentFour().getCommentContent());
		//
		// test.setCommentFive();
		// System.out.println("商品评论Five======>:"+
		// test.product.getCommentFive().getCommentId()+">>>>" +
		// test.product.getCommentFive().getCommentContent());
		//
		// test.setCommentSix();
		// System.out.println("商品评论Six======>:"+ test.product.getCommentSix().getCommentId()+">>>>"
		// + test.product.getCommentSix().getCommentContent());
		//
		// test.setCommentSeven();
		// System.out.println("商品评论Seven======>:"+
		// test.product.getCommentSeven().getCommentId()+">>>>" +
		// test.product.getCommentSeven().getCommentContent());
		//
		// test.setCommentEight();
		// System.out.println("商品评论Eight======>:"+
		// test.product.getCommentEight().getCommentId()+">>>>" +
		// test.product.getCommentEight().getCommentContent());
		//
		// test.setCommentNine();
		// System.out.println("商品评论Nine======>:"+
		// test.product.getCommentNine().getCommentId()+">>>>" +
		// test.product.getCommentNine().getCommentContent());
		//
		// test.setCommentTen();
		// System.out.println("商品评论Ten======>:"+ test.product.getCommentTen().getCommentId()+">>>>"
		// + test.product.getCommentTen().getCommentContent());
		//
		// test.setCommentEleven();
		// System.out.println("商品评论Eleven======>:"+
		// test.product.getCommentEleven().getCommentId()+">>>>" +
		// test.product.getCommentEleven().getCommentContent());
		//
		// test.setCommentTwelve();
		// System.out.println("商品评论Twelve======>:"+
		// test.product.getCommentTwelve().getCommentId()+">>>>" +
		// test.product.getCommentTwelve().getCommentContent());
		//
		// test.setCommentThirteen();
		// System.out.println("商品评论Thirteen======>:"+
		// test.product.getCommentThirteen().getCommentId()+">>>>" +
		// test.product.getCommentThirteen().getCommentContent());
		//
		// test.setCommentFourteen();
		// System.out.println("商品评论Fourteen======>:"+
		// test.product.getCommentFourteen().getCommentId()+">>>>" +
		// test.product.getCommentFourteen().getCommentContent());
		//
		// test.setCommentFifteen();
		// System.out.println("商品评论Fifteen======>:"+
		// test.product.getCommentFifteen().getCommentId()+">>>>" +
		// test.product.getCommentFifteen().getCommentContent());
		//
		// test.setCommentSixteen();
		// System.out.println("商品评论Sisteen======>:"+
		// test.product.getCommentSixteen().getCommentId()+">>>>" +
		// test.product.getCommentSixteen().getCommentContent());
		//
		// test.setCommentSeventeen();
		// System.out.println("商品评论Seventeen======>:"+
		// test.product.getCommentSeventeen().getCommentId()+">>>>" +
		// test.product.getCommentSeventeen().getCommentContent());
		//
		// test.setCommentEighteen();
		// System.out.println("商品评论Eighteen======>:"+
		// test.product.getCommentEighteen().getCommentId()+">>>>" +
		// test.product.getCommentEighteen().getCommentContent());
		//
		// test.setCommentNineteen();
		// System.out.println("商品评论Nineteen======>:"+
		// test.product.getCommentNineteen().getCommentId()+">>>>" +
		// test.product.getCommentNineteen().getCommentContent());
		//
		// test.setCommentTwenty();
		// System.out.println("商品评论Twenty======>:"+
		// test.product.getCommentTwenty().getCommentId()+">>>>" +
		// test.product.getCommentTwenty().getCommentContent());

		// test.setId();
		// System.out.println("id=====>: " + test.product.getId() + ">>>>"
		// + test.product.getPid());
		//
		// test.setBrand();
		// System.out.println("品牌=====>：" + test.product.getBrand());
		//
		// test.setProductName();
		// System.out.println("商品名称======>:" + test.product.getProductName());
		//
		// test.setShortName();
		// System.out.println("商品短名称======>:" + test.product.getShortName());
		//
		// test.setshortNameDetail();
		// System.out.println("第二种商品短名称======>:" + test.product.getShortNameDetail());
		//
		// test.setClassic();
		// System.out.println("导航=====>: " + test.product.getClassic());
		//
		// test.setOrgPic();
		// System.out.println("图片=====>：" + test.product.getOrgPic());
		//
		// test.setPrice();
		// System.out.println("价钱=====> ：" + test.product.getPrice());
		//
		// test.setCategory();
		// System.out.println("中文原始分类====》:" + test.product.getCategory());
		//
		// test.setOriCatCode();
		// System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
		//
		// test.setIsCargo();
		// System.out.println("是否有货====》:" + test.product.getIsCargo());
		//
		// test.setMparams();
		// System.out.println("mp内容=====> ：" + test.product.getMparams());
		//
		// test.setTitle();
		// System.out.println("标题=====>：" + test.product.getTitle());
		//
		// test.setKeyword();
		// System.out.println("关键字=====>: " + test.product.getKeyword());
		//
		// test.setContents();
		// System.out.println("content=====>：" + test.product.getContents());

		// test.setPrice();
		// System.out.println("当前价格=====>:" + test.product.getPrice());
		//
		// test.setMaketPrice();
		// System.out.println("市场价格=====>:" + test.product.getMaketPrice());
		//
		// test.setLimitTime();
		// System.out.println("折扣活动截止时间=====>:" + test.product.getLimitTime());
		//
		// test.setDiscountPrice();
		// System.out.println("活动价格=====>:" + test.product.getDiscountPrice());
		//
		// test.setD1ratio();
		// System.out.println("活动销售价格的折扣=====>:" + test.product.getD1ratio());
		//
		// test.setD2ratio();
		// System.out.println("当前销售价格的折扣=====>:" + test.product.getD2ratio());

		// test.setTotalComment();
		// System.out.println("总评论数=====>:" + test.product.getTotalComment());
		//
		// test.setCommentStar();
		// System.out.println("评论星级数=====>:" + test.product.getCommentStar());
		//
		// test.setCommentPercent();
		// System.out.println("评论百分数=====>:" + test.product.getCommentPercent());

		// URL url;
		// try {
		// url = new URL("http://www.amazon.cn/dp/B00BBRJMY6");
		// List<String> list = test.nextUrlList(url);
		// for (int i = 0; i < list.size(); i++) {
		// System.out.println(list.get(i));
		// }
		// System.out.println(list.size());
		// String aa=test.nextUrl(url);
		// System.out.println("下一页===》"+aa);
		// } catch (MalformedURLException e) {
		// }
	}
}