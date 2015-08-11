package com.ym.nutch.parse.template;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import com.ym.nutch.exception.ProductIDIsNullException;
import com.ym.nutch.exception.ProductNameIsNullException;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

/**
 * 解析器
 * 
 * @author user
 */
public abstract class TemplateParser implements XMParser {
	public Logger				LOG						= LoggerFactory.getLogger(TemplateParser.class);
	protected OriProduct		product					= null;
	protected String			url						= "";
	protected String			productRowKey			= "";
	protected String			enCode;
	protected DocumentFragment	root;
	// -------------------------正则匹配时，已做了忽略大小写，正则表达式请用小写实现
	protected Pattern			productFlag;
	protected static Pattern	productListFlag;
	protected String			sellerCode;
	protected String			seller;
	// -----------------------------------------
	public List<String>			productUrlList			= null;											// 详情网页中有，有商品编码列表时，可复写addProductUrls加入
	// -----------------------------------------
	protected String			idXPath					= "";
	protected String			titleXPath				= "//TITLE";
	protected List<String>		titleFilter				= new ArrayList<String>();							// title中的过滤词
	protected String			keywordXpath			= "//META[contains(@name,'eyword')]/@*";
	protected List<String>		keywordsFilter			= new ArrayList<String>();							// keywords中的过滤词

	// --------------------------------xpath
	protected String			productNameXpath		= "";
	protected String			productNameListXpath	= "";

