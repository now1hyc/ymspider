package com.ym.framework.implement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

import com.ym.framework.interfaces.ICrawlerProduct;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserTemplateFactory;
import com.ym.nutch.parse.template.TemplateParser;
import com.ym.nutch.parse.template.XMNutchTemplateSettor;
import com.ym.nutch.parse.template.XMNutchTemplateSettor.Ant;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;

public class CrawlerProductService implements ICrawlerProduct {

	public static final Logger			LOG						= LoggerFactory.getLogger(CrawlerProductService.class);

	// 加载配置文件
	private static HashMap<String, Ant>	antByMerchantSettor		= XMNutchTemplateSettor.getCodeName_settor();

	private static String[]				productsColumns			= new String[] { "classic", "shortName", "productName",
			"title", "keyword", "price", "maketPrice", "discountPrice", "contents", "mparams", "orgPic", "sellerCode",
			"seller", "product_url", "pid", "brand", "creatTime", "isCargo", "smallpic", "bigpic", "status", "opCode",
			"category", "oriCatCode", "checkOriCatCode", "shortNameDetail", "d1ratio", "d2ratio", "limitTime",
			"totalComment", "commentStar", "commentPercent", "theThird", "hasComment" };

	// 商品增量表
	private static String[]				deltaProductColumns		= new String[] { "oriPicUrl", "prodUrl", "productName",
			"price", "isCargo", "isNew", "isDownload", "creatTime", "status", "updateTime" };

	private static String[]				productsUpdateColumns	= new String[] { "productName", "price", "maketPrice",
			"discountPrice", "contents", "mparams", "orgPic", "product_url", "updateTime", "isCargo", "brand",
			"category", "oriCatCode", "checkOriCatCode", "shortNameDetail", "d1ratio", "d2ratio", "limitTime",
			"totalComment", "commentStar", "commentPercent", "hasComment" };

	@Override
	public OriProduct crawlerProduct(String sellerCode, String productUrl) {
		OriProduct product = null;
		LOG.info("fetcher url start! productUrl=" + productUrl);
		long beginTime = System.currentTimeMillis();
		if (!StrUtil.isEmpty(sellerCode)) {
			try {
				Ant ant = antByMerchantSettor.get(sellerCode);
				if (ant != null) {
					String charset = ant.getUrl_charset();
					if (!StrUtil.isEmpty(productUrl)) {
						String content = "";
						productUrl = formatUrl(sellerCode, productUrl);
						long beginContentTime = System.currentTimeMillis();
						content = RequestUtil.getPageContent(productUrl, charset);
						long endConetTime = System.currentTimeMillis();
						if (content != null && !StrUtil.isEmpty(content)) {
							LOG.info("fetch cost " + (endConetTime - beginContentTime) + "ms. "
									+ (content.length() / 1024) + "Kb. code=" + sellerCode + ",url=" + productUrl);
							try {
								product = getParse(content, productUrl, ant);
								long endTime = System.currentTimeMillis();
								LOG.info("End parse Product success,timecost=" + (endTime - beginTime) + "ms");
								return product;
							} catch (Exception ex) {
								LOG.error("Crawl Product Details Is ERROR ! url=" + productUrl);
								LOG.error("ERROR：" + ex);
								return product;
							}

						} else {
							LOG.info("Request the network fails! content = null! url-->" + productUrl);
						}
					} else {
						LOG.info("The Fetcher Url Get Failure By UrlQueue !");
					}
				} else {
					LOG.info("Unable to generate code based on Ant Object , url-->" + productUrl);
				}
			} catch (Exception e) {
				LOG.info("Parser Product Url Is ERROR! url-->" + productUrl);
				return product;
			}
		} else {
			LOG.info("code is null ,fetcher over! url-->" + productUrl);
		}
		return product;
	}

