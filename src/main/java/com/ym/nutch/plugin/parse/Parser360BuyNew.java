package com.ym.nutch.plugin.parse;

import java.io.File;
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
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.parse.template.PriceTool;
import com.ym.nutch.plugin.util.Constant;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

public class Parser360BuyNew extends ParserParent {

	public static final Logger	LOG			= LoggerFactory.getLogger(Parser360BuyNew.class);

	// PC端商品url规则 http://item.jd.com/1034292.html
	private Pattern				itemFlag1	= Pattern.compile("http\\://item\\.jd\\.com/([0-9]+).html.*?");
	// 手机版商品url规则： http://m.jd.com/product/647812.html
	private Pattern				itemFlag2	= Pattern.compile("http\\://m\\.jd\\.com/product/([0-9]+).html.*?");

	public Parser360BuyNew() {
		super();
		init();
	}

	public Parser360BuyNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	String									jsonXpath			= "//SCRIPT";
	String									contentDetailXpath1	= "//DIV[@class='mcc']";
	private static Map<String, String>		map					= new HashMap<String, String>();
	private static Map<String, JSONArray>	JSONArrayMap		= new HashMap<String, JSONArray>();

	public void init() {
		sellerCode = "1005";
		seller = "京东商城";
		productNameXpath = "//UL[@class='detail-list']//descendant::LI[contains(text(),'商品名称')]/@title"
				+ "| //DIV[@class='p-name']";
		imgXpath = "//DIV[@class='jqzoom']/IMG/@src" + " | //DIV[@id='spec-n1']/IMG/@src";
		keywordXpath = "//META[@name='keywords']/@content";
		brandXpath = "//UL[@class='detail-list']//descendant::LI[3]";
		classicXpath = "//DIV[@class='breadcrumb']" + " | //DIV[@class='crumb']";
		mparasXpath = "//DIV[@id='product-detail-2']//descendant::TR[TD]";
		contentDetailXpath = "//DIV[@id='product-detail-1']";
		nextPageXpath = "//A[@class='next']/@href";
		shortNameXpath = "//DIV[@class='breadcrumb']/SPAN/A[last()]";
		marketPriceXpath = "//DIV[@class='clearfix']/SCRIPT[1]";
		categoryXpath = "//DIV[@class='breadcrumb']//descendant::A[position()<3]";
		keywordsFilter.add("报价");
		keywordsFilter.add("京东");
		keywordsFilter.add("京东商城");
		titleFilter.add("【.*?】|京东商城|网上购物|行情|报价");
		productFlag = itemFlag1;

		throughList.add(itemFlag1);
		throughList.add(itemFlag2);

		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
	}