	/**
	 * 解析商品详情信息
	 * 
	 * @param content
	 * @param enCode
	 * @return
	 */
	public OriProduct getProductInformation(String url) {
		this.url = url;
		LOG.info("@getProductInformation url:" + url);

		if (!isProductPage(url)) {
			LOG.info(String.format("%s is not a product page", url));
			return null;
		}

		OriProduct product1 = new OriProduct();
		product = product1;
		product.setProduct_url(url);

		try {
			setId();
		} catch (ProductIDIsNullException e) {
			LOG.error(null, e);
		} catch (Exception e) {
			LOG.error(null, e);
		}
		try {
			setProductName();
		} catch (ProductNameIsNullException e) {
			LOG.error(null, e);
		} catch (Exception e) {
			LOG.error(null, e);
		}

		productRowKey = product.getPid();
		String productName = product.getProductName();
		if (StrUtil.isEmpty(productRowKey)) {
			LOG.warn(String.format("url:%s===>parse id[%s]", this.url, productRowKey));
			return null;
		}

		if (StrUtil.isEmpty(productName)) {
			LOG.warn(String.format("url:%s===>rowkey[%s],productName[%s]", this.url, productRowKey, productName));
			return null;
		}

		try {
			setPrice();
		} catch (Exception e) {
			LOG.error(String.format("rowkey:%s parse price error!", productRowKey), e);
			this.product.setPrice("0");
		}
		setSeller();
		String errMessage = " canbe null parse:" + productRowKey;

		try {
			setTitle();
		} catch (Exception e) {
			LOG.info("title" + errMessage, e);
		}
		try {
			setIsCargo();
		} catch (Exception e) {
			LOG.info("isCargo" + errMessage, e);
		}
		try {
			setMaketPrice();
		} catch (Exception e) {
			LOG.info("marketPrice" + errMessage, e);
		}
		try {
			setDiscountPrice();
		} catch (Exception e) {
			LOG.info("discountPrice" + errMessage, e);
		}
		try {
			setBrand();
		} catch (Exception e) {
			LOG.info("brand" + errMessage, e);
		}
		try {
			setClassic();
		} catch (Exception e) {
			LOG.info("classic" + errMessage, e);
		}
		try {
			setCreatTime();
		} catch (Exception e) {
			LOG.info("creatTime" + errMessage, e);
		}
		try {
			setKeyword();
		} catch (Exception e) {
			LOG.info("keyword" + errMessage, e);
		}
		try {
			setMparams();
		} catch (Exception e) {
			LOG.info("mparams" + errMessage, e);
		}
		try {
			setContents();
		} catch (Exception e) {
			LOG.info("contents" + errMessage, e);
		}
		try {
			setOrgPic();
		} catch (Exception e) {
			LOG.info("orgPic" + errMessage, e);
		}
		try {
			setShortName();
		} catch (Exception e) {
			LOG.info("shortName" + errMessage, e);
		}
		try {
			setUpdateTime();
		} catch (Exception e) {
			LOG.info("updateTime" + errMessage, e);
		}
		// 20130923添加，原始分类
		try {
			setOriCatCode();
		} catch (Exception e) {
			LOG.info("setOriCatCode" + errMessage, e);
		}
		try {
			setCategory();
		} catch (Exception e) {
			LOG.info("category" + errMessage, e);
		}
		try {
			setCheckOriCatCode();
		} catch (Exception e) {
			LOG.info("checkOriCatCode" + errMessage, e);
		}
		try {
			setshortNameDetail();
		} catch (Exception e) {
			LOG.info("shortNameDetail" + errMessage, e);
		}
		try {
			setLimitTime();
		} catch (Exception e) {
			LOG.info("setLimitTime" + errMessage, e);
		}
		try {
			setD1ratio();
		} catch (Exception e) {
			LOG.info("setD1ratio" + errMessage, e);
		}
		try {
			setD2ratio();
		} catch (Exception e) {
			LOG.info("setD2ratio" + errMessage, e);
		}
		try {
			setTotalComment();
		} catch (Exception e) {
			LOG.info("setTotalComment" + errMessage, e);
		}
		try {
			setCommentStar();
		} catch (Exception e) {
			LOG.info("setCommentStar" + errMessage, e);
		}
		try {
			setCommentPercent();
		} catch (Exception e) {
			LOG.info("setCommentPercent" + errMessage, e);
		}
		try {
			setCommentList();
		} catch (Exception e) {
			LOG.info("setCommentList" + errMessage, e);
		}

		try {
			if (product.getClist() != null) {
				product.setHasComment(1);
			} else {
				product.setHasComment(0);
			}
		} catch (Exception e) {
			product.setHasComment(0);
		}

		// 20130614添加，设置商品原始分类码
		// try{setCatCode();}catch (Exception e) {LOG.info("setCatCode"+errMessage, e);}
		if (product.getPrice() != null) {
			if ("0".equals(product.getPrice()) || "-1".equals(product.getPrice())) {
				product.setIsCargo(0);// 设置为无货
			}
		} else {
			try {
				new BigDecimal(product.getPrice());
			} catch (Exception ex) {
				LOG.info(String.format("product %s price format error", productRowKey));
				product.setPrice("0");
				product.setIsCargo(0);// 设置为无货
			}
		}

		return product;
	}

	/**
	 * 将商品网页非链接关系获取的商品url加入列表中
	 * 
	 * @param productUrl
	 */
	public void addProductUrls(String productUrl) {
	}

	// ---------------------------------------------网页解析中可直接使用的统一方法

	@Override
	public void setSeller() {
		product.setSeller(seller);
		product.setSellerCode(sellerCode);
	}

	/*
	 * 已做了忽略大小写处理
	 */
	@Override
	public void setId() throws ProductIDIsNullException {
		String opid = getProductID(url);
		if (opid == null | opid.isEmpty()) {
			throw new ProductIDIsNullException(url + ":未能获取商品id！！");
		} else {
			String pid = sellerCode + opid;
			product.setPid(pid);
			product.setOpid(opid);
		}
	}

	@Override
	public void setTitle() {
		if (!titleXPath.isEmpty()) {
			String title = "";
			Node node = TemplateUtil.getNode(root, titleXPath);
			if (node != null) {
				title = node.getTextContent();
				if (titleFilter.size() > 0) {
					for (String keyFilter : titleFilter) {
						title = title.replaceAll(keyFilter, "");
					}
				}
				title = title.trim();
				product.setTitle(title);
			} else {
				LOG.info("title parse null，url=" + url);
			}
		}
	}

