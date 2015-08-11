package com.ym.nutch.plugin.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONObject;
import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.parse.template.PriceTool;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;
import com.ym.nutch.plugin.util.URLUtil;

public class ParserYihaodianNew extends ParserParent {

	public static final Logger						LOG				= LoggerFactory.getLogger(ParserYihaodianNew.class);

	// 过滤商品详情页面 20131010更改新域名yhd.com
	// http://item.yihaodian.com/item/1071786_2-->http://item.yhd.com/item/1071786_2
	// http://item.1mall.com/item/8466824_2-->http://item.yhd.com/item/11681232_14
	// http://item.yhd.com/item/6371663_2
	private static Pattern							itemFlag1		= Pattern
																			.compile("(http\\://(www|item)\\.(yihaodian|yhd)\\.com/(item|product)/)([0-9]+).*?");
	private static Pattern							itemFlag2		= Pattern
																			.compile("(http\\://(www|item)\\.1mall\\.com/(item|product)/)([0-9]+).*?");
	// 手机端商品url：http://m.yhd.com/mw/productsquid/12097210/1/20/
	private static Pattern							itemFlag3		= Pattern
																			.compile("(http\\://m\\.yhd\\.com/mw/productsquid/)([0-9]+).*?");
	// 手机端商品url：http://m.yhd.com/product/8809_1_1_1
	private static Pattern							itemFlag4		= Pattern
																			.compile("(http\\://m\\.yhd\\.com/product/)([0-9_]+).*?");
	List<Pattern>									formatUrlList	= new ArrayList<Pattern>();
	private List<String>							seeds			= new ArrayList<String>();
	private static Map<String, DocumentFragment>	documentMap		= new HashMap<String, DocumentFragment>();

	public ParserYihaodianNew() {
		init();
	}

	public ParserYihaodianNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	Map<String, String>	map				= new HashMap<String, String>();
	DocumentFragment	contentDocument	= null;

	public void init() {
		sellerCode = "1012";
		seller = "一号店";
		idXPath = "//INPUT[@id='productMercantId']/@value";
		marketPriceXpath = "//DEL[@id='old_price']" + "| //SPAN[@class='oldprice']";
		priceXpath = "//SPAN[@id='nonMemberPrice']/@price" + "| //SPAN[@id='current_price']";
		brandXpath = "//DIV[@class='detailnav']//descendant::A[last()-1]" + "| //INPUT[@id='brandName']/@value";
		classicXpath = "//DIV[@class='detailnav']//descendant::A"
				+ "| //DIV[@class='crumb clearfix']//descendant::A[position()>1]";
		productNameXpath = "//INPUT[@id='productName']/@value" + "| //IMG[@id='productImg']/@title"
				+ "| //H1[@id='productMainName']";
		keywordXpath = "//META[@name='Keywords']/@content";
		imgXpath = "//IMG[@id='productImg']/@src" + "| //IMG[@id='J_prodImg']/@src";
		mparasXpath = "//DL[@class='standard']/descendant::DD";
		contentDetailXpath = "//DIV[@id='prodDescTabContent']";
		oriCatCodeXpath = "//SCRIPT";
		shortNameXpath = "//DIV[@class='s_standard_bd']/UL//descendant::LI"
				+ "| //DIV[@id='prodDetailCotentDiv']/DL//descendant::DD";
		// categoryXpath="//DIV[@class='detailnav']/SPAN/A";
		nextPageXpath = "//A[@class='page_next']/@href";
		filterStr = "所有分类 ";

		productFlag = itemFlag2;

		throughList.add(itemFlag1);
		throughList.add(itemFlag2);

		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		productUrlList.add(itemFlag3);
		productUrlList.add(itemFlag4);

		classFilter.add("1号店购物，手机也可以");
		classFilter.add("所有分类");

		keywordsFilter.add("报价");
		keywordsFilter.add("一号店");
		keywordsFilter.add("1号店");

		titleFilter.add("\\[.*?\\]|【.*?】|一号店|1号店|行情|报价|价格");
		outLinkXpath.add("//UL[@class='fl floorMain']/LI/A[1]/@href"); // 首页 http://www.1mall.com/1/
		outLinkXpath.add("//DIV[@class='alonesort']/DIV[@class='mt']/H3/A/@href");// 总分类页面的第一级分类 //
																					// http://www.yihaodian.com/marketing/allproduct.html
		outLinkXpath.add("//DIV[@class='alonesort']/DIV[@class='mc']/DL/DD/EM/SPAN/A/@href");// 总分类页面第三级分类
																								// //
																								// http://www.yihaodian.com/marketing/allproduct.html
		outLinkXpath.add("//UL[@id='itemSearchList']/LI[@class='search_item']/DIV/A[1]/@href"); // 分类列表页
																								// //
																								// http://www.yihaodian.com/ctg/s2/c21306-0/2/

		// 种子地址，不过滤
		seeds.add("http://www.yhd.com/marketing/allproduct.html");
		seeds.add("http://www.1mall.com/1/");

	}