	public OriProduct getParse(String content, String url, Ant ant) {
		URL base = null;
		try {
			base = new URL(url);
		} catch (MalformedURLException e) {
		}
		LOG.info("enter HtmlParser ,url=" + url);
		DocumentFragment root = null;
		try {
			root = RequestUtil.getDocumentFragmentByString(content, ant.getContent_charset());
		} catch (Exception e) {
			LOG.error("documentFragment root is error!!", e.getCause());
			return null;
		}

		TemplateParser templateParser = null;
		try {
			// TODO 获取解析模板 提取商品详情信息并入库
			templateParser = ParserTemplateFactory.getTemplateParser(base, root, content);
			if (templateParser == null) {
				throw new RuntimeException("templateParser not found");
			}
			OriProduct product = getProduct(content, url, base, templateParser);
			if (product != null) {
				return product;
			}
		} catch (Exception e) {
			LOG.error("templateParser is error!!", e.getCause());
			return null;
		}
		return null;
	}

	/**
	 * 获取商品详情信息，并将商品入库
	 */
	private OriProduct getProduct(String content, String url, URL base, TemplateParser templateParser) {
		LOG.info("url=" + base.toString());
		OriProduct product = null;
		String rowKey = "";
		try {
			// 如果是商品页，调用模板解析内容；
			if (templateParser.isProductPage(url.toLowerCase())) {
				product = templateParser.getProductInformation(url);
				rowKey = templateParser.getProductRowKey();
				if (product != null) {
					LOG.info("Product Is Not Null! rowKey=" + rowKey);
					return product;
				}
			}
		} catch (Exception e) {
			LOG.error("parse exception:" + url, e);
			setIsCargoByNoProduct(rowKey);
		}
		return product;
	}

	// 此处是把手机版的url转为PC端的url

