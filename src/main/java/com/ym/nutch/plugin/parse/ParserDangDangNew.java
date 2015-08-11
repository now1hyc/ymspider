package com.ym.nutch.plugin.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONObject;
import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductNameIsNullException;
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.parse.template.PriceTool;
import com.ym.nutch.plugin.util.Constant;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

public class ParserDangDangNew extends ParserParent {

	public static final Logger				LOG				= LoggerFactory.getLogger(ParserDangDangNew.class);

	// http://product.dangdang.com/product.aspx?product_id=1001251303
	// http://product.dangdang.com/1360800202.html
	// 商品页正则
	private Pattern							itemFlag1		= Pattern
																	.compile("http://product.dangdang.com/product.aspx\\?product_id=([\\d]+).*");					// group=1
																																									// for
																																									// pid
	private Pattern							itemFlag2		= Pattern
																	.compile("http://product.dangdang.com/([\\d]+).html.*");
	// touch手机端商品url：http://m.dangdang.com/touch/product.php?pid=60556677&sid=229413744d4ed0dd&recoref=category
	private Pattern							itemFlag3		= Pattern
																	.compile("http\\://m\\.dangdang\\.com/touch/product.php\\?pid\\=([0-9]+).*?");
	// 精简版手机端商品url：http://m.dangdang.com/product.php?pid=1323602601
	private Pattern							itemFlag4		= Pattern
																	.compile("http\\://m\\.dangdang\\.com/product\\.php\\?pid\\=([0-9]+).*?");

	String									idXpath			= "//SPAN[@id='pid_span']/@product_id";
	String									jsonXpath		= "//SCRIPT";
	String									paramXpath		= "//SPAN[@id='pid_span']";																				// 页面标记标签
	String									baseUrl			= "http://category.dangdang.com";																		// 为url加前缀
	// 20130923：图书类，商品参数信息提取[作者，出版社，页数，字数，包装等信息]
	private String							bookParamXpath	= "//DIV[@class='show_info']/UL[@name='Infodetail_pub']/LI/SPAN";
	// 20130823：图书类，商品品牌字段暂定位出版社信息
	private String							bookBrandXpath	= "//DIV[@class='show_info']/UL[@name='Infodetail_pub']/LI/SPAN[@class='c1' and SPAN[@class='ws1']]/A";

	Map<String, String>						map				= new HashMap<String, String>();
	Map<String, DocumentFragment>			mapByDocument	= new HashMap<String, DocumentFragment>();
	private static Map<String, Elements>	elementsMap		= new HashMap<String, Elements>();

	public ParserDangDangNew() {
		init();
	}

	public ParserDangDangNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	public void init() {
		sellerCode = "1008";
		seller = "当当网";
		filterStr = "当当网xm99商品详情";
		priceXpath = "//B[normalize-space(@class)='d_price']";
		discountPriceXpath = "//I[@id='promo_price']" + " | //DIV[@class='rule clearfix']/I";
		// marketPriceXpath = "//I[@class='m_price']";
		marketPriceXpath = "//SPAN[@id='originalPriceTag']" + " | //I[@id='originalPriceTag']";
		classicXpath = "//DIV[@class='breadcrumb']";
		productNameXpath = "//A[@id='largePicLink']/@title" + "| //DIV[@name='Title_pub']/H1";
		keywordXpath = "//META[@name='keywords']/@content";
		imgXpath = "//IMG[@id='largePic']/@wsrc";
		mparasXpath = "//UL[@class='tab_title clearfix']/LI/A";
		brandXpath = "//DIV[@class='mall_goods_foursort_style_frame'][1]";
		oriCatCodeXpath = "//DIV[@class='breadcrumb']//descendant::A/@href";
		totalCommentXpath = "//SPAN[@id='comm_num_up']/A/I";
		commentPercentXpath = "//SPAN[@id='comm_num_up']/I[@class='orange']";
		keywordsFilter.add("报价");
		keywordsFilter.add("当当网");
		keywordsFilter.add("【.*?】");
		titleFilter.add("【.*?】|当当网|网上购物|行情|报价");
		classFilter.add("当当网");
		classFilter.add("商品详情");

		// 合法url规则(只接受，商品页+列表页+商户页+品牌页)
		productFlag = itemFlag2;
		// 商品页地址规则
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		productUrlList.add(itemFlag3);
		productUrlList.add(itemFlag4);
	}

