package com.ym.nutch.plugin.parse;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONObject;
import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductNameIsNullException;
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

public class ParserSuningNew extends ParserParent {

	public static final Logger						LOG			= LoggerFactory.getLogger(ParserSuningNew.class);

	// http://www.suning.com/emall/prd_10052_10051_-7_1991097_.html
	// http://product.suning.com/102569029.html
	// 商品页正则
	private Pattern									itemFlag1	= Pattern
																		.compile("http://product.suning.com/([0-9]+).html.*");
	// http://www.suning.com/emall/prd_10052_14656_-7_4700801_.html
	private Pattern									itemFlag2	= Pattern
																		.compile("http://www.suning.com/emall/prd([0-9_-]+).html.*");
	// 手机端商品url：http://m.suning.com/emall/snmwprd_10052_10051_17006899_.html
	private Pattern									itemFlag3	= Pattern
																		.compile("http://m.suning.com/emall/snmwprd_([0-9_-]+).html.*");
	// 手机端商品url：http://m.suning.com/product/104083059.html
	private Pattern									itemFlag4	= Pattern
																		.compile("http://m.suning.com/product/([0-9]+).html.*");

	String											jsonXpath	= "//SCRIPT";
	Map<String, String>								map			= new HashMap<String, String>();
	private static Map<String, DocumentFragment>	documentMap	= new HashMap<String, DocumentFragment>();

	public ParserSuningNew() {
		init();
	}

	public ParserSuningNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	public void init() {
		sellerCode = "1001";
		seller = "苏宁易购";
		filterStr = "首页";
		idXPath = "//INPUT[@name='catEntryId_1']/@value";
		brandXpath = "//TABLE[@id='pro_para_table']/TR[TD[DIV[SPAN[contains(text(),'品牌：')]]]]/TD[2]";
		classicXpath = "//DIV[@class='path w cityId_replace']";
		categoryXpath = "//DIV[@class='path w cityId_replace']/descendant::A";
		productNameXpath = "//H1";
		keywordXpath = "//META[@name='keywords']/@content";
		imgXpath = "//DIV[@id='PicView']/descendant::IMG/@src" + " | //DIV[@class='bookFourthThum']/IMG/@src"
				+ " | //UL[@class='slide_ul']/LI/IMG/@src";
		mparasXpath = "//TABLE[@id='pro_para_table']/descendant::TR[TD]";
		contentDetailXpath = "//DIV[@id='detail_content']";
		priceXpath = "//EM[@id='easyPrice']";
		discountPriceXpath = "//EM[@id='rpPrice']";
		nextPageXpath = "//A[@id='nextPage']/@href" + " | //A[@id='next']/@href";
		totalCommentXpath = "//A[@id='jump_to_comment']/SPAN";
		commentPercentXpath = "//INPUT[@name='commentPercentOfGood']/@value";
		keywordsFilter.add("报价");
		keywordsFilter.add("价格");
		keywordsFilter.add("苏宁易购");
		keywordsFilter.add("苏宁");
		titleFilter.add("报价");
		titleFilter.add("价格表");
		titleFilter.add("【.*?】");
		titleFilter.add("苏宁易购");
		titleFilter.add("苏宁");

		// 合法URL规则
		productFlag = itemFlag1;
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
		int index3 = 0;
		List<Node> jsonList = new LinkedList<Node>();// 存放sn节点
		Node pageNode = null;// 分页node
		NodeList nodeList = TemplateUtil.getNodeList(root, jsonXpath);
		if (nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				value = node.getTextContent().replaceAll("[\\s]+", "");
				index1 = value.indexOf("varsn=");
				index2 = value.indexOf("var");
				index3 = value.indexOf("varsnqa=");
				if (index1 >= 0 && index2 >= 0) {
					jsonList.add(node);
				}
				if (index3 >= 0) {
					jsonList.add(node);
				}
				index1 = value.indexOf("param.sortType");
				if (index1 >= 0) {
					pageNode = node;
				}
			}
		}
		if (jsonList.size() == 0) {
			LOG.info(this.getClass() + "没有解析到json对象");
		} else {
			for (Node node : jsonList) {
				try {
					jsonValue = node.getTextContent().replaceAll("[\\s]{3,}", " ");
					index1 = jsonValue.indexOf("sn");
					if (index1 >= 0) {
						String jsonValue1 = jsonValue.substring(index1);// 得到json字符串
						jsonValue1 = Formatter.getJson(jsonValue);
						if (!"".equals(jsonValue1)) {
							JSONObject productObject1 = JSONObject.parseObject(jsonValue1);
							try {
								map.put("skuid", productObject1.getString("productId"));
							} catch (Exception ex) {
							}
							try {
								map.put("partNumber", productObject1.getString("partNumber"));
							} catch (Exception ex) {
							}
							try {
								map.put("catalogId", productObject1.getString("catalogId"));
							} catch (Exception ex) {
							}
							try {
								map.put("storeId", productObject1.getString("storeId"));
							} catch (Exception ex) {
							}
							try {
								map.put("currPrice", productObject1.getString("currPrice"));
							} catch (Exception ex) {
							}
							try {
								map.put("bookName", productObject1.getString("bookName"));
							} catch (Exception ex) {
							}
						}
					}
				} catch (Exception ex) {

				}
				// 20131023添加，解析原始分类编码
				index3 = jsonValue.indexOf("snqa");
				if (index3 >= 0) {
					String jsonValue3 = jsonValue.substring(index3);// 得到json字符串
					jsonValue = Formatter.getJson(jsonValue3);
					if (!jsonValue.equals("")) {
						JSONObject productObject3 = JSONObject.parseObject(jsonValue);
						try {
							map.put("category1", productObject3.getString("category1"));
						} catch (Exception ex) {
						}
						try {
							map.put("categoryName1", productObject3.getString("categoryName1"));
						} catch (Exception ex) {
						}
						try {
							map.put("category2", productObject3.getString("category2"));
						} catch (Exception ex) {
						}
						try {
							map.put("categoryName2", productObject3.getString("categoryName2"));
						} catch (Exception ex) {
						}
						try {
							map.put("category3", productObject3.getString("category3"));
						} catch (Exception ex) {
						}
						try {
							map.put("categoryName3", productObject3.getString("categoryName3"));
						} catch (Exception ex) {
						}
					}
				}
			}
		}