	public String formatUrl(String selleCode, String productUrl) {
		// 手机版： http://m.jd.com/product/647812.html
		// 电脑版： http://item.jd.com/647812.html
		try {
			if ("1005".equals(selleCode)) {
				String pcProductHost = "http://item.jd.com/";
				if (productUrl.contains("m.jd.com/product/")) {
					String proPid = productUrl.substring(productUrl.indexOf("/product/") + 9,
							productUrl.indexOf(".html"));
					if (!StrUtil.isEmpty(proPid)) {
						productUrl = pcProductHost + proPid + ".html";
					}
				}
			} else if ("1011".equals(selleCode)) {
				// 手机版：http://www.amazon.cn/gp/aw/d/B003GIS2IK
				// 电脑版：http://www.amazon.cn/dp/B003GIS2IK
				if (productUrl.contains("/gp/aw/d/")) {
					productUrl = productUrl.replaceAll("/gp/aw/d/", "/dp/");
				}
			} else if ("1012".equals(selleCode)) {
				// 手机版：http://m.yhd.com/product/8809_1_1_1
				// 电脑版：http://item.yhd.com/item/72654
				if (productUrl.indexOf("m.yhd.com/product/") >= 0) {
					productUrl = productUrl.replaceAll("m.yhd.com/", "item.yhd.com/");
					// 电脑版： http://item.yhd.com/item/4964089
					// 手机版： http://m.yhd.com/mw/productsquid/4897419/1/20/
				} else if (productUrl.matches("http://m.yhd.com/mw/([a-zA-Z]+)/([0-9]+).*?")) {
					String contentByPro = RequestUtil.getPageContent(productUrl, "utf-8");
					int index = contentByPro.indexOf("(new Parameter(") + 15;
					if (index > 0) {
						String proId = contentByPro.substring(index, contentByPro.indexOf("function backlist()"))
								.trim();
						proId = proId.substring(proId.indexOf(",\"") + 2, proId.indexOf("\"));"));
						productUrl = "http://item.yhd.com/item/" + proId;
					}
				}
			} else if ("1008".equals(selleCode)) {
				// /http://m.dangdang.com/touch/product.php?pid=1118086621&sid=153814ae1dad17ef&recoref=category
				if (productUrl.indexOf("m.dangdang.com/product.php?pid=") >= 0
						|| productUrl.indexOf("m.dangdang.com/touch/product.php?pid=") >= 0) {
					String dangPerfix = "http://product.dangdang.com/";
					String dangPid = productUrl.substring(productUrl.indexOf("?pid=") + 5, productUrl.indexOf("&"));
					productUrl = dangPerfix + dangPid + ".html";
				}
			} else if ("1001".equals(selleCode)) {
				// 电脑版：http://www.suning.com/emall/prd_10052_14656_-7_4700801_.html
				// 手机版：http://m.suning.com/emall/snmwprd_10052_14656_4700801_.html
				if (productUrl.matches("http://m.suning.com/emall/snmwprd([0-9_]+).html.*?")) {
					String suningPerfix = "http://www.suning.com/emall/prd_";
					String tempUrl = productUrl.substring(productUrl.indexOf("/snmwprd_") + 9,
							productUrl.indexOf(".html"));
					String[] tempArry = tempUrl.split("_");
					if (tempArry.length > 0) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < tempArry.length; i++) {
							if (i == 2) {
								sb.append("-7_");
							}
							String tempValue = tempArry[i];
							sb.append(tempValue + "_");

						}
						productUrl = suningPerfix + sb.toString() + ".html";
					}
				}
			} else if ("1071".equals(selleCode)) {
				// 电脑版：http://www.vip.com/detail-135804-17746151.html
				// 手机版：http://m.vip.com/product-135804-17746151.html
				if (productUrl.matches("(http://m.vip.com/product-).*?")) {
					productUrl = productUrl.replaceAll("m.vip.com/", "www.vip.com/")
							.replaceAll("/product-", "/detail-");
				}
			} else if ("1007".equals(selleCode)) {
				if (productUrl.matches("http://m.newegg.cn/Product/.*?")) {
					productUrl = productUrl.replaceAll("m.newegg.cn/", "www.newegg.cn/");
				}
			} else if ("1031".equals(selleCode)) {
				Pattern juMeiPattern1 = Pattern
						.compile("http\\://m\\.jumei\\.com/i/mobilewap/mall\\_view\\?product\\_id\\=([0-9]+).*?");
				Matcher juMeiMatcher1 = juMeiPattern1.matcher(productUrl.toLowerCase());
				// http://m.jumei.com/i/MobileWap/deal_view?hash_id=bj140122p2925
				// -->http://bj.jumei.com/i/deal/bj140122p2925.html
				Pattern juMeiPattern2 = Pattern
						.compile("http\\://m\\.jumei\\.com/i/mobilewap/deal\\_view\\?hash\\_id\\=([0-9a-zA-Z]+).*?");
				Matcher juMeiMatcher2 = juMeiPattern2.matcher(productUrl.toLowerCase());
				String pid = "";
				if (juMeiMatcher1.find()) {
					pid = juMeiMatcher1.group(1);
					if (!StrUtil.isEmpty(pid)) {
						productUrl = "http://mall.jumei.com/product_" + pid + ".html";
					}
				} else if (juMeiMatcher2.find()) {
					pid = juMeiMatcher2.group(1);
					if (!StrUtil.isEmpty(pid)) {
						productUrl = "http://bj.jumei.com/i/deal/" + pid + ".html";
					}
				}

				// http://m.yixun.com/t/detail/index.html?pid=712354
			} else if ("1009".equals(selleCode)) {
				String pid = "";
				Pattern yiXunPattern1 = Pattern
						.compile("http\\://m\\.yixun\\.com/t/detail/index\\.html\\?pid\\=([0-9]+).*?");
				Matcher yiXunMatcher1 = yiXunPattern1.matcher(productUrl.toLowerCase());
				// http://m.yixun.com/t/list/index.html?cid=705882t705891&option=55e7060&cateword=格力#detail({"pid":"536605","_":1390459002846})
				Pattern yiXunPattern2 = Pattern
						.compile("http\\://m\\.yixun\\.com/t/list/index\\.html\\?cid\\=([0-9a-zA-Z].*?)#detail([0-9a-zA-Z\":,_()]).*?");
				Matcher yiXunMatcher2 = yiXunPattern2.matcher(productUrl.toLowerCase());
				// http://m.yixun.com/t/channel/index.html#detail({"pid":"1461627","channelId":"300","_":1390468082873})
				// 晚市
				// http://m.yixun.com/t/channel/morning.html#detail(%7B%22pid%22%3A%2279973%22%2C%22channelId%22%3A%22100%22%2C%22_%22%3A1392346625178%7D)
				Pattern yiXunPattern3 = Pattern
						.compile("(http\\://m\\.yixun\\.com/t/channel/)([a-zA-Z].*?)\\.html\\#detail.*?");
				Matcher yiXunMatcher3 = yiXunPattern3.matcher(productUrl.toLowerCase());
				if (yiXunMatcher1.find()) {
					pid = yiXunMatcher1.group(1);
					productUrl = "http://item.yixun.com/item-" + pid + ".html";
					;
				} else if (yiXunMatcher2.find() || yiXunMatcher3.find()) {
					int index1 = productUrl.indexOf("\"pid\":") + 7;
					int index2 = productUrl.indexOf(",") - 1;
					int index3 = productUrl.indexOf("pid%22%3A%22") + 12;
					int index4 = productUrl.indexOf("%22%2C%22");
					if (index1 >= 0 && index2 >= 0) {
						pid = productUrl.substring(index1, index2);
					}
					if (index3 >= 0 && index4 >= 0) {
						pid = productUrl.substring(index3, index4);
					}
					productUrl = "http://item.yixun.com/item-" + pid + ".html";
				}

			} else if ("1030".equals(selleCode)) {
				// 电脑版：http://product.lefeng.com/product/211870.html
				// 手机版：http://m.lefeng.com/index.php/product/index/pid/211870
				Pattern leFengPattern = Pattern.compile("http://m.lefeng.com/index.php/product/index/pid/([0-9]+).*?");
				Matcher leFengMatcher = leFengPattern.matcher(productUrl.toLowerCase());
				if (leFengMatcher.find()) {
					String pid = leFengMatcher.group(1);
					productUrl = "http://product.lefeng.com/product/" + pid + ".html";
					;
				}
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName() + ":手机版商品url转为PC版商品url失败! url-->" + productUrl);
			e.printStackTrace();
		}
		return productUrl;
	}

	/**
	 * 将product对象先返回服务端在入库操作，以减少时间
	 * 
	 * @param product
	 */
	@Override
	public void productStorage(OriProduct product) {
		String rowKey = "";
		String productUrl = "";
		boolean isAdd = true, dbOk = false;
		if (product != null) {
			rowKey = product.getPid();
			productUrl = product.getProduct_url();
			/**
			 * 根据id查询数据库,如果查询记录存在(也就是存在pid值),则说明该数据是需要更新的记录,否则为添加记录。 当查询时出现异常信息,则认为该操作为添加操作。
			 */
			try {
				LOG.info("qry " + rowKey + " product from hbase: " + product);
				Map<String, String> map = BaseDao.queryOne("products", product.getId());
				if (map.get("pid") != null && !map.get("pid").equals("")) {
					isAdd = false;
				}
			} catch (Exception ex) {
				isAdd = true;
				LOG.error(rowKey, ex);
			}
			int hasComment = product.getHasComment(); // 商品评论内容的状态：有效:1;无效:0
			if (isAdd) {
				String[] productsValues = { product.getClassic(), product.getShortName(), product.getProductName(),
						product.getTitle(), product.getKeyword(), product.getPrice(), product.getMaketPrice(),
						product.getDiscountPrice(), product.getContents(), product.getMparams(), product.getOrgPic(),
						product.getSellerCode(), product.getSeller(), product.getProduct_url(), product.getPid(),
						product.getBrand(), product.getCreatTime(), "" + product.getIsCargo(), "", "",
						"" + product.getStatus(), product.getOpCode(), product.getCategory(), product.getOriCatCode(),
						product.getCheckOriCatCode(), product.getShortNameDetail(), product.getD1ratio(),
						product.getD2ratio(), product.getLimitTime(), product.getTotalComment(),
						product.getCommentStar(), product.getCommentPercent(), product.getTheThird(),
						product.getHasComment() };
				long begin = System.currentTimeMillis();
				try {
					dbOk = BaseDao.insertData("products", product.getId(), "properties", productsColumns,
							productsValues);
					long end = System.currentTimeMillis();
					LOG.info(String.format("Adding:%s save products success,timecost:%sms,url:%s", rowKey, end - begin,
							productUrl));
				} catch (Exception e) {
					long end = System.currentTimeMillis();
					LOG.info(String.format("Adding:%s save products error,timecost:%sms,url:%s", rowKey, end - begin,
							productUrl), e);
				}
				if (dbOk) {
					String[] deltaProductValues = { product.getOrgPic(), product.getProduct_url(),
							product.getProductName(), product.getPrice(), product.getIsCargo() + "", "1", "0",
							"" + System.currentTimeMillis(), "1", "" + System.currentTimeMillis() };

					begin = System.currentTimeMillis();
					try {
						BaseDao.insertData("deltaProduct", product.getId(), "properties", deltaProductColumns,
								deltaProductValues);
						long end = System.currentTimeMillis();
						LOG.info(String.format("Adding:%s save deltaProduct success,timecost:%sms,url:%s", rowKey, end
								- begin, productUrl));
					} catch (Exception e) {
						long end = System.currentTimeMillis();
						LOG.info(
								String.format("Adding:%s save deltaProduct error,timecost:%sms,url:%s", rowKey, end
										- begin, productUrl), e);
					}

					if (!StrUtil.isEmpty(hasComment) && !"0".equals(hasComment)) {
						String[] commentsColumns = new String[] { "creatTime", "state",
								product.getCommentOne().getCommentId(), product.getCommentTwo().getCommentId(),
								product.getCommentThree().getCommentId(), product.getCommentFour().getCommentId(),
								product.getCommentFive().getCommentId(), product.getCommentSix().getCommentId(),
								product.getCommentSeven().getCommentId(), product.getCommentEight().getCommentId(),
								product.getCommentNine().getCommentId(), product.getCommentTen().getCommentId(),
								product.getCommentEleven().getCommentId(), product.getCommentTwelve().getCommentId(),
								product.getCommentThirteen().getCommentId(),
								product.getCommentFourteen().getCommentId(),
								product.getCommentFifteen().getCommentId(), product.getCommentSixteen().getCommentId(),
								product.getCommentSeventeen().getCommentId(),
								product.getCommentEighteen().getCommentId(),
								product.getCommentNineteen().getCommentId(), product.getCommentTwenty().getCommentId() };

						String[] commentsValues = { product.getCreatTime(), hasComment,
								product.getCommentOne().getCommentContent(),
								product.getCommentTwo().getCommentContent(),
								product.getCommentThree().getCommentContent(),
								product.getCommentFour().getCommentContent(),
								product.getCommentFive().getCommentContent(),
								product.getCommentSix().getCommentContent(),
								product.getCommentSeven().getCommentContent(),
								product.getCommentEight().getCommentContent(),
								product.getCommentNine().getCommentContent(),
								product.getCommentTen().getCommentContent(),
								product.getCommentEleven().getCommentContent(),
								product.getCommentTwelve().getCommentContent(),
								product.getCommentThirteen().getCommentContent(),
								product.getCommentFourteen().getCommentContent(),
								product.getCommentFifteen().getCommentContent(),
								product.getCommentSixteen().getCommentContent(),
								product.getCommentSeventeen().getCommentContent(),
								product.getCommentEighteen().getCommentContent(),
								product.getCommentNineteen().getCommentContent(),
								product.getCommentTwenty().getCommentContent() };
						begin = System.currentTimeMillis();
						try {
							BaseDao.insertData("comments", product.getId(), "properties", commentsColumns,
									commentsValues);
							long end = System.currentTimeMillis();
							LOG.info(String.format("Adding:%s save comments success,state=%s,timecost:%sms,url:%s",
									rowKey, hasComment, end - begin, productUrl));
						} catch (Exception e) {
							long end = System.currentTimeMillis();
							LOG.info(
									String.format("Adding:%s save comments error,timecost:%sms,url:%s", rowKey, end
											- begin, productUrl), e);
						}
					}
				}

			} else {// update
				// 入库两个表操作
				String updateTime = "" + System.currentTimeMillis();
				String httpTime = "";
				if (StrUtil.isEmpty(httpTime)) {
					httpTime = updateTime;
				}
				long begin = System.currentTimeMillis();
				String[] productsValues = { product.getProductName(), product.getPrice(), product.getMaketPrice(),
						product.getDiscountPrice(), product.getContents(), product.getMparams(), product.getOrgPic(),
						product.getProduct_url(), updateTime, "" + product.getIsCargo(), product.getBrand(),
						product.getCategory(), product.getOriCatCode(), product.getCheckOriCatCode(),
						product.getShortNameDetail(), product.getD1ratio(), product.getD2ratio(),
						product.getLimitTime(), product.getTotalComment(), product.getCommentStar(),
						product.getCommentPercent(), product.getHasComment() };
				try {
					dbOk = BaseDao.insertData("products", product.getId(), "properties", productsUpdateColumns,
							productsValues);
					long end = System.currentTimeMillis();
					LOG.info(String.format("Append:%s update products success,timecost:%sms,url:%s", rowKey, end
							- begin, productUrl));
				} catch (Exception e) {
					long end = System.currentTimeMillis();
					LOG.info(String.format("Append:%s update products error,timecost:%sms,url:%s", rowKey, end - begin,
							productUrl), e);
				}
				if (dbOk) {
					begin = System.currentTimeMillis();
					try {
						BaseDao.insertData("priceHistory", product.getId() + updateTime, "properties", new String[] {
								"price", "updateTime" }, new String[] { product.getPrice(), httpTime });
						long end = System.currentTimeMillis();
						LOG.info(String.format("Append:%s save priceHistory success,timecost:%sms,url:%s", rowKey, end
								- begin, productUrl));
					} catch (Exception e) {
						long end = System.currentTimeMillis();
						LOG.info(
								String.format("Append:%s save priceHistory error,timecost:%sms,url:%s", rowKey, end
										- begin, productUrl), e);
					}

					String[] deltaProductValues = { product.getOrgPic(), product.getProduct_url(),
							product.getProductName(), product.getPrice(), product.getIsCargo() + "", "0", "0",
							"" + System.currentTimeMillis(), "1", "" + System.currentTimeMillis() };
					begin = System.currentTimeMillis();
					try {
						BaseDao.insertData("deltaProduct", product.getId(), "properties", deltaProductColumns,
								deltaProductValues);
						long end = System.currentTimeMillis();
						LOG.info(String.format("Append:%s save deltaProduct success,timecost:%sms,url:%s", rowKey, end
								- begin, productUrl));
					} catch (Exception e) {
						long end = System.currentTimeMillis();
						LOG.info(
								String.format("Append:%s save deltaProduct error,timecost:%sms,url:%s", rowKey, end
										- begin, productUrl), e);
					}

					if (!StrUtil.isEmpty(hasComment) && !"0".equals(hasComment)) {
						String[] updateCommentsColumns = new String[] { "updateTime", "state",
								product.getCommentOne().getCommentId(), product.getCommentTwo().getCommentId(),
								product.getCommentThree().getCommentId(), product.getCommentFour().getCommentId(),
								product.getCommentFive().getCommentId(), product.getCommentSix().getCommentId(),
								product.getCommentSeven().getCommentId(), product.getCommentEight().getCommentId(),
								product.getCommentNine().getCommentId(), product.getCommentTen().getCommentId(),
								product.getCommentEleven().getCommentId(), product.getCommentTwelve().getCommentId(),
								product.getCommentThirteen().getCommentId(),
								product.getCommentFourteen().getCommentId(),
								product.getCommentFifteen().getCommentId(), product.getCommentSixteen().getCommentId(),
								product.getCommentSeventeen().getCommentId(),
								product.getCommentEighteen().getCommentId(),
								product.getCommentNineteen().getCommentId(), product.getCommentTwenty().getCommentId() };

						String[] updateCommentsValues = { updateTime, hasComment,
								product.getCommentOne().getCommentContent(),
								product.getCommentTwo().getCommentContent(),
								product.getCommentThree().getCommentContent(),
								product.getCommentFour().getCommentContent(),
								product.getCommentFive().getCommentContent(),
								product.getCommentSix().getCommentContent(),
								product.getCommentSeven().getCommentContent(),
								product.getCommentEight().getCommentContent(),
								product.getCommentNine().getCommentContent(),
								product.getCommentTen().getCommentContent(),
								product.getCommentEleven().getCommentContent(),
								product.getCommentTwelve().getCommentContent(),
								product.getCommentThirteen().getCommentContent(),
								product.getCommentFourteen().getCommentContent(),
								product.getCommentFifteen().getCommentContent(),
								product.getCommentSixteen().getCommentContent(),
								product.getCommentSeventeen().getCommentContent(),
								product.getCommentEighteen().getCommentContent(),
								product.getCommentNineteen().getCommentContent(),
								product.getCommentTwenty().getCommentContent() };
						begin = System.currentTimeMillis();
						try {
							BaseDao.insertData("comments", product.getId(), "properties", updateCommentsColumns,
									updateCommentsValues);
							long end = System.currentTimeMillis();
							LOG.info(String.format("Adding:%s update comments success,state=%s,timecost:%sms,url:%s",
									rowKey, hasComment, end - begin, productUrl));
						} catch (Exception e) {
							long end = System.currentTimeMillis();
							LOG.info(
									String.format("Adding:%s update comments error,timecost:%sms,url:%s", rowKey, end
											- begin, productUrl), e);
						}
					}
				}
			}
		} else {
			LOG.info("product is null!");
		}
	}

	/**
	 * 如果该 rowkey有数据库记录存在,但是没有被解析成product对象,则说明该网页已经过期,将其数据库中设置为无货属性。
	 */
	public void setIsCargoByNoProduct(String rowkey) {
		if (rowkey != null && !"".equals(rowkey)) {
			try {
				Map<String, String> map = BaseDao.queryOne("products", rowkey);
				if (map.get("pid") != null && !map.get("pid").equals("")) {
					// 有记录,将其设置为无货状态
					BaseDao.insertData("products", rowkey, "properties", new String[] { "isCargo" },
							new String[] { "0" });
					LOG.info("HtmlParser->setIsCargoByNoProduct 商品解析为null……但已经更新库存状态为无货");
				}
			} catch (Exception ex) {
				LOG.info(rowkey + "==更新数据库isCargo属性时,发生异常");
			}

		}
	}

	public static void main(String[] args) {
		CrawlerProductService crawlerProductService = new CrawlerProductService();
		String sellerCode = "1031";
		String rawUrl = "http://bj.jumei.com/i/deal/bj140308p1333.html";
		Product product = crawlerProductService.crawlerProduct(sellerCode, rawUrl);
		System.out.println(product);
	}

}