	@Override
	public void setKeyword() {
		if (!keywordXpath.isEmpty()) {
			String keywords = "";
			Node node = TemplateUtil.getNode(root, keywordXpath);
			if (node != null) {
				keywords = node.getTextContent();

				if (keywordsFilter.size() > 0) {
					for (String keyFilter : keywordsFilter) {
						keywords = keywords.replaceAll(keyFilter, "");
					}
				}
				keywords = keywords.replaceAll("，|,|。", " ").trim();
				product.setKeyword(keywords);
			}
		}
	}

	@Override
	public void setProductName() throws ProductNameIsNullException {
		Node node = StrUtil.isEmpty(productNameXpath) ? null : TemplateUtil.getNode(root, productNameXpath);
		String productName = node == null ? null : StrUtil.trim(node.getTextContent());
		if (StrUtil.isEmpty(productName)) {
			throw new ProductNameIsNullException(String.format("productName parser error for url:%s", url));
		} else {
			product.setProductName(productName);
		}

	}

	@Override
	public boolean isProductPage(String url) {
		// LOG.info("TemplateParser->isProductPage url=" + url);
		if (productFlag.matcher(url.toLowerCase()).find()) {
			return true;
		}
		LOG.info(url + " not a product url!");
		return false;
	}

	@Override
	/**
	 * 判断是否为productURL或者productListURL;均做了转小写处理，因此各个子模板的正则表达需要进行小写处理
	 * @param urlString
	 * @return Boolean
	 */
	public boolean urlFilter(String target) {
		// if (productFlag.matcher(target.toLowerCase()).matches() ||
		// productListFlag.matcher(target.toLowerCase()).matches()) {
		if (productFlag.matcher(target.toLowerCase()).matches()) {
			return true;
		}
		return false;
	}

	/**
	 * 从url中获取productID，已进行了忽略大小写处理
	 * 
	 * @param urlString
	 * @return
	 */
	public String getProductID(String urlString) {
		String productID = null;
		Matcher ma = productFlag.matcher(urlString.toLowerCase());
		if (ma.find()) {
			productID = ma.group(1);
		}
		return productID;
	}

	/**
	 * 从网页中获取SKU List，并组装productUrl List
	 * 
	 * @return
	 */
	public List<String> getProductUrlListFromSKUList() {
		return null;
	}

	/**
	 * 从页面中获取下一页的url baseUrl 表示当前页面的url
	 */
	public String nextUrl(URL baseUrl) {
		return "";
	}

	/**
	 * 从页面中获取下一级的url baseUrl 表示当前页面的url
	 */
	@Override
	public String isCrawlProduct(String url) {
		if (productFlag.matcher(url.toLowerCase()).find()) {
			return "2";
		}
		LOG.info(url + " not a product url!");
		return "1";
	}

	public boolean isSafeUrl(String url) {
		return true;
	}

	public String formatSafeUrl(String url) {
		return url;
	}

	@Override
	public String formatUrl(String url) {
		return null;
	}

	public String getProductRowKey() {
		return productRowKey;
	}

	@Override
	public void setIsCargo() {
		this.product.setIsCargo(1);
	}

	// 二次处理,仅仅处理 分类(category)、产类型号、产品品牌、短名称
	@Override
	public void secondDeal() {
	}

	// 处理原始分类
	@Override
	public void setCategory() {
	}

	// 20130923修改，处理商品原始分类码
	@Override
	public void setOriCatCode() {
	}

	// 20131029修改，处理商品中文编码混合原始分类码
	@Override
	public void setCheckOriCatCode() {
	}

	// 20131106修改，处理商品的第二种短名称
	@Override
	public void setshortNameDetail() {
	}

	// 20131113添加，活动售价的折扣
	@Override
	public void setD1ratio() {
	}

	// 20131113添加，折扣活动截止时间
	@Override
	public void setD2ratio() {
	}

	// 20131113添加，当前销售价的折扣
	@Override
	public void setLimitTime() {
	}

	@Override
	public void setTotalComment() {
	}

	@Override
	public void setCommentStar() {
	}

	@Override
	public void setCommentPercent() {
	}

	@Override
	public void setCommentList() {
	}

}
