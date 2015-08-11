package com.ym.nutch.plugin.parse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.plugin.util.Constant;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.GetImgPriceUtil;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.TemplateUtil;
import com.ym.nutch.plugin.util.URLUtil;

public class ParserNewEggNew extends ParserParent {

	public static final Logger LOG = LoggerFactory.getLogger(ParserNewEggNew.class);

	// http://www.newegg.com.cn/Product/A38-120-1M5.htm 
	private Pattern itemFlag1 = Pattern.compile("(http://www.newegg([.com]*).cn/product/)([\\w-]+)\\.htm");

	//手机端商品url：http://m.newegg.cn/Product/A1B-313-0G9-02.htm
	private Pattern itemFlag2 = Pattern.compile("(http://m.newegg([.com]*).cn/product/)([\\w-]+)\\.htm");
	public ParserNewEggNew() {
		super();
		init();
	}

	public ParserNewEggNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	String jsonXpath = "//SCRIPT";
	Map<String, String> map = new HashMap<String, String>();
	private List<String> seeds = new ArrayList<String>();

	public void init() {
		sellerCode = "1007";
		seller = "新蛋中国";
		priceXpath="//DD[@class='neweggPrice']/P/IMG/@src"+" | //SPAN[@id='priceValue']/SPAN/STRONG/IMG/@src";
		brandXpath = "//DIV[@id='tabCot_product_2']/TABLE//descendant::TR[2]/TD";
		classicXpath = "//DIV[@id='crumb']/DIV";
		categoryXpath="//DIV[@id='crumb']/DIV/DIV/A";
		oriCatCodeXpath="//DIV[@id='crumb']/DIV/DIV/A/@href";
		productNameXpath = "//H1";
		imgXpath="//IMG[@id='midImg']/@src340"+" | //IMG[@class='zoomSmall']/@src";
		mparasXpath = "//DIV[@id='tabCot_product_2']/TABLE//descendant::TR[TD]";
		contentDetailXpath = "//DIV[@id='tabCot_product']/DIV/DIV/UL"+" | //DIV[@class='goods_detail_info']";
		keywordXpath="//META[@name='keywords']/@content";
		isCargoXpath="//DIV[@class='detailList']/DL/DD[contains(text(),'库存情况')]/SPAN";
		nextPageXpath = "//A[normalize-space(@class)='next']/@href";
		totalCommentXpath="//DIV[@id='comment']/DIV/DIV/UL/LI/A";
		commentStarXpath="//DD[@class='rank']/SPAN/A/@title";
		productFlag = itemFlag1;
		
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		filterStr = "产品编号xm99首页";
		
		keywordsFilter.add("-新蛋中国");
		keywordsFilter.add("新蛋中国");
		keywordsFilter.add("新蛋网");
		keywordsFilter.add("Newegg");
		
		titleFilter.add("【.*?】|-新蛋中国|新蛋中国|Newegg|新蛋网");
		
		classFilter.add("首页>");
		
		outLinkXpath.add("//DIV[@class='allCateList']/DL/DD/EM/A/@href");// 总分类页面
		outLinkXpath.add("//UL[@class='prolist cls']/LI/DIV/A/@href"); //分类列表页
		
		// 种子地址，不过滤
		seeds.add("http://www.newegg.com.cn/CategoryList.htm");
	}