	@Override
	public void setId() throws ProductIDIsNullException {
		Node idNode = TemplateUtil.getNode(root, idXPath);
		if (idNode == null) {
			throw new ProductIDIsNullException(url + ":未能获取商品id！！");
		} else {
			String pid = idNode.getTextContent();
			if (pid == null || pid.isEmpty()) {
				throw new ProductIDIsNullException(url + ":未能获取商品id！！,节点为null");
			} else {
				String id = sellerCode + pid;
				product.setPid(id);
				product.setOpid(pid);
			}
		}
	}

	@Override
	public void setBrand() {
		Node node = TemplateUtil.getNode(root, brandXpath);
		if (node != null) {
			String value = node.getTextContent().trim();
			if (value != null && !"".equals(value)) {
				value = value.replaceAll("/", "\\[xm99\\]");
				product.setBrand(value);
			}
		}
	}

	public void initContentDocument() {
		String resultJson = "";
		String targetUrl = "";
		String productIdXpath = "";
		String merchantIdXpath = "";
		String isYihaodianXpath = "";
		String productId = "";
		String merchantId = "";
		String isYihaodian = "";
		Node nodeProduct = null;
		Node nodeMerchant = null;
		Node nodeIsYihaodian = null;
		try {
			String pid = this.product.getPid();
			if (pid != null && !"".equals(pid)) {
				productIdXpath = "//INPUT[@id='productId']/@value";
				merchantIdXpath = "//INPUT[@id='merchantId']/@value";
				isYihaodianXpath = "//INPUT[@id='isYiHaoDian']/@value";
				nodeProduct = TemplateUtil.getNode(root, productIdXpath);
				nodeMerchant = TemplateUtil.getNode(root, merchantIdXpath);
				nodeIsYihaodian = TemplateUtil.getNode(root, isYihaodianXpath);
				if (nodeProduct != null && nodeMerchant != null && nodeIsYihaodian != null) {

					productId = nodeProduct.getTextContent();
					merchantId = nodeMerchant.getTextContent();
					isYihaodian = nodeIsYihaodian.getTextContent();
					targetUrl = "http://item-home.yhd.com/item/ajax/ajaxProdDescTabView.do?callback=detailProdDesc.prodDescCallback&productID="
							+ productId + "&merchantID=" + merchantId + "&isYihaodian=" + isYihaodian + "&pmId=" + pid;
				}
			}
			// }
			if (!StrUtil.isEmpty(targetUrl)) {
				String builder = RequestUtil.getPageContent(targetUrl, "UTF-8");
				if (!StrUtil.isEmpty(builder)) {
					builder = builder.replaceAll("[\\s]{3,}", " ");
					resultJson = builder.substring(builder.indexOf("({") + 1, builder.indexOf("})") + 1);
					if (resultJson == null && "".equals(resultJson)) {
						LOG.info(this.getClass() + "没有解析到json对象");
						map.put("resultJson", "false");
					} else {
						JSONObject jsonObj = JSONObject.parseObject(resultJson);
						if (jsonObj != null) {
							map.put("value", jsonObj.getString("value"));
						} else {
							map.put("value", "");
						}
					}
				}
			}

		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName() + "The initContentDocument() Is Parser ERROR !");
		}
	}

	@Override
	public void setContents() {
		String contents = "";
		StringBuffer sb = new StringBuffer();
		DocumentFragment contentDocument = null;
		String valueByMap = map.get("value");
		if (valueByMap != null && !"".equals(valueByMap)) {
			contentDocument = RequestUtil.getDocumentFragmentByString(valueByMap, enCode);
		}
		if (contentDocument != null) {
			NodeList contentNodeList = TemplateUtil.getNodeList(contentDocument, contentDetailXpath);
			if (contentNodeList.getLength() > 0) {
				for (int i = 0; i < contentNodeList.getLength(); i++) {
					contents = contentNodeList.item(i).getTextContent().replaceAll("[\\s]{2,}", "");
					sb.append(contents);
				}
			} else {
				sb.append("此商品暂无商品详情内容.");
			}
		} else {
			sb.append("此商品暂无商品详情内容.");
		}
		product.setContents(sb.toString());
	}

	@Override
	public void setMparams() {
		String value = "";
		initContentDocument();
		DocumentFragment contentDocument = null;
		String valueByMap = map.get("value");
		if (valueByMap != null && !"".equals(valueByMap)) {
			contentDocument = RequestUtil.getDocumentFragmentByString(valueByMap, enCode);
		}
		if (contentDocument != null) {
			value = TemplateUtil.getMParas(contentDocument, mparasXpath);
			if (value != null && !"".equals(value)) {
				value = TemplateUtil.formatMparams(value);
			} else {
				value = "此商品暂无商品参数内容.";
			}
		} else {
			value = "此商品暂无商品参数内容.";
		}
		product.setMparams(value);
	}

	@Override
	public boolean isSafeUrl(String url) {
		if (urlFilter(url)) {
			return true;
		} else {
			for (String seed : seeds) {
				if (seed.equals(url)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @Override public List<imageProduct> nextUrlList(URL baseUrl,String content) {
	 *           System.out.println(content); List<imageProduct> urlList = new
	 *           ArrayList<imageProduct>(); Set<imageProduct> urlSet = new
	 *           LinkedHashSet<imageProduct>(); URL urls = null; String productUrl=""; String
	 *           smallPic=""; Pattern
	 *           productFlag=Pattern.compile("http://m.yhd.com/mw/([a-zA-Z]+)/([0-9]+).*?"); Matcher
	 *           productMatch=productFlag.matcher(baseUrl.toString()); try {
	 *           if(productMatch.find()){ String
	 *           contentByPro=RequestUtil.getPageContent(baseUrl.toString(), "utf-8"); int
	 *           index=contentByPro.indexOf("(new Parameter(")+15; if(index>0){ String
	 *           proId=contentByPro.substring(index,
	 *           contentByPro.indexOf("function backlist()")).trim();
	 *           proId=proId.substring(proId.indexOf(",\"")+2, proId.indexOf("\"));"));
	 *           productUrl="http://item.yhd.com/item/"+proId; } Node
	 *           imgNode=TemplateUtil.getNode(root,
	 *           "//DIV[@id='proDetailScroll']/DIV/UL/LI[1]/IMG/@src"); if(imgNode != null){
	 *           smallPic=imgNode.getTextContent().trim(); } try{ urls = URLUtil.resolveURL(baseUrl,
	 *           productUrl); if(!StrUtil.isEmpty(urls.toString()) && !StrUtil.isEmpty(smallPic)){
	 *           imageProduct image=new imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){ } }else{ Document doc =
	 *           Jsoup.parse(content); Elements elements=doc.getElementsByTag("A");
	 *           if(elements.size() > 0){ for (int i = 0; i < elements.size(); i++) { Element
	 *           urlListEle = elements.get(i); productUrl=urlListEle.attr("href");
	 *           smallPic=urlListEle.getElementsByTag("img").attr("src"); try{ urls =
	 *           URLUtil.resolveURL(baseUrl, productUrl); if(!StrUtil.isEmpty(urls.toString()) &&
	 *           !StrUtil.isEmpty(smallPic)){ imageProduct image=new
	 *           imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){ } } } } } catch (Exception
	 *           e) { e.printStackTrace(); } urlList.addAll(urlSet); return urlList; }
	 */

	public void evenMore(URL secondBaseUrl, Set<String> set) {
		String urlTargetMore = "";
		String urlMore = "";
		String secondBaseUrlMore = secondBaseUrl.toString();
		secondBaseUrlMore = formatSafeUrl(secondBaseUrlMore);
		secondBaseUrlMore = secondBaseUrlMore.replaceAll("/s2/", "/searchPage/");
		String secondUrl = secondBaseUrlMore + "?callback=jsonp" + System.currentTimeMillis()
				+ "&isGetMoreProducts=1&moreProductsDefaultTemplate=0";
		try {
			StringBuilder builder = RequestUtil.getOfHttpURLConnection(secondUrl, "UTF-8");
			if (builder != null) {
				String valueDocument = builder.toString().replaceAll("[\\s]{3,}", " ");
				if (valueDocument != null && !"".equals(valueDocument)) {
					String resultJson = Formatter.getJson(valueDocument);
					if (resultJson == null && "".equals(resultJson)) {
						LOG.info(this.getClass() + ":url-->" + "evenMore Json Is Error");
						map.put("resultJson", "false");
					} else {
						JSONObject jsonObj = JSONObject.parseObject(resultJson);
						if (jsonObj != null) {
							String value = jsonObj.getString("value");
							if (value != null && !"".equals(value)) {
								DocumentFragment secondDoc = RequestUtil.getDocumentFragmentByString(value, "utf-8");
								if (secondDoc != null) {
									String secondXpath = "//LI[@class='producteg']/DIV/A[1]/@href";
									NodeList secondNodeList = TemplateUtil.getNodeList(secondDoc, secondXpath);
									if (secondNodeList.getLength() > 0) {
										for (int i = 0; i < secondNodeList.getLength(); i++) {
											urlTargetMore = secondNodeList.item(i).getTextContent();
											try {
												if (urlTargetMore.startsWith("javascript")
														|| urlTargetMore.startsWith("https")) {
													continue;
												}
												URL urlsMore = URLUtil.resolveURL(secondBaseUrl, urlTargetMore);
												urlMore = urlsMore.toString();
												if (set.contains(url) || !isSafeUrl(url)) {
													continue;
												}
												urlMore = formatSafeUrl(urlMore);
												set.add(urlMore);
											} catch (MalformedURLException e) {
												LOG.error(this.getClass().getSimpleName() + "->nextUrlList error " + e);
											}
										}
									}
								}
							}
						} else {
							LOG.info(this.getClass() + ":url-->" + "evenMore Json Is Error jsonObj==null!");
						}
					}
				}
			} else {
				LOG.info(this.getClass() + ":url-->" + "evenMore Json Is Error builder==null!");
			}
		} catch (Exception e) {
			LOG.error(this.getClass().getSimpleName() + "->nextUrlList error " + e);
		}
	}

	protected String dealBrand(String mparams, OriProduct product) {
		String brand = product.getProductName().replaceAll("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）", "").substring(0, 2);
		if (brand == null || brand.equals("")) {
			return super.dealBrand(mparams, product);
		} else {
			return brand;
		}
	}

	/**
	 * 20131106添加 品牌+型号
	 */
	@Override
	public void setShortName() {
		String shortName = "";
		StringBuffer shortNameBuffer = new StringBuffer();
		String brandValue = this.product.getBrand();
		if (brandValue != null && !"".equals(brandValue)) {
			if (brandValue.contains("[xm99]")) {
				String[] brandValueArry = brandValue.split("\\[xm99\\]");
				for (int i = 0; i < brandValueArry.length; i++) {
					String brandValueStr = brandValueArry[i];
					char c = brandValueStr.charAt(0);
					if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
						shortNameBuffer.append(brandValueStr);
						break;
					}
				}
			}
		}
		DocumentFragment contentDocument = null;
		String valueByMap = map.get("value");
		if (valueByMap != null && !"".equals(valueByMap)) {
			contentDocument = RequestUtil.getDocumentFragmentByString(valueByMap, enCode);
		}
		if (contentDocument != null) {
			NodeList shortNameNodeList = TemplateUtil.getNodeList(contentDocument, shortNameXpath);
			if (shortNameNodeList.getLength() > 0) {
				for (int i = 0; i < shortNameNodeList.getLength(); i++) {
					Node doc2Node = shortNameNodeList.item(i);
					String doc2Str = doc2Node.getTextContent().replaceAll("：", ":").replaceAll("[\\s]{1,}", "").trim();
					if (StrUtil.isEmpty(brandValue)) {
						if (doc2Str.indexOf("商品品牌") >= 0) {
							String brandBydoc2Str = doc2Str.replaceAll("商品品牌:", "");
							shortNameBuffer.append(brandBydoc2Str);
						}
					}
					if (doc2Str.indexOf("型号") >= 0) {
						String modelBydoc2Str = doc2Str.replaceAll("型号:", "");
						shortNameBuffer.append(" " + modelBydoc2Str);
						break;
					}
				}
			}
		}
		if (shortNameBuffer.length() > 0) {
			shortName = shortNameBuffer.toString();
			this.product.setShortName(shortName);
			this.product.setShortNameDetail(shortName);
		}
	}

	/**
	 * 20131115添加：限时抢购的截止时间
	 */
	@Override
	public void setLimitTime() {
		String limitTime = "";
		String productId = this.product.getPid();
		if (productId != null || !"".equals(productId)) {
			String jsonpValue = "jsonp" + System.currentTimeMillis();
			String limitTimeSecondUrl = "http://busystock.i.1mall.com/restful/detail?mcsite=1&provinceId=2&cityId=1000&countyId=32017&pmId="
					+ productId + "&callback=" + jsonpValue;
			String limitTimeValue = RequestUtil.getOfHttpURLConnection(limitTimeSecondUrl, "utf-8").toString();
			try {
				if (limitTimeValue != null && !"".equals(limitTimeValue)) {
					int index = limitTimeValue.indexOf(jsonpValue);
					if (index >= 0) {
						limitTimeValue = limitTimeValue.substring(index); // 得到json字符串
						limitTimeValue = Formatter.getJson(limitTimeValue);
						if (!"".equals(limitTimeValue)) {
							JSONObject jsonObj = JSONObject.parseObject(limitTimeValue);
							limitTime = jsonObj.getString("remainTime");
							if (limitTime != null || !"".equals(limitTime)) {
								long systemDate = System.currentTimeMillis();
								long dateTime = Long.parseLong(limitTime) + systemDate;
								if (dateTime > 0 && dateTime > systemDate) {
									String price = this.product.getPrice();
									if (price != null || !"".equals(price)) {
										this.product.setDiscountPrice(price);
									}
									limitTime = String.valueOf(dateTime);
									// System.out.println(new Date(dateTime));
									this.product.setLimitTime(limitTime);
								}
							}
						}
					}
				}
			} catch (Exception e) {
			}
			if (limitTime == null || "".equals(limitTime)) {
				this.product.setDiscountPrice("0");
				this.product.setLimitTime("0");
			}
		}
	}

	/**
	 * 20131115添加：活动销售价格的折扣 电商的当前打折销售价格除以市场价格后，精确到小数点后两位
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
	 * 20131115添加：当前销售价格的折扣=电商的当前销售价格除以市场价格后，精确到小数点后两位。
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

	// 20131121添加
	@Override
	public void setTotalComment() {
		String commentCount = "";
		String comentPercent = "";
		String commentCountXpath = "//P[@class='num']/SPAN";
		String commentPercentXpath = "//DIV[@class='evaluate_con clearfix']/UL/LI[1]/SPAN[contains(text(),'%好评')]";
		try {
			Node experoienceProductIdNode = TemplateUtil.getNode(root, "//INPUT[@id='experoienceProductId']/@value");
			if (experoienceProductIdNode != null) {
				String experoienceProductId = experoienceProductIdNode.getTextContent().trim();
				if (experoienceProductId != null && !"".equals(experoienceProductId)) {
					String totalCommentUrl = "http://e.yhd.com/front-pe/productExperience/proExperienceAction!ajaxView_pe.do?product.id="
							+ experoienceProductId;
					String totalCommentUrlValue = RequestUtil.getOfHttpURLConnection(totalCommentUrl, "utf-8")
							.toString();
					if (totalCommentUrlValue != null && !"".equals(totalCommentUrlValue)) {
						totalCommentUrlValue = Formatter.getJson(totalCommentUrlValue);
						if (!"".equals(totalCommentUrlValue)) {
							JSONObject jsonObj = JSONObject.parseObject(totalCommentUrlValue);
							String comment = jsonObj.getString("value");
							if (comment != null && !"".equals(comment)) {
								DocumentFragment commentDocument = RequestUtil.getDocumentFragmentByString(comment,
										"utf-8");
								if (commentDocument != null) {
									Node commentCountNode = TemplateUtil.getNode(commentDocument, commentCountXpath);
									Node comentPercentNode = TemplateUtil.getNode(commentDocument, commentPercentXpath);
									if (commentCountNode != null) {
										commentCount = StringUtils.trim(commentCountNode.getTextContent());
										int cc = commentCount == null ? 0 : StringUtils.isEmpty(commentCount) ? 0
												: Integer.parseInt(commentCount);
										this.product.setTotalComment(cc);
									}
									if (comentPercentNode != null) {
										comentPercent = comentPercentNode.getTextContent();
										if (!"".equals(comentPercent)) {
											comentPercent = comentPercent.replaceAll("[\u4E00-\u9FA5a-zA-Z%]", "");// 去掉中文字符串，只留数字
											map.put("comentPercent", comentPercent);
										}
									}
									return;
								}
							}
						}

					}
				}
			}
		} catch (Exception e) {

		}
	}

	// 20131121添加
	@Override
	public void setCommentPercent() {
		String commentPercent = map.get("comentPercent");
		if (commentPercent != null && !"".equals(commentPercent)) {
			this.product.setCommentPercent(commentPercent);
		} else {
			this.product.setCommentPercent("0");
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
				classicValue = classicValue.replaceAll("首页\\[xm99\\]", "");
				product.setClassic(classicValue);
			}
		}
	}

	public void initCommentContent() {
		String productPid = "";
		String merchantId = "";
		try {
			String productIdXpath = "//INPUT[@id='productId']/@value";
			String merchantIdXpath = "//INPUT[@id='merchantId']/@value";
			Node productIdNode = TemplateUtil.getNode(root, productIdXpath);
			Node merchantIdNode = TemplateUtil.getNode(root, merchantIdXpath);
			if (productIdNode != null && merchantIdNode != null) {
				productPid = productIdNode.getTextContent();
				merchantId = merchantIdNode.getTextContent();
			}
			if (!StrUtil.isEmpty(productPid) && !StrUtil.isEmpty(merchantId)) {
				String commentSecondUrl = "http://e.yhd.com/front-pe/productExperience/proExperienceAction!ajaxView_pe.do?product.id="
						+ productPid + "&merchantId=" + merchantId;
				String commentContentValue = RequestUtil.getOfHttpURLConnection(commentSecondUrl, "utf-8").toString();
				if (!StrUtil.isEmpty(commentContentValue)) {
					JSONObject jsonPrim = JSONObject.parseObject(commentContentValue);
					String value = jsonPrim.getString("value");
					DocumentFragment commentDoc = RequestUtil.getDocumentFragmentByString(value, "utf-8");
					if (commentDoc != null) {
						documentMap.put("commentDoc", commentDoc);
					}

				}
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName() + ": The initCommentContent Is Parser Error! " + url);
		}
	}

	/**
	 * 1号店由于没有给出的评论id，现以用户名+评论时间转换的时间戳=评论id
	 */
	@Override
	public void setCommentList() {
		// 初始化评论内容文本
		initCommentContent();
		

		String id[] = new String[10];
		String name[] = new String[10];
		String content[] = new String[10];
		// 1-10
		for (int i = 1; i <= 10; i++) {
			id[i - 1] = "//DIV[@class='main']/DIV[@class='item good-comment']["+i+"]/DL/DT/SPAN[@class='date']";
			name[i - 1] = "//DIV[@class='main']/DIV[@class='item good-comment']["+i+"]/DL/DT/SPAN[@class='name']";
			content[i - 1] = "//DIV[@class='main']/DIV[@class='item good-comment']["+i+"]/DL/DD[SPAN[contains(text(),'内容')]]/SPAN[@class='text']";
		}
		
		try {
			List<Comment> clist = new ArrayList<Comment>();
			for (int i = 0; i < 10; i++) {
				Comment c = getComment(id[i], name[i], content[i]);
				if (c != null)
					clist.add(c);
			}

			product.setClist(clist);

		} catch (Exception e) {
		}
		
	}

	private Comment getComment(String idXpath, String nameXpath, String contentXpath) {
		DocumentFragment dof = documentMap.get("commentDoc");
		Node idNode = TemplateUtil.getNode(dof, idXpath);
		Node nameNode = TemplateUtil.getNode(dof, nameXpath);
		Node contentNode = TemplateUtil.getNode(dof, contentXpath);
		if (idNode == null || nameNode == null || contentNode == null) {
			return null;
		}

		String ctime = idNode.getTextContent().replaceAll("[\\s]{2,}", "").trim();
		String name = nameNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();
		String content = contentNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();

		if (StringUtils.isNotEmpty(ctime) && StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(content)) {
			long time = System.currentTimeMillis();
			try {
				Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ctime);
				time = d.getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}

			String cid = name + time;

			String c = name + "[xm99]" + content;
			Comment cm = new Comment();
			cm.setCommentId(cid);
			cm.setCommentContent(c);
			return cm;
		}

		return null;
	}

	// *******************************************************************************************************************

	public static void main(String[] args) throws Exception {
		// String url = "http://item.1mall.com/item/3416261_2";
		// HtmlParser parser = new HtmlParser();
		// Product p = parser.testParse(url);
		// System.out.println(p);
		// long dateTime=Long.parseLong("205256358")+System.currentTimeMillis();
		// System.out.println(new Date(dateTime));
		ParserYihaodianNew test2 = new ParserYihaodianNew();
		test2.test1();
	}

	public void test1() throws Exception {
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\YiHaoDian.html"), "utf-8");
		ParserYihaodianNew test = new ParserYihaodianNew(root);
		test.product = new OriProduct();

		try {
			test.setId();
			System.out.println("id=====>: " + test.product.getPid() + ">>>>" + test.product.getOpid());
		} catch (ProductIDIsNullException e) {
		}
		test.setCommentList();
		System.out.println("商品评论One======>:" + test.product.getClist().get(0).getCommentId() + ">>>>"
				+ test.product.getClist().get(0).getCommentContent());

		// test.setBrand();
		// System.out.println("品牌=====>：" + test.product.getBrand());
		//
		// test.setClassic();
		// System.out.println("导航=====>: " + test.product.getClassic());
		//
		// test.setOrgPic();
		// System.out.println("图片=====>：" + test.product.getOrgPic());
		//
		// test.setMparams();
		// System.out.println("mp内容=====> ：" + test.product.getMparams());
		//
		// test.setContents();
		// System.out.println("content=====>：" + test.product.getContents());
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
		// test.setTitle();
		// System.out.println("标题=====>：" + test.product.getTitle());
		//
		// test.setKeyword();
		// System.out.println("关键字=====>: " + test.product.getKeyword());
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
		// // test.setCheckOriCatCode();
		// // System.out.println("中文混合编码原始分类====》:" + test.product.getCheckOriCatCode());
		//
		//
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
		//
		// test.setTotalComment();
		// System.out.println("总评论数=====>:" + test.product.getTotalComment());
		//
		// test.setCommentStar();
		// System.out.println("评论星级数=====>:" + test.product.getCommentStar());
		//
		// test.setCommentPercent();
		// System.out.println("评论百分数=====>:" + test.product.getCommentPercent());

		// URL url = new
		// URL("http://www.yhd.com/ctg/s2/c21306-%E6%89%8B%E6%9C%BA%E9%80%9A%E8%AE%AF-%E6%95%B0%E7%A0%81%E7%94%B5%E5%99%A8/");
		// List<String> list = test.nextUrlList(url);
		// for (String string : list) {
		// System.out.println(string);
		// }
		// System.out.println(list.size());

		// http://www.yhd.com/ctg/s2/c27470-0/b/a-s1-v0-p1-price-d0-f0-m1-rt0-pid-mid0-k/2/
		// 下一页--》
		// URL url = new
		// URL("http://www.yhd.com/ctg/s2/c21306-0/2/");
		// String bb = test.nextUrl(url);
		// System.out.println("下一页:"+bb);

		// String url="http://item.yihaodian.com/item/2761339_1";
		// String aa = test.formatSafeUrl(url);
		// System.out.println("格式化后----"+aa);

		// String url2="http://search.yhd.com/s2/c0-0/k%25e5%25a4%25a7%25e7%258e%258b/1/";
		// boolean bb = test.isSafeUrl(url2);
		// System.out.println("是否被过滤----"+bb);

	}

	public void test2() throws Exception {
		ParserYihaodianNew yhd = new ParserYihaodianNew();
		String url = "http://item.yihaodian.com/item/1071786_2";
		boolean b = yhd.isProductPage(url);
		System.out.println("是不是产品页：" + b);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File("D:\\20130904201439_url_1mall.txt")));
			String r = reader.readLine();
			while (r != null) {
				if (r.startsWith("javascript") || r.startsWith("https")) {
					r = reader.readLine();
					continue;
				}
				r = r.toString();
				if (!yhd.isSafeUrl(r)) {
					r = reader.readLine();
					continue;
				}
				r = yhd.formatSafeUrl(r);
				System.out.println("格式化后----" + r);
				r = reader.readLine();
			}
		} catch (MalformedURLException e) {

		}

		System.out.println("http://www.yihaodian.com/ctg/s2/c16070-0/b/a-s1-v0-p2-price-d0-f0-m1-rt0-pid-k".length());

	}
}