		if (pageNode != null) {// 解析下一页url需要的参数
			String contents = pageNode.getTextContent().replaceAll("[\\s]{3,}", " ");
			index1 = contents.indexOf("param");
			jsonValue = contents.substring(index1);// 得到json字符串
			jsonValue = Formatter.getJson(jsonValue);
			if (!jsonValue.equals("")) {
				JSONObject productObject = JSONObject.parseObject(jsonValue);
				try {
					map.put("holdURL", productObject.getString("holdURL"));
				} catch (Exception ex) {
				}// 基础url
				try {
					map.put("currentPage", productObject.getString("currentPage"));
				} catch (Exception ex) {
				}// 当前页
				try {
					map.put("pageNumbers", productObject.getString("pageNumbers"));
				} catch (Exception ex) {
				}// 总页数
				try {
					map.put("numFound", productObject.getString("numFound"));
				} catch (Exception ex) {
				}// 商品总数量
			}

			fillMap(contents, "param.inventory", "inventory");
			fillMap(contents, "param.sortType", "sortType");
			fillMap(contents, "param.historyFilter", "historyFilter");
		}
	}

	private void fillMap(String contents, String param, String key) {
		int index1 = contents.indexOf(param);

		if (index1 >= 0) {
			index1 = contents.indexOf("\"", index1);
			int index2 = contents.indexOf("\"", index1 + 1);
			map.put(key, contents.substring(index1 + 1, index2));
		}

	}

	@Override
	public void setId() throws ProductIDIsNullException {
		Node idNode = TemplateUtil.getNode(root, idXPath);
		if (idNode != null) {
			String pid = idNode.getTextContent().trim();
			if (pid != null && !"".equals(pid)) {
				String id = sellerCode + pid;
				product.setPid(id);
				product.setOpid(pid);
			}
		}
		if (idNode == null) {
			throw new ProductIDIsNullException(url + ":未能获取商品id！！");
		}
	}

	@Override
	public void setProductName() throws ProductNameIsNullException {
		Node node = TemplateUtil.getNode(root, productNameXpath);
		if (node != null) {
			String productName = node.getFirstChild().getTextContent().replaceAll("\\s", "");
			if ("".equals(productName)) {
				productName = node.getTextContent().replaceAll("\\s", "");
			} else {
				product.setProductName(productName);
			}
		} else {
			String bookName = map.get("bookName");
			if (!StrUtil.isEmpty(bookName)) {
				product.setProductName(bookName);
			} else {
				new ProductNameIsNullException(url + "商品名称为空！" + productNameXpath + "不匹配 ");
			}

		}
	}

	/**
	 * 该价格是通过异步请求加载的，根据获取的id等参数拼凑去请求url，返回的json解析
	 */
	@Override
	public void setPrice() throws ProductPriceIsNullException {
		int index1 = 0;
		String price = "";
		String storeId = "";
		String catalogId = "";
		String partNumber = "";
		String sbToString = "";
		try {
			parseInit();
		} catch (Exception ex) {
			LOG.info("parseInit Error", ex);
		}
		try {
			String productId = map.get("skuid");
			productId = trimPrice(productId);
			if (productId != null && !"".equals(productId)) {
				storeId = map.get("storeId");
				catalogId = map.get("catalogId");
				partNumber = map.get("partNumber");
				if (!StrUtil.isEmpty(storeId) && !StrUtil.isEmpty(catalogId) && !StrUtil.isEmpty(partNumber)) {
					// http://product.suning.com/emall/csl_10052_10051_18076363_000000000105330482_9017_.html
					String priceUrl = "http://product.suning.com/emall/csl_" + storeId + "_" + catalogId + "_"
							+ productId + "_" + partNumber + "_9017_.html";
					sbToString = RequestUtil.getOfHttpClient(priceUrl).toString();
					if (sbToString != null && !StrUtil.isEmpty(sbToString)) {
						index1 = sbToString.indexOf("shopList");
						sbToString = sbToString.substring(index1);// 得到json字符串
						sbToString = Formatter.getJson(sbToString).replaceAll("[\\s]{1,}", "");
						String[] priceArry = sbToString.split(",");
						if (priceArry.length > 0) {
							for (int i = 0; i < priceArry.length; i++) {
								String priceValue = priceArry[i];
								if (priceValue.indexOf("productPrice") >= 0) {
									price = priceValue.replaceAll("[\"productPrice\":]", "");
									if (!StrUtil.isEmpty(price)) {
										product.setPrice(price);
										return;
									}
								}

							}
						}

						// JSONObject productObject = JSONObject.fromObject(sbToString);
						// if(productObject!=null){
						// LOG.info("==========3==========");
						// price=productObject.getString("productPrice");
						// LOG.info("price==="+price);
						// if (price == null || price.isEmpty()) {
						// throw new ProductPriceIsNullException(url + ",组装的"
						// + priceUrl + "未能获取商品销售价格！setPrice异常不匹配");
						// }
						// }
					}
				}
			}
		} catch (Exception ex) {
			LOG.info(url + "  url未能获取商品销售价格！setPrice异常不匹配!");
			ex.printStackTrace();
		}
		if (StrUtil.isEmpty(price)) {
			Node priceNode = TemplateUtil.getNode(root, priceXpath);
			if (priceNode != null) {
				price = priceNode.getTextContent();

			} else {
				price = map.get("currPrice");
			}
			price = trimPrice(price);
			product.setPrice(price);
		}
	}

	@Override
	public void setMparams() {
		String value = TemplateUtil.getMParas(root, mparasXpath);
		if (value != null && !"".equals(value)) {
			value = TemplateUtil.formatMparams(value);
			value = value.replaceAll("纠错", "");
		} else {
			value = "无商品参数.........";
		}
		product.setMparams(value);
	}

	/**
	 * @param baseUrl
	 * @param content
	 * @return
	 * @Override public List<imageProduct> nextUrlList(URL baseUrl,String content) {
	 *           List<imageProduct> urlList = new ArrayList<imageProduct>(); Set<imageProduct>
	 *           urlSet = new LinkedHashSet<imageProduct>(); URL urls = null; String productUrl="";
	 *           String smallPic=""; try
	 *           {//http://m.suning.com/emall/snmwprd_10052_10051_18250394_.html Pattern
	 *           productFlag=
	 *           Pattern.compile("http://m.suning.com/emall/([a-zA-Z]+)_([0-9_]+).html.*?"); Matcher
	 *           productMatch=productFlag.matcher(baseUrl.toString()); if(productMatch.find()){ Node
	 *           imgNode=TemplateUtil.getNode(root, "//UL[@class='slide_ul']/LI[1]/IMG/@src");
	 *           if(imgNode != null){ smallPic=imgNode.getTextContent().trim(); } try{
	 *           if(!StrUtil.isEmpty(baseUrl.toString()) && !StrUtil.isEmpty(smallPic)){
	 *           imageProduct image=new imageProduct(smallPic,baseUrl.toString());
	 *           urlSet.add(image); LOG.info("imageProduct :"+image); } }catch(Exception e){ }
	 *           }else{ Document doc = Jsoup.parse(content); Elements
	 *           elements=doc.getElementsByTag("A"); if(elements.size() > 0){ for (int i = 0; i <
	 *           elements.size(); i++) { Element urlListEle = elements.get(i);
	 *           productUrl=urlListEle.attr("href");
	 *           smallPic=urlListEle.getElementsByTag("img").attr("src");
	 *           if(!smallPic.startsWith("http://") || smallPic.startsWith("http://script."))
	 *           continue; try{ urls = URLUtil.resolveURL(baseUrl, productUrl);
	 *           if(!StrUtil.isEmpty(urls.toString()) && !StrUtil.isEmpty(smallPic)){ imageProduct
	 *           image=new imageProduct(smallPic,urls.toString()); urlSet.add(image);
	 *           LOG.info("imageProduct :"+image); } }catch(Exception e){ } } } } } catch (Exception
	 *           e) { e.printStackTrace(); } urlList.addAll(urlSet); return urlList; }
	 */

	/**
	 * 20131023添加原始分类
	 */
	@Override
	public void setOriCatCode() {
		StringBuffer buffer = new StringBuffer();
		String category1 = map.get("category1");
		if (category1 != null && !"".equals(category1)) {
			buffer.append(category1 + "#");
		}
		String category2 = map.get("category2");
		if (category2 != null && !"".equals(category2)) {
			buffer.append(category2 + "#");
		}
		String category3 = map.get("category3");
		if (category3 != null && !"".equals(category3)) {
			buffer.append(category3);
		}
		product.setOriCatCode(buffer.toString());
	}

	/**
	 * 20131102添加
	 */
	@Override
	public void setCategory() {
		if (!"".equals(categoryXpath)) {
			NodeList categoryNodeList = TemplateUtil.getNodeList(root, categoryXpath);
			if (categoryNodeList.getLength() > 0) {
				String categoryValue = "";
				StringBuffer categoryBuffer = new StringBuffer();
				for (int i = 0; i < categoryNodeList.getLength(); i++) {
					categoryValue = categoryNodeList.item(i).getTextContent().replaceAll("[\\s]{1,}", "");
					if (i != 0) {
						categoryBuffer.append("[xm99]");
					}
					categoryBuffer.append(categoryValue);
				}
				if (categoryBuffer.length() > 0) {
					categoryValue = categoryBuffer.toString();
					categoryValue = categoryValue.replaceAll("首页\\[xm99\\]", "");
					product.setCategory(categoryValue);
				}
			}
		}
	}

	/**
	 * 20131102添加
	 */
	@Override
	public void setCheckOriCatCode() {
		StringBuffer checkOriCatCodeBuffer = new StringBuffer();
		String categoryName1 = map.get("categoryName1");
		if (categoryName1 != null && !"".equals(categoryName1)) {
			checkOriCatCodeBuffer.append(categoryName1 + "#");
		}
		String categoryCode1 = map.get("category1");
		if (categoryCode1 != null && !"".equals(categoryCode1)) {
			checkOriCatCodeBuffer.append(categoryCode1 + "[xm99]");
		}
		String categoryName2 = map.get("categoryName2");
		if (categoryName2 != null && !"".equals(categoryName2)) {
			checkOriCatCodeBuffer.append(categoryName2 + "#");
		}
		String categoryCode2 = map.get("category2");
		if (categoryCode2 != null && !"".equals(categoryCode2)) {
			checkOriCatCodeBuffer.append(categoryCode2 + "[xm99]");
		}
		String categoryName3 = map.get("categoryName3");
		if (categoryName3 != null && !"".equals(categoryName3)) {
			checkOriCatCodeBuffer.append(categoryName3 + "#");
		}
		String categoryCode3 = map.get("category3");
		if (categoryCode3 != null && !"".equals(categoryCode3)) {
			checkOriCatCodeBuffer.append(categoryCode3);
		}

		if (checkOriCatCodeBuffer.length() > 0) {
			String checkOriCatCodeValue = checkOriCatCodeBuffer.toString();
			this.product.setCheckOriCatCode(checkOriCatCodeValue);
		}
	}

	/**
	 * 20131108添加，品牌+型号
	 */
	@Override
	public void setShortName() {
		String brandValue = "";
		String modelValue = "";
		String shortNameByMparams = this.product.getMparams();
		if (shortNameByMparams != null || !"".equals(shortNameByMparams)) {
			String[] shortNameArry = shortNameByMparams.split("\\[xm99\\]");
			if (shortNameArry.length > 0) {
				for (int i = 0; i < shortNameArry.length; i++) {
					String shortNameStr = shortNameArry[i].replaceAll("[\\s]{1,}", "").trim();
					if (shortNameStr.indexOf("品牌") >= 0) {
						String[] shortNameStrArryByBrand = shortNameStr.split("=");
						if ("品牌".equals(shortNameStrArryByBrand[0])) {
							brandValue = shortNameStrArryByBrand[1];
						}

					}
					if (shortNameStr.indexOf("型号") >= 0) {
						String[] shortNameStrArryByModel = shortNameStr.split("=");
						if ("型号".equals(shortNameStrArryByModel[0])) {
							modelValue = shortNameStrArryByModel[1];
						}
					}

				}
			}

			if (brandValue != null || !"".equals(brandValue)) {
				String shortName = brandValue + " " + modelValue;
				this.product.setShortName(shortName);
				this.product.setShortNameDetail(shortName);
			}

		}

	}

	/**
	 * 20131108添加
	 */
	@Override
	public void setBrand() {
		if (!"".equals(brandXpath)) {
			Node brandNode = TemplateUtil.getNode(root, brandXpath);
			if (brandNode != null) {
				String brand = brandNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();
				if (brand != null && !"".equals(brand)) {
					brand = brand.replaceAll("（", "(").replaceAll("）", ")");
					if (brand.indexOf("(") >= 0 && brand.indexOf(")") >= 0) {
						brand = brand.replaceAll("\\(", "\\[xm99\\]").replaceAll("\\)", "");
					}
					product.setBrand(brand);
				}
			}
		}
	}

	/**
	 * 20131116添加：限时抢购的截止时间
	 */
	@Override
	public void setLimitTime() {
		String limitTime = "";
		String limitTimeXpath = "//INPUT[@id='endTime']/@value";
		Node limitTimeNode = TemplateUtil.getNode(root, limitTimeXpath);
		if (limitTimeNode != null) {
			String limitTimeValue = limitTimeNode.getTextContent().trim();
			if (!"".equals(limitTimeValue)) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date sd = sdf.parse(limitTimeValue);
					long limitTimeLong = sd.getTime();
					limitTime = String.valueOf(limitTimeLong);
				} catch (ParseException e) {
					LOG.info("The setLimitTime() Is ERROR !" + url);
				}
			}
		}
		if (!"".equals(limitTime)) {
			this.product.setLimitTime(limitTime);
		} else {
			this.product.setDiscountPrice("0");
			this.product.setLimitTime("0");
		}
	}

	/**
	 * 20131116添加：抢购价
	 */
	@Override
	public void setDiscountPrice() {
		String discountPrice = "";
		if (!discountPriceXpath.isEmpty()) {
			Node discountPriceNode = TemplateUtil.getNode(root, priceXpath);
			if (discountPriceNode != null) {
				discountPrice = trimPrice(discountPriceNode.getTextContent());
				product.setDiscountPrice(discountPrice);
			}
		}

	}

	public void initCommentContent() {
		try {
			String commentPid = map.get("partNumber");
			if (!StrUtil.isEmpty(commentPid)) {

				String commentSecondUrl = "http://zone.suning.com/review/wcs_review/" + commentPid
						+ "-0-1---pinglunLoadDataCallback.html";
				String commentContentValue = RequestUtil.getOfHttpURLConnection(commentSecondUrl, "utf-8").toString();
				if (!StrUtil.isEmpty(commentContentValue)) {
					int index = commentContentValue.indexOf("pinglunLoadDataCallback");
					if (index >= 0) {
						commentContentValue = commentContentValue.substring(commentContentValue.indexOf("{"),
								commentContentValue.lastIndexOf("}") + 1);
					}
					JSONObject jsonPrim = JSONObject.parseObject(commentContentValue);
					String htmlDom = jsonPrim.getString("htmlDom");
					// System.out.println(htmlDom);
					DocumentFragment commentDoc = RequestUtil.getDocumentFragmentByString(htmlDom, "utf-8");
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
		
		String id[] = new String[7];
		String name[] = new String[7];
		String content[] = new String[7];
		// 1-7
		for (int i = 1; i <= 7; i++) {
			id[i - 1] = "//DIV[@id='pingjia_tab_box']/DIV/INPUT[contains(@id,'productReviewId')]["+i+"]/@value";
			name[i - 1] = "//DIV[@id='pingjia_tab_box']/DIV/DIV[@class='comment-item fix']["+i+"]/DIV[@class='user-face']/P/SPAN";
			content[i - 1] = "//DIV[@id='pingjia_tab_box']/DIV/DIV[@class='comment-item fix']["+i+"]//descendant::DIV[@class='content']/P[1]";
		}
		
		try {
			List<Comment> clist = new ArrayList<Comment>();
			for (int i = 0; i < 7; i++) {
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

		String cid = idNode.getTextContent().replaceAll("[\\s]{2,}", "").trim();
		String name = nameNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();
		String content = contentNode.getTextContent().replaceAll("[\\s]{1,}", "").trim();

		if (StringUtils.isNotEmpty(cid) && StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(content)) {
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
		ParserSuningNew suning = new ParserSuningNew();
		suning.test1();

		// String url="http://product.suning.com/104643997.html";
		// String value=RequestUtil.getPageContent(url, "utf-8");
		// System.out.println(value);
		// System.out.println(new Date("1379399721367"));
	}

	public void test1() throws Exception {
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\Suning.html"), "utf-8");

		ParserSuningNew test = new ParserSuningNew(root);
		test.product = new OriProduct();

		try {
			test.setId();
			System.out.println("id=====>: " + test.product.getPid() + ">>>>" + test.product.getOpid());
		} catch (ProductIDIsNullException e) {
			e.printStackTrace();
		}
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
		// test.setClassic();
		// System.out.println("导航=====>: "+test.product.getClassic());
		//
		// test.setBrand();
		// System.out.println("品牌=====>："+ test.product.getBrand());
		//
		// test.setContents();
		// System.out.println("content=====>："+test.product.getContents());
		//
		// test.setOrgPic();
		// System.out.println("图片=====>："+test.product.getOrgPic());
		//
		// test.setMparams();
		// System.out.println("mp内容=====> ："+test.product.getMparams());
		//
		// test.setTitle();
		// System.out.println("标题=====>："+test.product.getTitle());
		//
		// test.setKeyword();
		// System.out.println("关键字=====>: "+ test.product.getKeyword());
		//
		// test.setIsCargo();
		// System.out.println("是否有货====》:"+test.product.getIsCargo());
		//
		// test.setCategory();
		// System.out.println("中文原始分类====》:" + test.product.getCategory());
		//
		// test.setOriCatCode();
		// System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
		//
		// test.setCheckOriCatCode();
		// System.out.println("中文混合原始分类====》:" + test.product.getCheckOriCatCode());
		//
		test.setPrice();
		System.out.println("当前价格=====>:" + test.product.getPrice());

		test.setProductName();
		System.out.println("商品名称======>:" + test.product.getProductName());
		//
		// test.setShortName();
		// System.out.println("短名称=====>："+ test.product.getShortName());
		//
		// test.setshortNameDetail();
		// System.out.println("第二种商品短名称======>:" + test.product.getShortNameDetail());

		test.setMaketPrice();
		System.out.println("市场价格=====>:" + test.product.getMaketPrice());

		test.setLimitTime();
		System.out.println("折扣活动截止时间=====>:" + test.product.getLimitTime());

		test.setDiscountPrice();
		System.out.println("活动价格=====>:" + test.product.getDiscountPrice());

		test.setD1ratio();
		System.out.println("活动销售价格的折扣=====>:" + test.product.getD1ratio());

		test.setD2ratio();
		System.out.println("当前销售价格的折扣=====>:" + test.product.getD2ratio());
		System.out.println("当前价格=====>:" + test.product.getPrice());
		// test.setTotalComment();
		// System.out.println("总评论数=====>:" + test.product.getTotalComment());
		//
		// test.setCommentStar();
		// System.out.println("评论星级数=====>:" + test.product.getCommentStar());
		//
		// test.setCommentPercent();
		// System.out.println("评论百分数=====>:" + test.product.getCommentPercent());

		// String t = test.nextUrl(new URL("http://search.suning.com/emall/strd.do?ci=258004"));
		// System.out.println("==下一页="+t);
		//
		//
		// String url = "http://product.suning.com/102569029.html";
		// boolean b = test.isProductPage(url);
		// System.out.println("是不是产品页："+b);

		// URL url;
		// try {
		// url = new URL("http://search.suning.com/emall/strd.do?ci=247504");
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