	/**
	 * 2013年8月7日修改if (index1 > 0 && index2 > 0)====》if (index1 >= 0 && index2 >= 0)
	 */
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
				index1 = value.indexOf("resources_ProductDetail");
				index2 = value.indexOf("productID");
				if (index1 >= 0 && index2 >= 0) {
					jsonNode = node;
					break;
				}
			}
		}
		if (jsonNode == null) {
			LOG.info(this.getClass() + "Error---No Analytical json Object!");
			LOG.info(this.getClass().getSimpleName() + "-->parseInit,json node is null");
		} else {
			jsonValue = jsonNode.getTextContent().replaceAll("[\\s]{3,}", " ");
			index1 = jsonValue.indexOf("resources_ProductDetail");
			if (index1 >= 0) {
				jsonValue = jsonValue.substring(index1);// 得到json字符串
				jsonValue = Formatter.getJson(jsonValue).replaceAll("[\\s]", "");
			}
		}
		if (jsonValue.contains("productID")) {
//			JSONObject productObject = JSONObject.fromObject(jsonValue);
			String productID=jsonValue.substring(jsonValue.indexOf("productID")+11, jsonValue.indexOf(",")-1);
			map.put("skuid", productID);
		}
	}

	/**
	 *  2013年7月25日修改：控制异常打印
	 */
	@Override
	public void setId() throws ProductIDIsNullException {
		parseInit();
		String pid = (String) map.get("skuid");
		if (pid == null || pid.isEmpty()) {
			throw new ProductIDIsNullException(url + "-->Product Id Is Error！");
		} else {
			String id = sellerCode + pid;
			super.product.setPid(id);
			super.product.setOpid(pid);
		}
		
	}

	/**
	 *  2013年8月7日修改：常规一种商品页有两种价格：新蛋价==》setPrice;折扣价==》setDiscountPrice。
	 *  另一种商品页只有一种价格，就是新蛋价，页面定位却在折扣价。
	 *  如果两种价格都存在的话，就正常解析，如果只有一种价格，就用折扣价格作新蛋价来处理。
	 *  在否则就报异常处理。
	 * @throws ProductPriceIsNullException 
	 */
	@Override
	public void setPrice() throws ProductPriceIsNullException{
		String price = "";
		try{
			Node priceNode = TemplateUtil.getNode(root, priceXpath);
			if (priceNode != null) {
				String urlprice = priceNode.getTextContent();
				price = GetImgPriceUtil.getPriceAsStr(urlprice);
				if(!"".equals(price)){
					int index=price.lastIndexOf(".");
					if(index>=0){
						price=price.substring(0, index);
						price=price+".00";
					}
					price=trimPrice(price).trim();
					this.product.setPrice(price);
				}
			}
		}catch(Exception e){
			throw new ProductPriceIsNullException(url
					+ "Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
		}
	}

//	@Override
//	public void setDiscountPrice() {
//		String discountPrice = null;
//		Node priceNode = TemplateUtil.getNode(root, discountPriceXpath);
//		if (priceNode != null) {
//			String urlprice = priceNode.getTextContent();
//			discountPrice = GetImgPriceUtil.getPriceAsStr(urlprice);
//			if(discountPrice!=null && !"".equals(discountPrice)){
//				discountPrice=trimPrice(discountPrice).trim();
//				super.product.setDiscountPrice(discountPrice);
//			}
//		}
//	}
	
	@Override
	public void setClassic() {
		Node node_location = TemplateUtil.getNode(root, classicXpath);
		if (node_location != null) {
			String value = node_location.getTextContent().replaceAll("[\\s]{3,}", "");
			if (classFilter.size() > 0) {
				for (String filterString : classFilter) {
					value = value.replaceAll(filterString, "");
				}
			}
			value = value.replaceAll(">", Constant.XMTAG);
			super.product.setClassic(value);
		}
	}

	/**
	 * 2013年8月10日取消该方法，Xpath定位修改，调用父类方法。
	 */
//	@Override
//	public void setMparams() {
//		String id = getContentId("规格参数");
//		String tableXpath = "//DIV[@id='" + id + "']/TABLE/descendant::TR";
//		super.mparasXpath = tableXpath;
//		super.setMparams();
//	}

	@Override
	public void setContents() {
		String id = getContentId("产品描述");
		if(id!=null && !"".equals(id)){
			String tableXpath = "//DIV[@id='" + id + "']/TABLE";
			super.contentDetailXpath = tableXpath;
			super.setContents();
		}else{
			LOG.info(url+"-->Praser Product Contents Information is NULL");
			String value="此商品没有提供商品详情内容";
			super.product.setContents(value);
		}
		
	}

	private String getContentId(String keyword) {
		Node node = TemplateUtil.getNode(root, contentDetailXpath);
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node n = nodeList.item(i);
			if (keyword.equals(n.getTextContent())) {
				String value = n.getFirstChild().getAttributes()
						.getNamedItem("rel").getNodeValue();
				String id = "tabCot_product_" + value;
				return id;
			}
		}
		return "";
	}

	//20131122添加
	@Override
	public String nextUrl(URL baseUrl) {
		String nextUrl = "";
		Node node = TemplateUtil.getNode(root, nextPageXpath);
		if (node != null) {
			try {
				String urlTarget = node.getTextContent();
				URL urls = URLUtil.resolveURL(baseUrl, urlTarget);
				nextUrl = urls.toString();
				if(isSafeUrl(nextUrl)){
					return formatSafeUrl(nextUrl);
				}
			} catch (MalformedURLException e) {
//				LOG.error(ParserNewEggNew.class.getSimpleName()+"->nextUrl error:" + e);
				return nextUrl;
			}
		}
		return nextUrl;
	}
	
	/**
	 * 获取列表URL规则
	
	@Override
	public List<imageProduct> nextUrlList(URL baseUrl,String content) {
		List<imageProduct> urlList = new ArrayList<imageProduct>();
		Set<imageProduct> urlSet = new LinkedHashSet<imageProduct>();
		URL urls = null;
		try {
			Document doc = Jsoup.parse(content);
			Elements elements=doc.getElementsByTag("A");
			if(elements.size() > 0){
				for (int i = 0; i < elements.size(); i++) {
					Element urlListEle = elements.get(i);
					String productUrl=urlListEle.attr("href");
					String smallPic=urlListEle.getElementsByTag("img").attr("src");
					if(!smallPic.startsWith("http://"))
						continue;
					try{
						urls = URLUtil.resolveURL(baseUrl, productUrl);
						if(!StrUtil.isEmpty(urls.toString()) && !StrUtil.isEmpty(smallPic)){
							imageProduct image=new imageProduct(smallPic,urls.toString());
							urlSet.add(image);
							LOG.info("imageProduct :"+image);
						}
					}catch(Exception e){
						LOG.info(this.getClass().getSimpleName()+"  resolveURL Error! url: "+baseUrl.toString());
					}
				}
		   }
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName()+"  Parser Error! url: "+baseUrl.toString());
		}
		urlList.addAll(urlSet);
		return urlList;
	}
	 */
	
	/**
	 * 2013年8月8日添加：添加过滤无用外链的条件
	 */
	public boolean isSafeUrl(String url) {
		if (urlFilter(url)) {
			return true;
		} else{
			for(String seed : seeds){
				if(seed.equals(url)){
					return true;
				}
			}
			return false;
		}
	} 