	public void parseInit() {
		String jsonValue = "";
		String value = "";
		int index1 = 0;
		int index2 = 0;
		Node jsonNode = null;
		NodeList nodeList = TemplateUtil.getNodeList(root, jsonXpath);
		if (nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				value = node.getTextContent().replaceAll("[\\s]{3,}", " ");
				index1 = value.indexOf("google_tag_params");
				index2 = value.indexOf("prodid");
				if (index1 > 0 && index2 > 0) {
					jsonNode = node;
					break;
				}
			}
		}
		if (jsonNode == null) {
			map.put("isJson", "false");
		} else {
			jsonValue = jsonNode.getTextContent().replaceAll("[\\s]{3,}", " ");
			index1 = jsonValue.indexOf("google_tag_params");
			jsonValue = jsonValue.substring(index1);// 得到json字符串
			jsonValue = this.delString(jsonValue, "promotionprice:");
			jsonValue = Formatter.getJson(jsonValue);
		}
		if (!"".equals(jsonValue)) {
			map.put("isJson", "true");
			JSONObject productObject = JSONObject.parseObject(jsonValue);
			map.put("skuid", productObject.getString("prodid"));
			map.put("name", productObject.getString("pname"));
			map.put("price", productObject.getString("dangdangprice"));
			map.put("marketprice", productObject.getString("marketprice"));
			map.put("classes", productObject.getString("pcat"));
			// map.put("brand",productObject.getString("pbrand"));
			map.put("author", productObject.getString("author"));// 作者
		}
		/**
		 * 解析javascript拼凑url时需要的属性值
		 */
		Node paramNode = TemplateUtil.getNode(root, paramXpath);
		if (paramNode != null) {
			NamedNodeMap attributeMap = paramNode.getAttributes();
			String paramProductId = getParam(attributeMap, "name");
			String paramType = getParam(attributeMap, "type");
			String paramDescribe = getParam(attributeMap, "describe_map");
			String paramCategory = getParam(attributeMap, "category_id");
			String paramMain = getParam(attributeMap, "main_id");
			map.put("paramProductId", paramProductId);
			map.put("paramType", paramType);
			map.put("paramDescribe", paramDescribe);
			map.put("paramCategory", paramCategory);
			map.put("paramMain", paramMain);
		}
	}

	/**
	 * 清除json字符串中不合法的json
	 * 
	 * @param jsonString
	 *            有不合法的json字符串
	 * @param property
	 *            json的属性名，例如清除属性isMorePrice，只需要填写”isMorePrice:“
	 * @return 清除该属性后的json字符串
	 */
	private String delString(String jsonString, String property) {
		int index1 = 0;
		int index2 = 0;
		index1 = jsonString.indexOf(property);
		if (index1 > 0) {
			index2 = jsonString.indexOf(",", index1);
			jsonString = jsonString.substring(0, index1) + jsonString.substring(index2 + 1);
		}
		return jsonString;
	}

	/**
	 * 将调用setid方法是就将parseInit()初始化。
	 */
	@Override
	public void setId() throws ProductIDIsNullException {
		try {
			parseInit();
		} catch (Exception ex) {
			LOG.info(url + "   parseInit Json Is Error");
		}
		try {
			initDocByTextArea(); // 统一处理品牌，商品参数，商品详情内容
		} catch (Exception ex) {
			LOG.info(url + "   initDocByTextArea Parser Is Error");
		}
		String pid = "";
		String isJson = map.get("isJson");// 判断是否有json
		if (isJson != null && !isJson.equals("") && isJson.equals("false")) {// 表示没有json被解析出来
			pid = map.get("paramProductId");
		} else {
			pid = (String) map.get("skuid");
		}
		if (pid == null || pid.isEmpty()) {
			throw new ProductIDIsNullException("-->Error---Product Id Is Null！,url=" + url);
		} else {
			String id = sellerCode + pid;
			super.product.setPid(id);
			super.product.setOpid(pid);
		}
	}

	/**
	 * 修改productNameXPath
	 */
	@Override
	public void setProductName() throws ProductNameIsNullException {
		String isJson = map.get("isJson");// 判断是否有json
		if (isJson != null && !"".equals(isJson) && isJson.equals("false")) {// 表示没有json被解析出来
			super.setProductName();
			return;
		}
		String productName = (String) map.get("name");
		if (productName == null || productName.isEmpty()) {
			new ProductNameIsNullException(url
					+ "-->Error---Failed To Get Product Sales ProductName!  setProductName Exception No Matching!");
		} else {
			if (keywordsFilter.size() > 0) {
				for (String keyFilter : keywordsFilter) {
					productName = productName.replaceAll(keyFilter, "");
				}
			}
			super.product.setProductName(productName);
		}
	}

	private String getParam(NamedNodeMap attributeMap, String paramName) {
		Node node = attributeMap.getNamedItem(paramName);
		if (node == null) {
			return "";
		} else {
			return node.getNodeValue();
		}
	}

	@Override
	public void setPrice() throws ProductPriceIsNullException {
		String price = "";
		try {
			Node priceNode = TemplateUtil.getNode(root, priceXpath);
			if (priceNode != null) {
				price = trimPrice(priceNode.getTextContent().replaceAll("[\\s]{1,}", ""));
				if (!StrUtil.isEmpty(price) && price.contains("-")) {
					String[] priceArry = price.split("-");
					price = priceArry[0];
				}
				price = price.replaceAll("[&yen]", "");
				if (price == null || price.isEmpty()) {
					throw new ProductPriceIsNullException(url
							+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
				}
			} else {
				String priceXpath2 = "//P[@class='red']";
				Node priceNode2 = TemplateUtil.getNode(root, priceXpath2);
				if (priceNode2 != null) {
					String inforPrice = priceNode2.getTextContent();
					if (inforPrice != null && !"".equals(inforPrice)) {
						if (inforPrice.contains("赠品")) {
							price = "0";
						}
					}
				}
			}
			this.product.setPrice(price);
		} catch (Exception ex) {
			LOG.info(url + "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!", ex);
		}
	}

	@Override
	public void setMaketPrice() {
		String marketPrice = "";
		String isJson = map.get("isJson");// 判断是否有json
		if (isJson != null && !"".equals(isJson) && isJson.equals("false")) {// 表示没有json被解析出来
			Node marketPriceNode = TemplateUtil.getNode(root, marketPriceXpath);
			if (marketPriceNode != null) {
				marketPrice = marketPriceNode.getTextContent().replaceAll("[\\s]{1,}", "");
			}
		} else {
			marketPrice = map.get("marketprice");
		}
		if (!StrUtil.isEmpty(marketPrice)) {
			if (marketPrice.contains("¥ ")) {
				marketPrice = marketPrice.substring(marketPrice.indexOf("¥ ") + 2);
			} else {
				marketPrice = trimPrice(marketPrice).replaceAll("[\\s]{1,}", "");
			}
			this.product.setMaketPrice(marketPrice);
		}
	}

	public void initDocByTextArea() {
		DocumentFragment initDocument = null;
		String initDocXpath = "//DIV[@id='detail_all']//descendant::TEXTAREA";
		Node initDocNode = TemplateUtil.getNode(root, initDocXpath);
		if (initDocNode != null) {
			initDocument = RequestUtil.getDocumentFragmentByString(initDocNode.getTextContent(), enCode);
			mapByDocument.put("1", initDocument);
		}
	}

	@Override
	public void setBrand() {
		String doc2Str = "";
		try {
			DocumentFragment brandNode = mapByDocument.get("1");
			if (brandNode != null) {
				String brandXpath = "//DIV[@class='mall_goods_foursort_style_frame']";
				NodeList brandNodeList = TemplateUtil.getNodeList(brandNode, brandXpath);
				if (brandNodeList.getLength() > 0) {
					for (int i = 0; i < brandNodeList.getLength(); i++) {
						doc2Str = brandNodeList.item(i).getTextContent();
						if (doc2Str.indexOf("品牌") < 0) {
							continue;
						}
						doc2Str = doc2Str.replaceAll("[品牌：:]", "");
						break;
					}
				}
			}
			// }
			if (StrUtil.isEmpty(doc2Str)) {
				// 可能是图书类商品，尝试获取出版社信息作为品牌字段
				Node tmp = TemplateUtil.getNode(root, bookBrandXpath);
				if (tmp != null) {
					doc2Str = tmp.getTextContent();
				}
			}
			if (!StrUtil.isEmpty(doc2Str)) {
				product.setBrand(doc2Str);
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName() + "The setBrand Parser Is Error!");
		}
	}

	@Override
	public void setClassic() {
		String isJson = map.get("isJson");// 判断是否有json
		if (isJson != null && !isJson.equals("") && isJson.equals("false")) {// 表示没有json被解析出来
			super.setClassic();
			return;
		}
		String classes = (String) map.get("classes");
		if (classes != null && !"".equals(classes)) {
			classes = classes.replaceAll("当当网,|,商品详情", "");
			classes = classes.replaceAll(",", Constant.XMTAG).replaceAll("[\\s]{1,}", "");
			this.product.setClassic(classes);
		}
	}

	@Override
	public void setMparams() {
		StringBuffer sb = new StringBuffer();
		StringBuffer shortNameBuffer = new StringBuffer();
		DocumentFragment mparamsDoc = mapByDocument.get("1");
		if (mparamsDoc != null) {
			String mparamsXpath = "//DIV[@class='mall_goods_foursort_style_frame']";
			NodeList mparamsNodeList = TemplateUtil.getNodeList(mparamsDoc, mparamsXpath);
			if (mparamsNodeList.getLength() > 0) {
				for (int i = 0; i < mparamsNodeList.getLength(); i++) {
					Node mparamsNode = mparamsNodeList.item(i);
					String mparamsStr = mparamsNode.getTextContent().replaceAll("：", "=");
					if (mparamsStr.indexOf("品牌") >= 0) {
						String brandBydocStr = mparamsStr.replaceAll("品牌=", "");
						shortNameBuffer.append(brandBydocStr);
					}
					if (mparamsStr.indexOf("型号") >= 0) {
						String modelBydocStr = mparamsStr.replaceAll("型号=", "");
						shortNameBuffer.append(" " + modelBydocStr);
					}
					sb.append(mparamsStr);
					sb.append("[xm99]");
				}
			}
		}
		String str = sb.toString();
		if (StrUtil.isEmpty(str)) {
			// 可能是图书类商品，尝试通过bookParamXpath解析获取参数信息
			String mp = TemplateUtil.getMParas(root, bookParamXpath);
			if (!StrUtil.isEmpty(mp)) {
				str = TemplateUtil.formatMparams(mp);
			}
		}
		product.setMparams(str);
		if (shortNameBuffer.length() > 0) {
			String shortName = shortNameBuffer.toString();
			this.product.setShortName(shortName);
			this.product.setShortNameDetail(shortName);
		}
	}

	/**
	 * 改从直接从页面获取内容
	 */
	@Override
	public void setContents() {
		String contents = "";
		StringBuffer sb = new StringBuffer();
		DocumentFragment contentsDoc = mapByDocument.get("1");
		if (contentsDoc != null) {
			String contentsStr = "";
			String contentsXpath = "//TABLE[@width='750']/TBODY/TR" + " | //DIV[@class='right_content']";
			NodeList contentsNodeList = TemplateUtil.getNodeList(contentsDoc, contentsXpath);
			if (contentsNodeList.getLength() > 0) {
				for (int i = 0; i < contentsNodeList.getLength(); i++) {
					Node contentsNode = contentsNodeList.item(i);
					contentsStr = contentsNode.getTextContent().replaceAll("[\\s]{2,}", " ").trim();
					sb.append(contentsStr);
				}
			}
		}
		contents = sb.toString();
		// 针对电子书类无详情无参数类的商品做处理，给定默认值。
		if (StrUtil.isEmpty(contents)) {
			contents = "此商品暂无商品详情内容.";
		}
		this.product.setContents(contents);
	}

	/**
	 * 通过指定outLinkXpath提取页面外链地址
	 * 
	 * @Override public List<imageProduct> nextUrlList(URL baseUrl,String content) {
	 *           List<imageProduct> urlList = new ArrayList<imageProduct>(); Set<imageProduct>
	 *           urlSet = new LinkedHashSet<imageProduct>(); URL urls = null; try { Document doc =
	 *           Jsoup.parse(content); Elements elementsByA=doc.getElementsByTag("A");
	 *           if(elementsByA.size() > 0){ for (int i = 0; i < elementsByA.size(); i++) { Element
	 *           urlListEle = elementsByA.get(i); String productUrl=urlListEle.attr("href"); String
	 *           smallPic=urlListEle.getElementsByTag("img").attr("src");
	 *           if(StrUtil.isEmpty(smallPic)){
	 *           smallPic=urlListEle.getElementsByTag("img").attr("imgsrc"); }
	 *           if(!smallPic.startsWith("http://")) continue; try{ urls =
	 *           URLUtil.resolveURL(baseUrl, productUrl); if(!StrUtil.isEmpty(urls.toString()) &&
	 *           !StrUtil.isEmpty(smallPic)){ imageProduct image=new
	 *           imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){
	 *           LOG.info(this.getClass().getSimpleName
	 *           ()+"  resolveURL Error! url: "+baseUrl.toString()); } } } if(urlSet.size() <= 0){
	 *           Element elementByDIV=doc.getElementsByClass("ddmlist").get(0); Elements
	 *           elementsByP=elementByDIV.getElementsByTag("p"); if(elementsByP.size() > 0){ for(int
	 *           j=0;j<elementsByP.size();j++){ Element urlListEle = elementsByP.get(j); String
	 *           productUrl
	 *           =urlListEle.getElementsByTag("span").get(0).getElementsByTag("a").attr("href");
	 *           String
	 *           smallPic=urlListEle.getElementsByTag("span").get(1).getElementsByTag("img").attr
	 *           ("src"); if(!smallPic.startsWith("http://")) continue; try{ urls =
	 *           URLUtil.resolveURL(baseUrl, productUrl); if(!StrUtil.isEmpty(urls.toString()) &&
	 *           !StrUtil.isEmpty(smallPic)){ imageProduct image=new
	 *           imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){
	 *           LOG.info(this.getClass().getSimpleName
	 *           ()+"  resolveURL Error! url: "+baseUrl.toString()); } } } } } catch (Exception e) {
	 *           LOG
	 *           .info(this.getClass().getSimpleName()+"  Parser Error! url: "+baseUrl.toString());
	 *           } urlList.addAll(urlSet); return urlList; }
	 */

	@Override
	public void setOriCatCode() {
		if (!oriCatCodeXpath.isEmpty()) {
			NodeList catGoryNodeList = TemplateUtil.getNodeList(root, oriCatCodeXpath);
			if (catGoryNodeList.getLength() > 0) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < catGoryNodeList.getLength(); i++) {
					String code = catGoryNodeList.item(i).getTextContent().trim();
					if (!code.contains("category_id=")) {
						continue;
					}
					if (i != 0) {
						sb.append("#");
					}
					code = code.substring(code.lastIndexOf("=") + 1, code.length());
					sb.append(code);
				}
				product.setOriCatCode(sb.toString());
			} else {
				LOG.info("The OriCatCode Is Null-->" + url);
			}
		} else {
			LOG.info("The OriCatCode Xpath Is Null-->" + url);
		}
	}

	protected String dealBrand(String mparams, OriProduct product) {
		String brand = product.getBrand();
		if (brand == null || brand.equals("")) {
			return super.dealBrand(mparams, product);
		} else {
			return brand;
		}
	}

	protected String dealShop(String mparams, OriProduct product) {
		if (mparams == null || mparams.equals("")) {
			return "当当商家店(暂时不解析商家名称)";
		} else {
			return "";
		}
	}

	@Override
	public void setCategory() {
		String cateGoryValue = this.product.getClassic();
		if (cateGoryValue != null && !"".equals(cateGoryValue)) {
			cateGoryValue = cateGoryValue.replaceAll("[\\s]{1,}", "");
			product.setCategory(cateGoryValue);
		}
	}

	@Override
	public void setLimitTime() {
		String limitTime = "";
		String discountByPrice = "";
		String limitTimeXpath = "//DIV[@class='show_info']/SCRIPT" + " | //DIV[@class='sale']/SCRIPT";
		Node limitTimeNode = TemplateUtil.getNode(root, limitTimeXpath);
		if (limitTimeNode != null) {
			String limitTimeValue = limitTimeNode.getTextContent().trim();
			if (limitTimeValue != null || !"".equals(limitTimeValue)) {
				String[] limitTimeArry = limitTimeValue.split(";");
				if (limitTimeArry.length > 0) {
					for (int i = 0; i < limitTimeArry.length; i++) {
						String limitTimeValue2 = limitTimeArry[i];
						if (limitTimeValue2.contains("var the_s")) {
							String[] limitTimeArry2 = limitTimeValue2.split("=");
							if (limitTimeArry2.length > 0) {
								limitTime = limitTimeArry2[1].replaceAll("'", "").replaceAll("[\\s]{1,}", "").trim();
								if (limitTime != null || !"".equals(limitTime)) {
									long limitTimeByLong = Long.parseLong(limitTime) * 1000
											+ System.currentTimeMillis();
									if (limitTimeByLong > 0) {
										limitTime = String.valueOf(limitTimeByLong);
										// 促销商品，将当前显示的价格设置为活动售价
										String discountTextXpath = "//SPAN[@class='icon_bg']";
										Node discountTextNode = TemplateUtil.getNode(root, discountTextXpath);
										if (discountTextNode != null) {
											String discountText = discountTextNode.getTextContent().replaceAll(
													"[\\s]{1,}", "");
											if (!StrUtil.isEmpty(discountText) && discountText.contains("抢购")) {
												String discountXpath = "//I[@id='promo_price']";
												Node discountNode = TemplateUtil.getNode(root, discountXpath);
												if (discountNode != null) {
													discountByPrice = trimPrice(discountNode.getTextContent());
												}
											}
										} else {
											discountByPrice = this.product.getPrice();
										}
										if (!StrUtil.isEmpty(discountByPrice)) {
											this.product.setDiscountPrice(discountByPrice);
											this.product.setPrice(discountByPrice);
											this.product.setLimitTime(limitTime);
										} else {
											this.product.setDiscountPrice("0");
											this.product.setLimitTime("0");
										}
										return;
									}
								}
							}
						}
					}
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

	public void initCommentContent() {
		try {
			String commentPid = this.product.getPid();
			if (!StrUtil.isEmpty(commentPid)) {
				String commentSecondUrl = "http://product.dangdang.com/comment/main.php?_="
						+ System.currentTimeMillis() + "&product_id=" + commentPid;
				String commentSrc = RequestUtil.getPageContent(commentSecondUrl, "gbk");
				if (!StrUtil.isEmpty(commentSrc)) {
					Document commentDoc = Jsoup.parse(commentSrc);
					if (commentDoc != null) {
						Element commentContent = commentDoc.getElementById("comment_list");
						Elements commentCats = commentContent.getElementsByClass("comment_items");
						elementsMap.put("commentCats", commentCats);
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
			Elements jsonArray = elementsMap.get("commentCats");
			int arraySize = jsonArray == null ? 0 : jsonArray.size();

			if (0 < arraySize) {
				List<Comment> clist = new ArrayList<Comment>();
				for (int i = 0; i < arraySize; i++) {
					Element o = jsonArray.get(i);
					if (o != null) {
						String cid = o.getElementsByClass("comment_btn").get(0).getElementsByTag("a").get(1)
								.attr("reviewId");
						if (StringUtils.isNotEmpty(cid)) { // describe_summary_38576146
							String name = o.getElementsByClass("items_user").get(0).getElementsByTag("p").get(0)
									.getElementsByTag("a").text();
							String content = o.getElementsByClass("items_detail").get(0).getElementsByTag("p").get(0)
									.text();
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