	public void parseInit() {
		String resultJson = "";
		String yinxiangJson = "";// 音像类
		String value = "";
		int index = 0;
		NodeList nodeList = TemplateUtil.getNodeList(root, jsonXpath);
		if (nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node != null) {
					value = node.getTextContent().replaceAll("[\\s]{3,}", " ");
					if (value != null && !"".equals(value)) {
						index = value.indexOf("window.pageConfig");
						if (index >= 0) {
							resultJson = value.substring(index);
							resultJson = Formatter.getJson(resultJson);
							break;
						}
						index = value.indexOf("wareinfo");// 音像类
						if (index >= 0) {
							yinxiangJson = value.substring(index);
							yinxiangJson = Formatter.getJson(yinxiangJson);
							break;
						}
					}
				}
			}
			JSONObject jsonObj = null;
			if (!StrUtil.isEmpty(resultJson)) {
				try {
					jsonObj = JSONObject.parseObject(resultJson);
					if (resultJson.indexOf("product") >= 0) {
						JSONObject productObject = jsonObj.getJSONObject("product");
						map.put("skuid", productObject.getString("skuid"));
						map.put("skuidkey", productObject.getString("skuidkey"));
						map.put("cat", productObject.getString("cat"));
					}
					return;
				} catch (Exception e) {
					LOG.error("parse json error " + e.toString());
				}
			}
			// 音像制品类商品，提取商品基本信息，二次请求时需要引用
			if (!StrUtil.isEmpty(yinxiangJson)) {
				try {
					jsonObj = JSONObject.parseObject(yinxiangJson);
					map.put("skuid", jsonObj.getString("pid"));
					map.put("skuidkey", jsonObj.getString("sid"));
					// 商品的分类码信息通过页面更新纠错链接提取
					// http://market.jd.com/jdvote/skucheck.aspx?skuid=880730&cid1=9987&cid2=653&cid3=655
					String jdvoteXpath = "//A[contains(@href,'jdvote/skucheck')]/@href";
					Node priceNode = TemplateUtil.getNode(root, jdvoteXpath);
					if (priceNode != null) {
						String href = priceNode.getTextContent().trim();
						Map<String, String> pms = new TemplateUtil().formMap(href.split("&"), "=");
						map.put("cat", pms.get("cid1") + "," + pms.get("cid2") + "," + pms.get("cid3"));
					}
					return;
				} catch (Exception e) {
					LOG.error("parse json error " + e.toString());
				}
			}
			LOG.info("parseInit Json Is Error,url=" + url);
			map.put("resultJson", "false");
		}

	}

	@Override
	public void setId() throws ProductIDIsNullException {
		parseInit();
		String pid = null;
		for (int i = 0; i < productUrlList.size(); i++) {
			Matcher ma1 = productUrlList.get(i).matcher(url.toLowerCase());
			if (ma1.find()) {
				pid = ma1.group(1);
			}
			if (pid == null || pid.isEmpty()) {
				continue;
			} else {
				String id = sellerCode + pid;
				product.setPid(id);
				product.setOpid(pid);
			}
		}
	}

	@Override
	public void setPrice() throws ProductPriceIsNullException {
		try {
			String price = "";
			String resultJson = "";
			String productId = map.get("skuid");
			if (!StrUtil.isEmpty(productId)) {
				String priceUrl = "http://p.3.cn/prices/get?skuid=J_" + productId
						+ "&type=1&callback=changeImgPrice2Num";
				String value = RequestUtil.getOfHttpURLConnection(priceUrl, "gbk").toString();
				if (!StrUtil.isEmpty(value)) {
					value = value.replaceAll("[\\s]{2,}", " ");
					int beginIndex = value.indexOf("[") + 1;
					int endIndex = value.indexOf("]");
					if (beginIndex >= 0 && endIndex >= 0) {
						value = value.substring(beginIndex, endIndex);// 得到json字符串
						resultJson = Formatter.getJson(value);
					}
				}
				if (!StrUtil.isEmpty(resultJson)) {
					JSONObject productObject = JSONObject.parseObject(value);
					price = productObject.getString("p").trim();
					if (price == null || price.isEmpty()) {
						throw new ProductPriceIsNullException(url
								+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
					}
					if ("-1".equals(price) || "-1.00".equals(price)) {
						this.product.setPrice("0");
					} else {
						this.product.setPrice(price);
					}
				}
			} else {
				throw new ProductPriceIsNullException(url
						+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
			}
		} catch (Exception ex) {
			throw new ProductPriceIsNullException(url + ex);
		}
	}

	@Override
	public void setMaketPrice() {
		String marketPrice = "";
		if (!marketPriceXpath.isEmpty()) {
			Node marketPriceNode = TemplateUtil.getNode(root, marketPriceXpath);
			if (marketPriceNode != null) {
				marketPrice = marketPriceNode.getTextContent().trim();
				marketPrice = marketPrice.substring(marketPrice.indexOf("Price=") + 6, marketPrice.indexOf(";"));
				marketPrice = marketPrice.replaceAll("'", "");
				if (!StrUtil.isEmpty(marketPrice)) {
					if (marketPrice.contains("marketPrice")) {
						marketPrice = "";
					}
					marketPrice = trimPrice(marketPrice);
					this.product.setMaketPrice(marketPrice);
					return;
				}
			} else {
				Node marketPriceNode2 = TemplateUtil.getNode(root, "//LI[@id='summary-market']/DIV[@class='dd']/DEL");
				if (marketPriceNode2 != null) {
					marketPrice = marketPriceNode2.getTextContent().trim();
					if (!StrUtil.isEmpty(marketPrice)) {
						marketPrice = trimPrice(marketPrice);
						this.product.setMaketPrice(marketPrice);
						return;
					}
				}
			}
		}
		this.product.setMaketPrice("0");
	}

	@Override
	public void setIsCargo() {
		String cat = map.get("cat").replaceAll("[\\[\\]]", "");
		if (cat != null && !"".equals(cat)) {
			String[] sortIdArray = cat.split(",");
			if (sortIdArray.length == 3) {
				String sortid1 = sortIdArray[0];
				String sortid2 = sortIdArray[1];
				String sortid3 = sortIdArray[2];
				String skuidkey = map.get("skuidkey");
				if (skuidkey != null && !"".equals(skuidkey)) {
					String setIsCargoUrl = "http://st.3.cn/gds.html?callback=getStockCallback&skuid=" + skuidkey
							+ "&provinceid=1&cityid=72&areaid=4137&townid=0&sortid1=" + sortid1 + "&sortid2=" + sortid2
							+ "&sortid3=" + sortid3 + "&cd=1_1_1";
					String value = RequestUtil.getOfHttpURLConnection(setIsCargoUrl, "gbk").toString();
					if (value != null && !"".equals(value)) {
						int index = value.indexOf("getStockCallback");
						if (index >= 0) {
							value = value.substring(index); // 得到json字符串
							value = Formatter.getJson(value);
							if (!StrUtil.isEmpty(value)) {
								JSONObject jsonObj = JSONObject.parseObject(value);
								JSONObject productObject = jsonObj.getJSONObject("stock");
								String name = productObject.getString("StockStateName");
								if (name != null && !"".equals(name) && "有货".equals(name)) {
									product.setIsCargo(1);
									return;
								}
							}
						}
					}
				}
			}
		}
		product.setIsCargo(0);
	}

	/**
	 * 获取列表页面
	 * 
	 * @Override public List<imageProduct> nextUrlList(URL baseUrl,String content) {
	 *           List<imageProduct> urlList = new ArrayList<imageProduct>(); Set<imageProduct>
	 *           urlSet = new LinkedHashSet<imageProduct>(); URL urls = null; try { Document doc =
	 *           Jsoup.parse(content); Elements elements=doc.getElementsByTag("A");
	 *           if(elements.size() > 0){ for (int i = 0; i < elements.size(); i++) { Element
	 *           urlListEle = elements.get(i); String productUrl=urlListEle.attr("href"); String
	 *           smallPic=urlListEle.getElementsByTag("img").attr("src");
	 *           if(smallPic.contains("no_90_90.png")){
	 *           smallPic=urlListEle.getElementsByTag("img").attr("imgdata"); }else
	 *           if(smallPic.contains("no_308_108.png") || smallPic.contains("no_100_100.png")){
	 *           smallPic=urlListEle.getElementsByTag("img").attr("imgsrc"); }
	 *           if(!smallPic.startsWith("http://")) continue; try{ urls =
	 *           URLUtil.resolveURL(baseUrl, productUrl); if(!StrUtil.isEmpty(urls.toString()) &&
	 *           !StrUtil.isEmpty(smallPic)){ imageProduct image=new
	 *           imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){
	 *           LOG.info(this.getClass().getSimpleName
	 *           ()+"  resolveURL Error! url: "+baseUrl.toString()); } } } } catch (Exception e) {
	 *           LOG
	 *           .info(this.getClass().getSimpleName()+"  Parser Error! url: "+baseUrl.toString());
	 *           } urlList.addAll(urlSet); return urlList; }
	 */
	protected String dealShop(String mparams, OriProduct product) {
		String contents = product.getContents();
		contents = contents.replaceAll("\\n", "xm99").replaceAll("：", ":");
		String shopName = "";
		int index = contents.indexOf("店铺:");
		if (index >= 0) {
			index = index + "店铺:xm99".length();
			int index2 = contents.indexOf("xm99", index);
			shopName = contents.substring(index, index2);
		}
		return shopName;
	}

	protected String dealBrand(String mparams, OriProduct product) {
		String contents = product.getContents();
		contents = contents.replaceAll("\\n", "xm99").replaceAll("：", ":");
		String brand = "";
		int index = contents.indexOf("品牌:");
		if (index >= 0) {
			index = index + "品牌:xm99".length();
			int index2 = contents.indexOf("xm99", index);
			brand = contents.substring(index, index2);
		}
		if (brand == null || brand.equals("")) {
			return super.dealBrand(mparams, product);
		} else {
			return brand;
		}
	}

	@Override
	public void setClassic() {
		classFilter.add("首页");
		String value = "";
		Node node_location = TemplateUtil.getNode(root, classicXpath);
		if (node_location != null) {
			value = node_location.getTextContent();
			if (classFilter.size() > 0) {
				for (String filterString : classFilter) {
					value = value.replaceAll(filterString + "[\\s| ]{0,}>{0,}", "");
				}
			}
		}
		value = value.replaceAll(">", Constant.XMTAG).replaceAll("[\\s]{1,}", "").trim();
		product.setClassic(value);
	}

	@Override
	public void setContents() {
		StringBuffer sb = new StringBuffer();
		NodeList nodeList = TemplateUtil.getNodeList(root, contentDetailXpath);
		if (nodeList.getLength() == 0) {
			NodeList nodeList1 = TemplateUtil.getNodeList(root, contentDetailXpath1);
			for (int i = 0; i < nodeList1.getLength(); i++) {
				Node node1 = nodeList1.item(i);
				TemplateUtil.getTextHelper(sb, node1);
			}
		} else {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				TemplateUtil.getTextHelper(sb, node);
			}
		}
		String contents = sb.toString().replaceAll("[\\s]{1,}", "").trim();
		if (!StrUtil.isEmpty(contents)) {
			product.setContents(contents);
		}
	}

	/**
	 * 情况一：常规商品，http://list.jd.com/9987-653-655.html,cat: [9987,653,655]
	 * 情况二：电子书刊类，音像类，商品列表页:http://www.jd.com/allSort.aspx此类商品并没有分类级别，如: http://e.jd.com/ebook.html,
	 * http://e.jd.com/30123604.html: <div class="infma" id="correction">纠错信息：如果您发现商品信息不准确，欢迎<a
	 * target="_blank" href="http://club.jd.com/jdvote/skucheckbook.aspx?skuid=30123604&
	 * cid1=5272&cid2=5278&cid3=10838">纠错</a>。</div>
	 * 可以截取cid1为一级分类，cid2为二级分类，cid3为三级分类，但存在一个问题，在入品牌库时并没有这些相应的级别分类
	 */
	@Override
	public void setOriCatCode() {
		parseInit();
		String cateGory = "";
		String cat = map.get("cat").replaceAll("[\\[\\]]", "");
		if (cat != null && !"".equals(cat)) {
			cateGory = cat.replaceAll(",", "#");
			if (cateGory != null && !"".equals(cateGory)) {
				product.setOriCatCode(cateGory);
			} else {
				LOG.info("该商品没有原始分类！！！" + url);
			}
		} else {
			// 解析含有‘纠错’的A标签类有分类信息。
			String cateGoryXpth = "//DIV[@id='correction']/A[contains(text(),'纠错') | contains(text(),'欢迎更新')]/@href";
			Node cateGoryNode = TemplateUtil.getNode(root, cateGoryXpth);
			StringBuffer sb = new StringBuffer();
			if (cateGoryNode != null) {
				String oriCatCodeUrl = cateGoryNode.getTextContent().trim();
				oriCatCodeUrl = oriCatCodeUrl.substring(oriCatCodeUrl.indexOf("cid1"), oriCatCodeUrl.length());
				String[] cateCodeArray = oriCatCodeUrl.split("&");
				if (cateCodeArray.length > 0) {
					for (int i = 0; i < cateCodeArray.length; i++) {
						if (i != 0) {
							sb.append("#");
						}
						String catNum = cateCodeArray[i].replaceAll("[cid" + (i + 1) + "=]", "");
						sb.append(catNum);
					}
					cateGory = sb.toString();
					product.setOriCatCode(cateGory);
				}
			} else {
				LOG.info("The OriCatCode Is Null-->" + url);
			}
		}
	}

	/**
	 * 短名称
	 */
	@Override
	public void setShortName() {
		if (!"".equals(shortNameXpath)) {
			Node shortNameNode = TemplateUtil.getNode(root, shortNameXpath);
			if (shortNameNode != null) {
				String shortName = shortNameNode.getTextContent().trim();
				product.setShortName(shortName);
				product.setShortNameDetail(shortName);
			}
		} else {
			LOG.info(url + "-->Error---The shortName Is Null !");
		}
	}

	@Override
	public void setCategory() {
		String value = "";
		StringBuffer cateGoryBuffer = new StringBuffer();
		NodeList node_location = TemplateUtil.getNodeList(root, categoryXpath);
		if (node_location.getLength() > 0) {
			for (int i = 0; i < node_location.getLength(); i++) {
				if (i != 0) {
					cateGoryBuffer.append("[xm99]");
				}
				value = node_location.item(i).getTextContent().replaceAll("[\\s]{1,}", "");
				cateGoryBuffer.append(value);
			}
		}
		if (cateGoryBuffer.length() > 0) {
			value = cateGoryBuffer.toString();
		}
		product.setCategory(value);
	}

	/**
	 * 二次请求，直降价格和活动截止时间
	 */
	public static void activitiesPrice() {
		String productId = map.get("skuid");
		if (productId != null && !"".equals(productId)) {
			String limitTimeUrl = "http://jprice.360buy.com/pageadword/" + productId + "-1-1-1_72_4137_0.html";
			String value = RequestUtil.getOfHttpURLConnection(limitTimeUrl, "utf-8").toString();
			if (value != null && !"".equals(value)) {
				int index = value.indexOf("promotionInfoList");
				if (index >= 0) {
					value = value.substring(index); // 得到json字符串
					try {
						value = Formatter.getJson(value);
						if (!"".equals(value)) {
							JSONObject jsonObj = JSONObject.parseObject(value);
							map.put("promoEndTime", jsonObj.getString("promoEndTime"));
							map.put("discount", jsonObj.getString("discount"));
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}

	/**
	 * 折扣活动截止时间,只限活动商品
	 */
	@Override
	public void setLimitTime() {
		activitiesPrice();
		String promoEndTime = map.get("promoEndTime");
		if (!StrUtil.isEmpty(promoEndTime)) {
			this.product.setLimitTime(promoEndTime);
			long limitTimeByLong = Long.parseLong(promoEndTime);
			if (limitTimeByLong > 0 && promoEndTime.length() == 13) { // 如果判断该商品属于时限活动商品，那么将重新设置当前价格和活动售价
				// 将当前显示的京东价格设置为活动售价
				String price = this.product.getPrice();
				if (!"".equals(price) && !"0".equals(price)) {
					this.product.setDiscountPrice(price);
					return;
				}
			}
		}
		this.product.setDiscountPrice("0");
		this.product.setLimitTime("0");
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
		d1ratio = String.valueOf(d1ratioByDouble);
		this.product.setD1ratio(d1ratio);
	}

	/**
	 * 当前销售价格的折扣=电商的当前销售价格除以市场价格后，精确到小数点后两位。
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

	@Override
	public void setTotalComment() {
		String commentCount = "";
		String averageScore = "";
		long jsonp = System.currentTimeMillis();
		try {
			String pid = this.product.getPid();
			if (pid != null && !"".equals(pid)) {
				String totalCommentUrl = "http://club.jd.com/productpage/p-" + pid + "-s-0-t-3-p-0.html?callback=jsonp"
						+ jsonp + "&_=" + jsonp;
				String commentValue = RequestUtil.getOfHttpURLConnectionByJDComments(totalCommentUrl, "GBK").toString();
				if (!StrUtil.isEmpty(commentValue)) {
					commentValue = Formatter.getJson(commentValue);
					if (!StrUtil.isEmpty(commentValue)) {
						JSONObject jsonObj = JSONObject.parseObject(commentValue);
						String productCommentSummary = jsonObj.getString("productCommentSummary");
						if (!StrUtil.isEmpty(productCommentSummary)) {
							JSONObject jsonObjBycommentsCountAndScore = JSONObject.parseObject(productCommentSummary);
							commentCount = jsonObjBycommentsCountAndScore.getString("commentCount");
							averageScore = jsonObjBycommentsCountAndScore.getString("averageScore");
						}

						int cc = null == commentCount ? 0 : StringUtils.isEmpty(commentCount) ? 0 : Integer
								.parseInt(commentCount);
						this.product.setTotalComment(cc);
						if (!StrUtil.isEmpty(averageScore)) {
							map.put("averageScore", averageScore);
						}
						String commentsJson = jsonObj.getString("comments");
						JSONArray jsonArray = JSONArray.parseArray(commentsJson);
						if (jsonArray.size() > 0) {
							JSONArrayMap.put("jsonArray", jsonArray);
						}
						return;
					}
				}
			}
		} catch (Exception e) {
			LOG.info(url + "-->" + this.getClass().getSimpleName() + "The setTotalComment Parse Is Error!");
		}
	}

	@Override
	public void setCommentStar() {
		String commentStar = map.get("averageScore");
		if (!StrUtil.isEmpty(commentStar)) {
			this.product.setCommentStar(commentStar);
		} else {
			this.product.setCommentStar("0");
		}
	}

	@Override
	public void setCommentList() {
		// 初始化评论内容文本
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
							String name = StringUtils.trimToEmpty(o.getString("nickname"));
							String content = StringUtils.trimToEmpty(o.getString("content"));
							content = StringUtils.isEmpty(content) ? "" : content.replaceAll("[\\s]{2,}", "");
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

	// *******************************************************************************************************************

	public static void main(String[] args) throws Exception, ProductIDIsNullException {
		// String url = "http://item.jd.com/1003273808.html";
		// HtmlParser parser = new HtmlParser();
		// Product p = parser.testParse(url);
		// System.out.println(p);

		Parser360BuyNew test = new Parser360BuyNew();
		test.test3();
		// String url = test.formatUrl("http://sssitem.jd.com/647812.html");
		// System.out.println(url);

	}

	public void test3() throws Exception, ProductIDIsNullException {
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\jingdong1.html"), "gb2312");
		Parser360BuyNew test = new Parser360BuyNew(root);
		test.product = new OriProduct();
		test.setId();
		// System.out.println("id=====>:" + test.product.getId() + ">>>>"
		// + test.product.getPid());
		//
		// test.setProductName();
		// System.out.println("商品名称======>:" + test.product.getProductName());
		//
		// test.setClassic();
		// System.out.println("导航=====>:" + test.product.getClassic());
		//
		// test.setBrand();
		// System.out.println("品牌=====>:" + test.product.getBrand());
		//
		// test.setContents();
		// System.out.println("content=====>:" + test.product.getContents());
		//
		// test.setOrgPic();
		// System.out.println("图片=====>:" + test.product.getOrgPic());
		//
		// test.setMparams();
		// System.out.println("mp内容=====>:" + test.product.getMparams());
		//
		// test.setTitle();
		// System.out.println("标题=====>:" + test.product.getTitle());
		//
		// test.setKeyword();
		// System.out.println("关键字=====>:" + test.product.getKeyword());
		//
		// test.setIsCargo();
		// System.out.println("是否有货====》:" + test.product.getIsCargo());
		//
		// test.setCategory();
		// System.out.println("中文原始分类====》:" + test.product.getCategory());
		//
		// test.setOriCatCode();
		// System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
		//
		// test.setShortName();
		// System.out.println("商品短名称====》:" + test.product.getShortName());
		//
		// test.setshortNameDetail();
		// System.out.println("第二种商品短名称====》:" + test.product.getShortNameDetail());

		// test.setPrice();
		// System.out.println("当前价格=====>:" + test.product.getPrice());
		//
		// test.setMaketPrice();
		// System.out.println("市场价格=====>:" + test.product.getMaketPrice());
		//
		// test.setLimitTime();
		// System.out.println("折扣活动截止时间=====>:" + test.product.getLimitTime());
		//
		// System.out.println("活动价格=====>:" + test.product.getDiscountPrice());
		//
		// test.setD1ratio();
		// System.out.println("活动销售价格的折扣=====>:" + test.product.getD1ratio());
		//
		// test.setD2ratio();
		// System.out.println("当前销售价格的折扣=====>:" + test.product.getD2ratio());
		// System.out.println("当前价格=====>:" + test.product.getPrice());
		//
		// test.setTotalComment();
		// System.out.println("总评论数=====>:" + test.product.getTotalComment());
		//
		// test.setCommentStar();
		// System.out.println("评论星级数=====>:" + test.product.getCommentStar());
		//
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
		// URL url;
		// try {
		// url = new URL("http://m.jd.com/products/670-671-672.html");
		// List<imageProduct> list = test.nextUrlList(url);
		// for (int i = 0; i < list.size(); i++) {
		// System.out.println(list.get(i));
		// }
		// System.out.println(list.size());
		// } catch (MalformedURLException e) {
		// }

		// String
		// aa=test.formatSafeUrl("http://www.coo8.com/products/cat18000442-00-0-n-8-0-0-0-844048X6pooXX844042X15rf.html");
		// System.out.println(aa);

		// try {
		// System.out.println("==下一页="+ test.nextUrl(new URL(
		// "http://www.coo8.com/products/cat18000328.html")));
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// }

		// String url1 = "http://www.coo8.com/product/A0003186844.html";
		// boolean b = test.isProductPage(url1);
		// System.out.println("是不是产品页：" + b);

	}
}