/**
 * 20131016添加：将url标准化，去重操作。
 *  http://www.newegg.com.cn/SubCategory/1400-4.htm#itemGrid1
 */
	@Override
	public String formatSafeUrl(String url){
		String removeStr="#itemGrid1";
		url=url.replaceAll("http://www.newegg.com.cn/", "http://www.newegg.cn/");
		if(url.lastIndexOf(removeStr) >= 0){
			url=url.replaceAll(removeStr, "");
		}
		return url;
	}
	
	
	/**
	 *  2013年7月25日添加方法
	 */
//	@Override
//	public void setShortName() {
//		String shortName=super.product.getProductName();
//		if (shortName != null && !"".equals(shortName)) {
//			if (keywordsFilter.size() > 0) {
//				super.keywordsFilter.add("【.*?】");
//				super.keywordsFilter.add("\\(.*?\\)");
//				for (String keyFilter : keywordsFilter) {
//					shortName = shortName.replaceAll(keyFilter, "");
//				}
//			}
//			super.product.setShortName(shortName);
//		} else {
//			LOG.info(url+"-->Product ShortName Is Error!");
//		}
//	} 
	
	@Override
	public void setShortName() {
		String modelValue="";
		String brandValue="";
		String shortNameByMparams=this.product.getMparams();
		if(shortNameByMparams !=null || !"".equals(shortNameByMparams)){
			String[] shortNameArry=shortNameByMparams.split("\\[xm99\\]");
			if(shortNameArry.length > 0){
				for(int i=0;i<shortNameArry.length;i++){
					String shortNameStr=shortNameArry[i].replaceAll("[\\s]{2,}", "").trim();
//					if(shortNameStr.indexOf("品牌") >= 0){
//						String[] shortNameStrArryByBrand=shortNameStr.split("=");
//						if("品牌".equals(shortNameStrArryByBrand[0])){
//							brandValue=shortNameStrArryByBrand[1];
//						}
//						
//					}
					if(shortNameStr.indexOf("型号") >= 0){
						String[] shortNameStrArryByModel=shortNameStr.split("=");
						if("型号".equals(shortNameStrArryByModel[0])){
							modelValue=shortNameStrArryByModel[1];
							modelValue=" "+modelValue;
						}
					}
					
				}
			}
		}
		if("".equals(brandValue)){
			brandValue=this.product.getBrand();
			if(brandValue!=null && !"".equals(brandValue)){
				if(brandValue.contains("\\[xm99\\]")){
					String[] brandValueArry = brandValue.split("\\[xm99\\]");
					for(int i=0;i<brandValueArry.length;i++){
						String brandValueStr=brandValueArry[i];
						 char c = brandValueStr.charAt(0);   
		                  if(((c >= 'a'&& c <= 'z') || (c >= 'A' && c <= 'Z'))){ 
		                	  brandValue=brandValueStr;
		                      break;
		                  }
					}
				}
				brandValue=brandValue.replaceAll("\\[xm99\\]", " ");
			}
		}
		if(brandValue!=null && !"".equals(brandValue)){
			String shortName=brandValue+modelValue;
			this.product.setShortName(shortName);
			this.product.setShortNameDetail(shortName);
		}
		
	} 
	
	/**
	 * 20131030添加，解析有货无货状态
	 */
	@Override
	public void setIsCargo() {
		if(!isCargoXpath.isEmpty()){
			Node isCargoNode=TemplateUtil.getNode(root, isCargoXpath);
			if(isCargoNode!=null){
				String isCargoInfor=isCargoNode.getTextContent().replaceAll("[\\s]", "");
				if(isCargoInfor != null && !"".equals(isCargoInfor)){
					if(isCargoInfor.contains("有货")){
						this.product.setIsCargo(1);
						return;
					}
				}
			}
		}
		this.product.setIsCargo(0);
	}
	
	/**
	 * 20131110添加
	 * @param args
	 * @throws Exception
	 * @throws ProductIDIsNullException
	 */
	
	@Override
	public void setBrand() {
		String brandValue="";
		Node node = TemplateUtil.getNode(root, brandXpath);
		if (node != null) {
			String value = node.getTextContent().replaceAll("[\\s]{2,}", "").replaceAll("：", ":").trim();
			String[] values = value.split(":");
			if (values.length == 2) {
				brandValue=values[1];
			} else {
				brandValue=value;
			}
			if(brandValue != null || !"".equals(brandValue)){
				brandValue=brandValue.replaceAll("[\\s]{1,}", "\\[xm99\\]");
				this.product.setBrand(brandValue);
			}
		}
	}
	
	@Override
	public void setCategory() {
		if(!"".equals(categoryXpath)){
			NodeList categoryNodeList=TemplateUtil.getNodeList(root, categoryXpath);
			if(categoryNodeList.getLength() > 0){
				String categoryValue="";
				StringBuffer categoryBuffer = new StringBuffer();
				for(int i=0;i<categoryNodeList.getLength();i++){
					categoryValue=categoryNodeList.item(i).getTextContent().replaceAll("[\\s]{1,}", "");
					if (i != 0) {
						categoryBuffer.append("[xm99]");
					}
					categoryBuffer.append(categoryValue);
				}
				if(categoryBuffer.length() > 0){
					categoryValue=categoryBuffer.toString();
					categoryValue=categoryValue.replaceAll("首页\\[xm99\\]", "");
					product.setCategory(categoryValue);
				}
			}
		}
	}
	
	@Override
	public void setOriCatCode() {
		if(!oriCatCodeXpath.isEmpty()){
			NodeList oriCateCodeNodeList=TemplateUtil.getNodeList(root, oriCatCodeXpath);
			if(oriCateCodeNodeList.getLength() > 0){
				for(int i=0;i<oriCateCodeNodeList.getLength();i++){
					String oriCatCodeByHref=oriCateCodeNodeList.item(i).getTextContent().trim();
					if(oriCatCodeByHref.contains("/SubCategory/")){
						oriCatCodeByHref=oriCatCodeByHref.substring(oriCatCodeByHref.indexOf("/SubCategory/")+13, oriCatCodeByHref.indexOf(".htm"));
						if(oriCatCodeByHref != null || !"".equals(oriCatCodeByHref)){
							this.product.setOriCatCode(oriCatCodeByHref);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void setCheckOriCatCode() {
		String checkOriCatCodeValue="";
		String cateGoryValue=this.product.getCategory();
		if(cateGoryValue!=null && !"".equals(cateGoryValue)){
			cateGoryValue=cateGoryValue.replaceAll("[\\s]{1,}", "");
			String oriCatCode = this.product.getOriCatCode();
			checkOriCatCodeValue=cateGoryValue+"#"+oriCatCode;	
			product.setCheckOriCatCode(checkOriCatCodeValue);
		}
	}
	
	/**
	 * 20131118添加：二次请求
	 * http://www.newegg.cn/Ajax/Product/AjaxProdDetailCountDown.aspx?sysno=297426
	 */
	@Override
	public void setLimitTime() {
		String limitTime="";
		Node panicBuyingNode=TemplateUtil.getNode(root, "//DD[@class='neweggPrice']/SPAN");
		if(panicBuyingNode!=null){
			String panicBuyingValue=panicBuyingNode.getTextContent().replaceAll("[\\s]{1,}", "");
			if(panicBuyingValue != null && !"".equals(panicBuyingValue) && panicBuyingValue.contains("抢购价")){
				String productId=this.product.getPid();
				if(productId!=null || !"".equals(productId)){
					String limitTimeSecondUrl="http://www.newegg.cn/Ajax/Product/AjaxProdDetailCountDown.aspx?sysno="+productId;
					String limitTimeValue = RequestUtil.getOfHttpURLConnection(limitTimeSecondUrl, "utf-8").toString();
					try{
						if(limitTimeValue!=null && !"".equals(limitTimeValue)){
							limitTimeValue = Formatter.getJson(limitTimeValue);
							if (!"".equals(limitTimeValue)) {
								JSONObject jsonObj = JSONObject.parseObject(limitTimeValue);
								limitTime= jsonObj.getString("Data");
								if(!"".equals(limitTime) && !"0".equals(limitTime)){
								   long dateTime=Long.parseLong(limitTime)*1000+System.currentTimeMillis();
								   if(dateTime > 0){
									   String price=this.product.getPrice();
										if(price != null && !"".equals(price)){
											this.product.setDiscountPrice(price);
										}
									   limitTime=String.valueOf(dateTime);
									   if(!"".equals(limitTime)){
										   String secondPriceXpath="//DD[@class='originalPrice']/DEL";
											Node secondPriceNode=TemplateUtil.getNode(root, secondPriceXpath);
											if(secondPriceNode!=null){
												String secondPrice=secondPriceNode.getTextContent();
												if(secondPrice != null && !"".equals(secondPrice)){
													secondPrice=trimPrice(secondPrice).trim();
													this.product.setPrice(secondPrice);
//													System.out.println("限时活动中的常规价格   "+this.product.getPrice());
												}
											}
										   this.product.setLimitTime(limitTime);
									   }
								   }
								}
							}
						}
					}catch(Exception e){
				    }
				if("".equals(limitTime) || "0".equals(limitTime)){
					this.product.setDiscountPrice("0");
					this.product.setLimitTime("0");
				}
			 }
			}
		}
	}
	
	@Override
	public void setCommentList(){
		
		String id1="//DIV[@class='reviewList']/DIV[normalize-space(@class='listCells')][1]/DIV[normalize-space(@class='title')]/H2/A/@href";
		String id2="//DIV[@id='comment']//descendant::DIV[@class='listCell '][2]/DIV[normalize-space(@class='title')]/H2/A/@href";
		String id3="//DIV[@id='comment']//descendant::DIV[@class='listCell '][3]/DIV[normalize-space(@class='title')]/H2/A/@href";
		String id4="//DIV[@id='comment']//descendant::DIV[@class='listCell '][4]/DIV[normalize-space(@class='title')]/H2/A/@href";
		String id5="//DIV[@id='comment']//descendant::DIV[@class='listCell '][5]/DIV[normalize-space(@class='title')]/H2/A/@href";

		String name1="//DIV[@class='reviewList']/DIV[normalize-space(@class='listCells')][1]/DL[@class='userInfo']/DT/A[1]/@title";
		String name2="//DIV[@class='reviewList']//descendant::DIV[@class='listCell '][2]/DL[@class='userInfo']/DT/A[1]/@title";
		String name3="//DIV[@id='comment']//descendant::DIV[@class='listCell '][3]/DL[@class='userInfo']/DT/A[1]/@title";
		String name4="//DIV[@id='comment']//descendant::DIV[@class='listCell '][4]/DL[@class='userInfo']/DT/A[1]/@title";
		String name5="//DIV[@id='comment']//descendant::DIV[@class='listCell '][5]/DL[@class='userInfo']/DT/A[1]/@title";

		String content1="//DIV[@class='reviewList']/DIV[normalize-space(@class='listCells')][1]/DIV[@class='detail']/DIV[@class='content']/DL//descendant::DD[position()<4]";
		String content2="//DIV[@class='reviewList']//descendant::DIV[@class='listCell '][2]/DIV[@class='detail']/DIV[@class='content']/DL//descendant::DD[position()<4]";
		String content3="//DIV[@id='comment']//descendant::DIV[@class='listCell '][3]/DIV[@class='detail']/DIV[@class='content']/DL//descendant::DD[position()<4]";
		String content4="//DIV[@id='comment']//descendant::DIV[@class='listCell '][4]/DIV[@class='detail']/DIV[@class='content']/DL//descendant::DD[position()<4]";
		String content5="//DIV[@id='comment']//descendant::DIV[@class='listCell '][5]/DIV[@class='detail']/DIV[@class='content']/DL//descendant::DD[position()<4]";
			
		try{
			List<Comment> clist = new ArrayList<Comment>();
			Comment c1 = getComment(id1, name1, content1);
			if(c1!=null) clist.add(c1);
			Comment c2 = getComment(id2, name2, content2);
			if(c2!=null) clist.add(c2);
			Comment c3 = getComment(id3, name3, content3);
			if(c3!=null) clist.add(c3);
			Comment c4 = getComment(id4, name4, content4);
			if(c4!=null) clist.add(c4);
			Comment c5 = getComment(id5, name5, content5);
			if(c5!=null) clist.add(c5);
			product.setClist(clist);
			
		}catch(Exception e){
		}
	}
	
	private Comment getComment(String idXpath,String nameXpath,String contentXpath){
		Node node=TemplateUtil.getNode(root, idXpath);
		if(node!=null){
			String id=node.getTextContent();
			id=id.substring(id.lastIndexOf("-")+1,id.indexOf(".htm"));
			if(StringUtils.isNotEmpty(id)){
				String cid=sellerCode+id;
				Node nameNode=TemplateUtil.getNode(root, nameXpath);
				String name = nameNode==null? "" : StringUtils.trim(nameNode.getTextContent());
				NodeList contentNodeList=TemplateUtil.getNodeList(root, contentXpath);
				int contentNodeSize = contentNodeList==null ? 0 : contentNodeList.getLength();
				StringBuffer sb=new StringBuffer();	
				if(contentNodeSize >0){
					for(int i=0;i<contentNodeSize;i++){
						String content=contentNodeList.item(i).getTextContent().replaceAll("[\\s]{1,}", "").trim();
						sb.append("\n"+content);
					}
				}
				if(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(sb.toString())){
					String c = name + "[xm99]" + sb.toString();
					Comment cm = new Comment();
					cm.setCommentId(cid);
					cm.setCommentContent(c);
					return cm ;
				}
			}
		}
		return null ;
	}
	
	public static void main(String[] args) throws Exception, ProductIDIsNullException {
//		String url = "http://www.newegg.com.cn/Product/A28-184-4NQ-02.htm";
//		HtmlParser hp = new HtmlParser();
//		Product p = hp.testParse(url);		
//		System.exit(1);
		
		
		ParserNewEggNew test = new ParserNewEggNew();
//		test.formatSafeUrl("http://www.newegg.com.cn/SubCategory/1400-4.htm#itemGrid1");
		 
//		//http://www.newegg.cn/Product/A11-189-2PU.htm
//		 boolean aa=test.isProductPage("http://www.newegg.cn/Product/A11-189-2PU.htm");
//		 System.out.println(aa);
// 
//		 String formatUrl=test.formatSafeUrl("http://www.newegg.com.cn/Product/A28-184-4NQ-02.htm");
//		 System.out.println("formatUrl---->"+formatUrl);
		// String url = "http://www.baidu.com";
		// test.htmlByUrl(url);
		
		test.test1();
	}
	
	public void test1() throws Exception, ProductIDIsNullException{
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\xindan.html"), "gb2312");
		ParserNewEggNew test = new ParserNewEggNew(root);
		test.product = new OriProduct();
		
//		 URL url = new URL(
//		 "http://www.newegg.cn/Product/A0G-1PL-147.htm");
//		 String aa = test.nextUrl(url);
//		 System.out.println(aa);
		 
//		 test.setCommentOne();
//		 System.out.println("商品评论One======>:"+ test.product.getCommentOne().getCommentId()+">>>>" + test.product.getCommentOne().getCommentContent());
//		 
//		 test.setCommentTwo();
//		 System.out.println("商品评论Two======>:"+ test.product.getCommentTwo().getCommentId()+">>>>" + test.product.getCommentTwo().getCommentContent());
//		 
//		 test.setCommentThree();
//		 System.out.println("商品评论Three======>:"+ test.product.getCommentThree().getCommentId()+">>>>" + test.product.getCommentThree().getCommentContent());
//		 
//		 test.setCommentFour();
//		 System.out.println("商品评论Four======>:"+ test.product.getCommentFour().getCommentId()+">>>>" + test.product.getCommentFour().getCommentContent());
//		 
//		 test.setCommentFive();
//		 System.out.println("商品评论Five======>:"+ test.product.getCommentFive().getCommentId()+">>>>" + test.product.getCommentFive().getCommentContent());
		 
		 test.setId();
		 System.out.println("id=====>: " + test.product.getPid() + ">>>>"
		 + test.product.getPid());
		
		test.setProductName();
		System.out.println("商品名称======>:" + test.product.getProductName());
//		
//		 test.setClassic();
//		 System.out.println("导航=====>: " + test.product.getClassic());
//		
//		 test.setBrand();
//		 System.out.println("品牌=====>：" + test.product.getBrand());
//		
//		 test.setContents();
//		 System.out.println("content=====>：" + test.product.getContents());
//		
		 test.setOrgPic();
		 System.out.println("图片=====>：" + test.product.getOrgPic());
//		
		 test.setPrice();
		 System.out.println("价钱=====> ：" + test.product.getPrice());
//		
//		 test.setDiscountPrice();
//		 System.out.println("折扣价格=====>：" + test.product.getDiscountPrice());
//		 
//		 test.setMparams();
//		 System.out.println("mp内容=====> ：" + test.product.getMparams());
//		
//		 test.setTitle();
//		 System.out.println("标题=====>：" + test.product.getTitle());
//		
//		 test.setKeyword();
//		 System.out.println("关键字=====>: " + test.product.getKeyword());
//		
//		 test.setIsCargo();
//		 System.out.println("是否有货====》:" + test.product.getIsCargo());
//				
//		 test.setShortName();
//		 System.out.println("商品短名称======>:" + test.product.getShortName());
//			
//		 test.setshortNameDetail();
//		 System.out.println("第二种商品短名称======>:" + test.product.getShortNameDetail());
//		 
//		 test.setCategory();
//		 System.out.println("中文原始分类======>:" + test.product.getCategory());
//		 
//		 test.setOriCatCode();
//		 System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
//		 
//		 test.setCheckOriCatCode();
//		 System.out.println("校验混合中文原始分类====》:" + test.product.getCheckOriCatCode());
		 
//		test.setPrice();
//		 System.out.println("当前价格=====>:" + test.product.getPrice());
//		 
//		test.setMaketPrice();
//		System.out.println("市场价格=====>:" + test.product.getMaketPrice());
//		
//		
//		test.setDiscountPrice();
//		System.out.println("活动价格=====>:" + test.product.getDiscountPrice());
//		
//		test.setLimitTime();
//		System.out.println("折扣活动截止时间=====>:" + test.product.getLimitTime());
//		System.out.println("活动价格=====>:" + test.product.getDiscountPrice());
//		System.out.println("当前价格=====>:" + test.product.getPrice());
//		
//		test.setD1ratio();
//		System.out.println("活动销售价格的折扣=====>:" + test.product.getD1ratio());
//		
//		test.setD2ratio();
//		System.out.println("当前销售价格的折扣=====>:" + test.product.getD2ratio());
//		
//		long limitTime=Long.parseLong("311122")*1000+System.currentTimeMillis();
//		System.out.println(new Date(limitTime));
		
//		test.setTotalComment();
//		System.out.println("总评论数=====>:" + test.product.getTotalComment());
//		
//		test.setCommentStar();
//		System.out.println("评论星级数=====>:" + test.product.getCommentStar());
//		
//		test.setCommentPercent();
//		System.out.println("评论百分数=====>:" + test.product.getCommentPercent());
		
//		URL url;
//		try {
//			url = new URL("http://www.newegg.com.cn/CategoryList.htm");
//			List<String> list = test.nextUrlList(url);
//			for (int i = 0; i < list.size(); i++) {
//				System.out.println(list.get(i));
//			}
//			System.out.println(list.size());
//		} catch (MalformedURLException e) {
//		}
	}
}
