package com.ym.nutch.plugin.parse;

import java.io.File;
import java.net.URLEncoder;
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
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.Comment;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.plugin.util.Formatter;
import com.ym.nutch.plugin.util.GsonParser;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.RequestUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;

public class Parser51buyNew extends ParserParent {

	public static final Logger LOG = LoggerFactory.getLogger(Parser51buyNew.class);

	// http://item.yixun.com/item-334401.html
	private Pattern itemFlag1 = Pattern.compile("(http://item.yixun.com/item-)([0-9]+).*?");
   //手机端商品url http://m.yixun.com/t/detail/index.html?pid=712354&channelId=
	private Pattern itemFlag2 = Pattern.compile("(http\\://m\\.yixun\\.com/t/detail/index\\.html\\?pid\\=)([0-9a-zA-Z&=?]+).*?");
	//手机端易迅跳转url： http://m.yixun.com/t/list/index.html?cid=705852t705856&option=&cateword=%E5%85%A8%E9%83%A8%E6%89%8B%E6%9C%BA#detail(%7B%22pid%22%3A%22712354%22%2C%22_%22%3A1390300141558%7D)
   //	               http://m.yixun.com/t/channel/index.html#detail(%7B%22pid%22%3A%221257119%22%2C%22channelId%22%3A%22300%22%2C%22_%22%3A1392285546222%7D)
//	                   http://m.yixun.com/t/channel/morning.html#detail(%7B%22pid%22%3A%22572984%22%2C%22channelId%22%3A%22200%22%2C%22_%22%3A1392626213298%7D)";
	private Pattern itemFlag3 = Pattern.compile("(http\\://m\\.yixun\\.com/t/list/index\\.html\\?cid\\=)([0-9a-zA-Z].*?)\\#detail.*?");
	private Pattern itemFlag4 = Pattern.compile("(http\\://m\\.yixun\\.com/t/channel/)([a-zA-Z].*?)\\.html\\#detail.*?");
	                              
	public Parser51buyNew() {
		init();
	}

	public Parser51buyNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	String jsonXpath = "//SCRIPT";
	Map<String, String> map = new HashMap<String, String>();
	private List<String> seeds = new ArrayList<String>();
	private static Map<String, JSONArray> JSONArrayMap = new HashMap<String, JSONArray>();
	
	public void init() {
		sellerCode = "1009";
		seller = "易迅网";
		classicXpath="//DIV[@class='mod_crumb']//descendant::A[@target='_blank']";
		oriCatCodeXpath="//DIV[@class='mod_crumb']/A[@target='_blank'][last()]/@href";
		priceXpath="//STRONG[@class='price_font']"+" | //SPAN[@itemprop='price']"+" | //SPAN[@itemprop='highPrice']/DEL";
		discountPriceXpath="//SPAN[@itemprop='lowPrice']";
		marketPriceXpath = "//LI[@class='li item_icson']/DEL";
		keywordXpath = "//META[@name='keywords']/@content";
		mparasXpath="//TABLE//descendant::TR[TD[2]]";
		nextPageXpath = "//A[@class='page-next']/@href" + " | //A[@class='sort_page_arrow']/@href";
		totalCommentXpath="//A[@id='sea_review_box']/SPAN";
		commentStarXpath="//SPAN[@class='x_mod_grade_score']";
		imgXpath="//IMG[@id='xgalleryImg']/@src";
		filterStr = "首页";
		productFlag = itemFlag1;
		
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		productUrlList.add(itemFlag3);
		productUrlList.add(itemFlag4);
		
		classFilter.add("首页 >");
		classFilter.add("首页>");
		keywordsFilter.add("报价");
		keywordsFilter.add("U盘,手机,读卡器,数码相机,MP3/MP4，手机电池，相机电池，数码摄影包，液晶保护膜，手机膜贴，镜头滤镜，Mp3/MP附件,手机充电器,相机清洁用品,蓝牙耳机,单反相机,iPod 附件,数码相框,USB播放器, 易迅网");
		keywordsFilter.add("易迅网");
		titleFilter.add("\\[.*?\\]|【.*?】|易迅网|网上购物|行情|报价|价格");

		outLinkXpath.add("//DIV[@class='pmc']/DIV[@class='pic']/A");   //手机端分类列表页面：http://m.jd.com/products/670-671-672.html
		outLinkXpath.add("//A[IMG[contains(@src,'.jpg')]]");
	}

	/**
	 * 商品详情内容和商品参数内容用二次请求url的方式获取
	 * 再从productObject中提取属性的时候先做是否包含该属性的判断，避免有野商品页面确实没有该属性而出现异常，影响其他字段解析。
	 * 另一种页面：http://item.yixun.com/item-164850.html  二次请求的url不同。
	 * 添加二次请求的url(maparmsUrl)日志打印
	 * 商品页面的网页代码布局不一样，json的位置也不一样，如：http://item.yixun.com/item-289292.html，造成
	 *                 json解析失败，没有删除之前的代码，只是在原代码的基础上做了适配
	 * 对不同的页面进行不同的二次请求url处理，主要针对商品详情内容和商品参数内容。
	 */
	public void parseInit() {
		//直接从商品网页源代码中的SCRIPT脚本获取。
		JSONObject productObject = Formatter.getJson(root, jsonXpath,"varitemInfo=", "p_char_id", "itemInfo");
		if (productObject == null || productObject.isEmpty()) {
			productObject = Formatter.getJson(root, jsonXpath,"setItemInfo(", "pid", "setItemInfo(");
		}
		if(productObject.toString().contains("name")){
			map.put("name", productObject.getString("name"));   //商品名称
		}
		if(productObject.toString().contains("p_char_id")){
			map.put("flag", productObject.getString("p_char_id"));//商品图片
		}
		if(productObject.toString().contains("brand_name")){
			map.put("brand_name", productObject.getString("brand_name"));//商品的品牌
		}
		if(productObject.toString().contains("shipping_desc")){
			map.put("shipping_desc", productObject.getString("shipping_desc")); //有货无货
		}
		
		// 处理详情和参数，二次请求
		//(//http://item.wgimg.com/det_00000000000000000000005A830D453D)
		//先从商品网页源代码中获得二次请求的url，在发出请求，获得商品详情内容和参数内容。 
		JSONObject tmp = Formatter.getJson(root, "//SCRIPT","varconfig=", "lv", "config");
		if (tmp != null && !tmp.isEmpty()) {
			String scdUrl = tmp.getString("detailUrl");
			String message = RequestUtil.getOfHttpURLConnection(scdUrl,"utf-8").toString();
			if(message!=null && !"".equals(message)){
				message = message.substring(message.indexOf("{"), message.lastIndexOf(";"));
				message = message.replaceAll("\"0\"", "\"detail\"");
				message = message.replaceAll("\"1\"", "\"param\"");
				GsonParser gp=new GsonParser(message);
				String detail=gp.getP().getDetail();
				if(detail!=null && !"".equals(detail)){
					map.put("content", detail);
				}
				String mapram=gp.getP().getParam();
				if(mapram != null && !"".equals(mapram)){
					map.put("maparmas",mapram );
				}
			}
		} 		
	}
	
	/**
	 * parseInit()方法移至此方法中先初始化。
	 * 把日志信息改成英文，避免中文乱码现象。
	 */
	
	@Override
	public void setId() throws ProductIDIsNullException {
		String pid = null;
//		url="http://item.yixun.com/item-1270935.html";
		Matcher ma1 = productFlag.matcher(url.toLowerCase());
		if (ma1.find()) {
			pid = ma1.group(2);
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
	 *  把日志信息改成英文，避免中文乱码现象。
	 */
	@Override
	public void setProductName(){
		parseInit();
		String productName = (String) map.get("name").replaceAll("[\\s]", "");
		if (productName == null || productName.isEmpty()) {
			new ProductNameIsNullException(url + "-->Error---Product ProductName Is Null");
		}
		product.setProductName(productName);
	}
	
	/**
	 *  把日志信息改成英文，避免中文乱码现象。
	 */
		@Override
		public void setBrand() {
		   String brand = map.get("brand_name").trim();
			if(brand!=null && !"".equals(brand)){
				brand=brand.replaceAll("[\\s]{1,}", "\\[xm99\\]");
				product.setBrand(brand);
			}else{
				LOG.info(url+"-->Error---Product Brand Information Is Null");
			}
		}


	/**
	 * 之前是利用解析jison，现在网页改版，改是利用Xpath定位解析。
	 *  把日志信息改成英文，避免中文乱码现象。
	 */
	@Override
	public void setPrice() throws ProductPriceIsNullException {
		if(!"".equals(priceXpath)){
			String price = null;
			Node highPriceNode=TemplateUtil.getNode(root, priceXpath);
			Node lowPriceNode=TemplateUtil.getNode(root, discountPriceXpath);
			if(highPriceNode!=null){
				price = trimPrice(highPriceNode.getTextContent()).trim();
//				System.out.println("price===1==="+price);
				if (price == null || price.isEmpty() || price.equals("0")) {
					throw new ProductPriceIsNullException(url
							+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
				}
				if(lowPriceNode!=null){
					price = trimPrice(lowPriceNode.getTextContent()).trim();
//					System.out.println("price===2==="+price);
					if (price == null || price.isEmpty() || price.equals("0")) {
						throw new ProductPriceIsNullException(url
								+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
					}
				}
				product.setPrice(price);
			}else{
				throw new ProductPriceIsNullException(url
						+ "-->Error---Failed To Get Product Sales Price!  setPrice Exception No Matching!");
			}
		}
	}
	
	@Override
	public void setMaketPrice() {
		if(!"".equals(marketPriceXpath)){
			String marketPrice ="";
			Node marketPriceNode=TemplateUtil.getNode(root, marketPriceXpath);
			if(marketPriceNode!=null){
				marketPrice = marketPriceNode.getTextContent();
				if(!"".equals(marketPrice)){
					marketPrice=trimPrice(marketPrice);
					product.setMaketPrice(marketPrice);
				}
			}
		}
	}

	@Override
	public void setClassic() {
		StringBuffer sb = new StringBuffer();
		NodeList node_locationList = TemplateUtil.getNodeList(root, classicXpath);
		if (node_locationList.getLength() > 0) {
			for(int i=0;i<node_locationList.getLength();i++){
				String value=node_locationList.item(i).getTextContent().replaceAll("[\\s]{1,}", "");
				if(value != null && !"".equals(value)){
					sb.append(value);
				}
				if(i < node_locationList.getLength()-1){
					sb.append("[xm99]");
				}
			}
			String classicValue=sb.toString();
			if(classicValue!=null && !"".equals(classicValue)){
				classicValue=classicValue.replaceAll("首页\\[xm99\\]", "");
				product.setClassic(classicValue);
			}
		}
	}
	
	@Override
	public void setCategory() {
		String category=product.getClassic();
		if(category!=null && !"".equals(category)){
			product.setCategory(category);
		}
	}
	
	
//	@Override
//	public void setOrgPic() {
//		String img = (String) map.get("flag");// 21-136-999
//		if (img != null && !img.equals("") && img.indexOf("-") > 0) {
//			String[] imgPaths = img.split("-");
//			if (imgPaths.length == 3) {
//				img = "http://img2.icson.com/product/mpic/" + imgPaths[0] + "/"
//						+ imgPaths[1] + "/" + img + ".jpg";
//			} else {
//				LOG.info("解析图片路径时页面出错,解析的图片节点不属于xx-xx-xx格式");
//				return;
//			}
//		} else {
//			LOG.info("解析图片路径时页面出错");
//			return;
//		}
//		try {
//			product.setOrgPic(img);
//			product.setSmallPic(Constant.img_host + sellerCode + "/"
//					+ product.getPid() + Constant.ext_jpg);
//			product.setBigPic(Constant.img_host + sellerCode + "/big/"
//					+ product.getPid() + Constant.ext_jpg);
//			LoadingPicServer.addPicUrl(img, sellerCode, product.getPid());
//		} catch (Exception ex) {
//			LOG.error("图片解析错误");
//		}
//	}

	/**
	 * 由于页面规则更爱，改为通过二次请求获取详情内容。
	 * 把日志信息改成英文，避免中文乱码现象。
	 */
	@Override
	public void setContents() {
		StringBuffer sb = new StringBuffer();
		String xpath1 = "//DIV[@class='mod_detail_info id_features']";
		String xpath2 = "//DIV[@class='mod_detail_info id_link']";
		String value="";
		String contents=map.get("content");
		if(contents!=null && !"".equals(contents)){
			DocumentFragment contentsDOC=RequestUtil.getDocumentFragmentByString(contents, "utf-8");
			if(contentsDOC!=null){
				NodeList tmp = TemplateUtil.getNodeList(contentsDOC, xpath1);
				if(tmp != null){
					for (int i = 0; i < tmp.getLength(); i++) {
						Node node1 = tmp.item(i);
						TemplateUtil.getTextHelper(sb, node1);
					}	
				}
				tmp = TemplateUtil.getNodeList(contentsDOC, xpath2);
				if(tmp != null){
					for (int i = 0; i < tmp.getLength(); i++) {
					   Node node1 = tmp.item(i);
					   TemplateUtil.getTextHelper(sb, node1);
					}
				}
				value = sb.toString().replaceAll("[\\s\n]", "").trim();
			}else{
				LOG.info(url+"-->Praser Product Contents Information is NULL");
				value="此商品没有提供商品详情内容";
			}
		}else{
			LOG.info(url+"-->Praser Product Contents Information is NULL -->" + url);
			value="此商品没有提供商品详情内容";
		}
		product.setContents(value);  
	}

	/**
	 * 1、通过组装url。 2.通过url,返回json字符串。 3.解析json中的data属性。该属性值是HTML字符串
	 * 4.解析HTML字符串为DocumentFragment对象，最终得到所有tr的值
	 * 
	 * 由于页面规则更爱，改为通过二次请求获取商品参数内容。
	 *  把日志信息改成英文，避免中文乱码现象。
	 */
	@Override
	public void setMparams() {
		String value="";
		String maparams=map.get("maparmas");
	    if(maparams!=null && !"".equals(maparams)){
	    	DocumentFragment maparamsDOC=RequestUtil.getDocumentFragmentByString(maparams, "utf-8");
			if(maparamsDOC!=null){
				value = TemplateUtil.getMParas(maparamsDOC, mparasXpath);
				if (value != null && !"".equals(value)) {
					value = TemplateUtil.formatMparams(value);
				}
			}else{
				LOG.info(url+"-->Praser Product Maparmas Information is NULL");
				value="此商品没有提供参数";
			}
	    }else{
	    	LOG.info(url+"-->Praser Product Maparmas Information is NULL");
	    	value="此商品没有提供参数";
		}
	    product.setMparams(value);
	}

	/**
	 * 有货页面：http://item.yixun.com/item-41325.html
	 * 无货页面：http://item.yixun.com/item-349860.html
	 */
	@Override
	public void setIsCargo() {
		String isCargo=map.get("shipping_desc");
		if(isCargo!=null && !"".equals(isCargo) && isCargo.contains("有货")){
			product.setIsCargo(1);
		}else{
			product.setIsCargo(0);
		}
	}
	
	/**
	 * 获取列表URL规则:添加过滤无用外链和将url格式化，去重
	 
	@Override
	public List<imageProduct> nextUrlList(URL baseUrl,String content) {
		List<imageProduct> urlList = new ArrayList<imageProduct>();
		Set<imageProduct> urlSet = new LinkedHashSet<imageProduct>();
		URL urls = null;
		JSONObject jsonObj = null;
		String resultJson = "";
		try {
		     resultJson = Formatter.getJson(content);
			   if (!StrUtil.isEmpty(resultJson)) {
			     jsonObj = JSONObject.fromObject(resultJson);
			     if (resultJson.indexOf("data") >= 0) {
			      JSONObject productObject = jsonObj.getJSONObject("data");
			      if(productObject.toString().contains("banner")){
			    	  String banner=productObject.getString("banner");
			    	  if(!StrUtil.isEmpty(banner)){
				    	  JSONArray jsonArray = JSONArray.fromObject(banner);
					    	  for(int i=0;i<jsonArray.size();i++){
					    		  JSONObject commentOneJson = jsonArray.getJSONObject(i);
					    		  String productUrl = commentOneJson.getString("url");
					    		  String smallPic = commentOneJson.getString("img");
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
			      }else{
			    	  if(productObject.toString().contains("list")){
			    		  String list=productObject.getString("list");
			    		  if(!StrUtil.isEmpty(list)){
					    	  JSONArray jsonArray = JSONArray.fromObject(list);
						    	  for(int i=0;i<jsonArray.size();i++){
						    		  JSONObject commentOneJson = jsonArray.getJSONObject(i);
						    		  String productUrl = commentOneJson.getString("goodsUrl");
						    		  String smallPic="";
						    		  String productID = commentOneJson.getString("productID");
						    		  if(!StrUtil.isEmpty(productID)){
						    			  StringBuffer sb=new StringBuffer();
						    			  String yixunPerfix="http://img1.icson.com/product/pic200/";
						    			  String[] picArgs=productID.split("-");
						    			  if(picArgs.length>2){
						    				  for(int j=0;j<2;j++){
						    					  String argsValue=picArgs[j];
						    					  sb.append(argsValue+"/");
						    				  }
						    			  }
						    			  smallPic=yixunPerfix+sb.toString()+productID+".jpg";
						    		  }
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
			    	  }
			      }
			     }
			   }
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName()+" JSON Error! url : "+baseUrl.toString());
		}
		urlList.addAll(urlSet);
		return urlList;
	}
	*/
	
	/**
	 * 添加过滤无用外链的条件
	 * 只对两种页面做操作，列表url(http://searchex.yixun.com/)和商品url(http://item.yixun.com/) 其他的都过滤。
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
	 * 1.对符合商品正则的url进行截取，去掉？号后面的字符串
	 * 格式外链，将带参数的列表url进行格式化处理，提高解析效率。
	 * 格式外链，将带参数的列表url进行格式化处理，只对两种页面做处理：分类列表url和商品url。     
	 */
	public String formatSafeUrl(String url) {
		if(StrUtil.isEmpty(url)){
			return url;
		}
		String pid = null;
		if(isProductPage(url)){
			for(Pattern pattern:productUrlList){
				Matcher ma1=pattern.matcher(url);
				if(ma1.find()){
					pid=ma1.group(1);
					if(pid!=null && !"".equals(pid)){
						if(pid.matches("[-a-zA-Z0-9]+")){
							if(url.indexOf(".html?")>=0){
								url=url.substring(0,url.indexOf("?"));
							}
							if(url.indexOf("#review_box")>=0){
								url=url.substring(0,url.indexOf("#review_box"));
							}
							if(url.contains("item.51buy.com")){
								url = url.replaceAll("item.51buy.com", "item.yixun.com");
							}
						}
					}
					return url;
				}
			} 
		}else{
			//2013年8月29日修改，对域名searchex.yixun.com/html分类列表页进行格式化去重操作
			//http://searchex.yixun.com/html?YTAG=3.707217245021&path=705852t705856&area=2001
			//http://searchex.yixun.com/html?attr=55e5686&path=705852t705856&area=2001
			//====>http://searchex.yixun.com/html?path=705852t705856
			//http://searchex.yixun.com/html?path=705852t705856t707218t707231
			//-->http://searchex.yixun.com/html?path=705852t705856#nolink
             //-->http://searchex.yixun.com/html?path=705852t705856
			//http://searchex.yixun.com/html?path=705624&area=2001&page=2&size=32&charset=utf-8&YTAG=3.705702251000#category_container
			//http://searchex.yixun.com/html?path=705624&area=2001&page=2
			if(url.indexOf("searchex.yixun.com")>=0){
				if(url.indexOf("#nolink")>=0){
					url=url.substring(0,url.indexOf("#nolink"));
				}
				String base = "http://searchex.yixun.com/html?";
				String[] params = url.split("[&?]");
				Map<String, String> pmap = new TemplateUtil().formMap(params, "=");
				StringBuffer sb = new StringBuffer();
				if (pmap.get("path") != null) {
					String path = pmap.get("path");
					if(path.indexOf("t") > 1){
						path=path.substring(0, 13);
					}
					sb.append("path=");
					sb.append(URLEncoder.encode(path));
					if (pmap.get("page") != null) {
						sb.append("&page=");
						sb.append(pmap.get("page"));
					}
					url = base + sb.toString();
				}
				return url;
			}
//			2013年9月2日修改 都是旧的域名，都过滤处理，不做操作。
//			2013年8月29日修改 不对搜索页面search.51buy.com做处理，过滤。
			// http://list.51buy.com/111-0-6-11-40-0-7--.html
			// list.51buy.com 会跳转至 list.51buy.com,为防止search.51buy.com停用，直接转为search.yixun.com
//			if(url.indexOf("list.51buy.com") >= 0){
//				url = url.replaceAll("list.51buy", "search.yixun");
//			}
//			http://search.51buy.com/234--------.html?YTAG=3.234241200&areacode=2001
//			-->失效，跳转  http://www.yixun.com/
			
//			http://search.51buy.com/234-------1344e3712-.html?q=%B4%A5%C3%FE%B1%BE&areacode=1
//			-->http://searchex.yixun.com/html?key=%B4%A5%C3%FE%B1%BE
			
//			http://search.51buy.com/379--4-----7247e41628-.html?YTAG=3.21041101&q=%C2%B7%D3%C9%C6%F7&areacode=1
//			-->http://searchex.yixun.com/html?key=%C2%B7%D3%C9%C6%F7
//			if(url.indexOf("search.51buy.com")>=0 || url.indexOf("search.yixun.com") >= 0){
//				String baseSearchUrl="http://searchex.yixun.com/";
//				if(url.indexOf("?")>=0){
//					url=url.substring(0,url.indexOf("?"));
//				}
//				if(url.indexOf("--------")<0){
//					String baseUrl=url.substring(0,url.indexOf("--"));
//					url=baseUrl+"--------.html";
//				}
//				return url;
//			}
		}
		return url;
	}
	
	/**
	 * 2013年8月8日添加：手动拼接
	 * 下一页：
        http://searchex.yixun.com/html?path=705852t705856&area=2001&page=2&size=32&charset=utf-8
                   截后===http://searchex.yixun.com/html?path=705852t705856&page=2
             
      2013年8月8日修改，由于“下一页”，手动拼接page数：从页面解析该分类页面的总商品数量 totalNum，每一页的商品数量  pageSize，
                由此得出该分类一共有多少页，在根据当前页号intPageNo来进行判断是否是最后一页，是则不加1.
	 */
//	@Override
//	public String nextUrl(URL baseUrl) {
//		String nextUrl = "";
//		int intPageNo=0;
//		Node pageNoNode=TemplateUtil.getNode(root, "//INPUT[@id='curPageNo']/@value");   //获取当前是第几页
//		Node totalNumNode=TemplateUtil.getNode(root, "//INPUT[@id='totalNum']/@value");  //总共商品数量  作为判断是否是最后一页  
//		Node pageSizeNode=TemplateUtil.getNode(root, "//INPUT[@id='pageSize']/@value");  //每页的数量  作为判断是否是最后一页
//		if(pageNoNode!=null){
//			intPageNo=Integer.parseInt(pageNoNode.getTextContent().trim());
//			double totalNum=Double.parseDouble(totalNumNode.getTextContent().trim());   //总的商品数量
//			double pageSize=Double.parseDouble(pageSizeNode.getTextContent().trim());   //每页的商品的数量
//			if(totalNum > 0 && pageSize > 0){
//				BigDecimal bg = new BigDecimal(totalNum/pageSize);
//				double DoublePageNum = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//				int IntPageNum=(int) Math.round(DoublePageNum);
//				if(intPageNo < IntPageNum){
//					intPageNo=intPageNo+1;
//				}
//			}
//			String temUrl=baseUrl.toString();
//			String[] params=temUrl.split("&");
//			Map<String, String> pmap = new TemplateUtil().formMap(params, "=");
//			temUrl=temUrl.replaceAll("&page="+pmap.get("page"), "");
//			nextUrl = temUrl+"&page="+intPageNo;
//		}
//		if(isSafeUrl(nextUrl)){
//			return formatSafeUrl(nextUrl);
//		}
//		return "";
//	}
	
	/**
	 * 解析编码原始分类
	 */
	@Override
	public void setOriCatCode() {
		if(!oriCatCodeXpath.isEmpty()){
			Node oriCatCodeNode=TemplateUtil.getNode(root, oriCatCodeXpath);
			if(oriCatCodeNode!=null){
				StringBuffer oriCatCodeBuffer = new StringBuffer();
				String oriCatCodeValue=oriCatCodeNode.getTextContent();
				oriCatCodeValue=oriCatCodeValue.substring(oriCatCodeValue.indexOf("path=")+5, oriCatCodeValue.indexOf("&area="));
				String[] oriCatCodeArry=oriCatCodeValue.split("t");
				if(oriCatCodeArry.length > 0){
					for(int i=0;i<oriCatCodeArry.length;i++){
						if (i != 0) {
							oriCatCodeBuffer.append("#");
						}
						oriCatCodeValue=oriCatCodeArry[i];
						oriCatCodeBuffer.append(oriCatCodeValue);
					}
				}
				oriCatCodeValue=oriCatCodeBuffer.toString();
				if(oriCatCodeValue!=null && !"".equals(oriCatCodeValue)){
					product.setOriCatCode(oriCatCodeValue);
				}
			}
		}
	}
	
	
	@Override
	public void setShortName() {
		String brandValue="";
		String modelValue="";
		String shortNameByMparams=this.product.getMparams();
		if(shortNameByMparams !=null || !"".equals(shortNameByMparams)){
			String[] shortNameArry=shortNameByMparams.split("\\[xm99\\]");
			if(shortNameArry.length > 0){
				for(int i=0;i<shortNameArry.length;i++){
					String shortNameStr=shortNameArry[i].replaceAll("[\\s]{2,}", "").trim();
					if(shortNameStr.indexOf("品牌") >= 0){
						String[] shortNameStrArryByBrand=shortNameStr.split("=");
						if("品牌".equals(shortNameStrArryByBrand[0])){
							brandValue=shortNameStrArryByBrand[1];
							brandValue=brandValue.replaceAll("[\\s]{1,}", "\\[xm99\\]");
						}
					}
					if(shortNameStr.indexOf("型号") >= 0){
						String[] shortNameStrArryByModel=shortNameStr.split("=");
						if("型号".equals(shortNameStrArryByModel[0])){
							modelValue=" "+shortNameStrArryByModel[1];
						}
					}
				}
			}
		}
		if(brandValue==null || "".equals(brandValue)){
			brandValue=this.product.getBrand();
		}
		if(brandValue != null || !"".equals(brandValue)){
			if(brandValue.contains("[xm99]")){
				String[] brandValueArry = brandValue.split("\\[xm99\\]");
				for(int j=0;j<brandValueArry.length;j++){
					String brandValueStr=brandValueArry[j];
					 char c = brandValueStr.charAt(0);   
	                  if(((c >= 'a'&& c <= 'z') || (c >= 'A' && c <= 'Z'))){ 
	                	  brandValue=brandValueStr;
	                      break;
	                  }
				}
			}
			brandValue=brandValue.replaceAll("\\[xm99\\]", " ");
		}
		
			String shortName=brandValue+modelValue;
			if(shortName!=null || !"".equals(shortName)){
				this.product.setShortName(shortName);
				this.product.setShortNameDetail(shortName);
			}
		}
	
	@Override
	public void setLimitTime() {
		String limitTime="";
		String limitTimeXpath="//SPAN[@id='mod_time']/@end";
		Node limitTimeNode=TemplateUtil.getNode(root, limitTimeXpath);
		if(limitTimeNode!=null){
			String limitTimeValue=limitTimeNode.getTextContent().trim();
			if(!"".equals(limitTimeValue)){
				long limitTimeLong=Long.parseLong(limitTimeValue)*1000;
				if(limitTimeLong > 0){
					limitTime=String.valueOf(limitTimeLong);
					if(!"".equals(limitTime)){
//						System.out.println(new Date(limitTimeLong));
						this.product.setLimitTime(limitTime);
					}
				}
			}
		}
		if("".equals(limitTime) || "0".equals(limitTime)){
			this.product.setDiscountPrice("0");
			this.product.setLimitTime("0");
		}
	}
	
	public void initCommentContent(){
		try {
			String commentPid=this.product.getPid();
			if(!StrUtil.isEmpty(commentPid)){
				String commentSecondUrl="http://pinglun.yixun.com/json1.php?mod=reviews&act=getreviews&jsontype=str&pid="+commentPid;
				String commentContentValue=RequestUtil.getOfHttpURLConnection(commentSecondUrl, "gbk").toString();
				if(!StrUtil.isEmpty(commentContentValue)){
					JSONArray jsonArray = JSONArray.parseArray(commentContentValue);
					if(jsonArray.size()>0){
						JSONArrayMap.put("jsonArray", jsonArray);
					}
				}
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName()+": The initCommentContent Is Parser Error! "+url);
		}
	}
	
	@Override
	public void setCommentList(){
		//初始化评论内容文本
		initCommentContent();
		try{
			JSONArray jsonArray=JSONArrayMap.get("jsonArray");
			int arraySize = jsonArray == null ? 0 : jsonArray.size();
			if (0 < arraySize) {
				List<Comment> clist = new ArrayList<Comment>();
				for (int i = 0; i < arraySize; i++) {
					JSONObject o = jsonArray.getJSONObject(i);
					if (o != null) {
						String cid = StringUtils.trimToEmpty(o.getString("id"));
						if (StringUtils.isNotEmpty(cid)) {
							String name = StringUtils.trimToEmpty(o.getString("user_name"));
							String content = StringUtils.trimToEmpty(o.getString("content"));
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
			
		}catch(Exception e){
		}
	}
	
	public static void main(String[] args) throws Exception, ProductIDIsNullException {
		Parser51buyNew test = new Parser51buyNew();
		test.test1();
//		URL urls = new URL(url);
//		List<String> list = test.nextUrlList(urls);
//		for(String str : list){
//			System.out.println(str);
//		}
//		System.out.println(list.size());
//		
//		String aa = test.nextUrl(urls);
//		 System.out.println("下一页--->"+aa);
		 //
	}
	
	public void test1() throws Exception, ProductIDIsNullException{
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\yixun.html"), "gb2312");
		Parser51buyNew test = new Parser51buyNew(root);
		test.product = new OriProduct();
//		 test.setId();
//		 System.out.println("id=====>: " + test.product.getId() + ">>>>"
//		 + test.product.getPid());
		
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
//		 
//		 test.setCommentSix();
//		 System.out.println("商品评论Six======>:"+ test.product.getCommentSix().getCommentId()+">>>>" + test.product.getCommentSix().getCommentContent());
//		 
//		 test.setCommentSeven();
//		 System.out.println("商品评论Seven======>:"+ test.product.getCommentSeven().getCommentId()+">>>>" + test.product.getCommentSeven().getCommentContent());
//		 
//		 test.setCommentEight();
//		 System.out.println("商品评论Eight======>:"+ test.product.getCommentEight().getCommentId()+">>>>" + test.product.getCommentEight().getCommentContent());
//		 
//		 test.setCommentNine();
//		 System.out.println("商品评论Nine======>:"+ test.product.getCommentNine().getCommentId()+">>>>" + test.product.getCommentNine().getCommentContent());
//		 
//		 test.setCommentTen();
//		 System.out.println("商品评论Ten======>:"+ test.product.getCommentTen().getCommentId()+">>>>" + test.product.getCommentTen().getCommentContent());
		 
//		test.setProductName();
//		System.out.println("商品名称======>:" + test.product.getProductName());
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
//		 test.setPrice();
//		 System.out.println("价钱=====> ：" + test.product.getPrice());
//		 
//		 test.setMaketPrice();
//		 System.out.println("市场价=====>：" + test.product.getMaketPrice());
//		
//		 test.setDiscountPrice();
//		 System.out.println("折扣价钱=====> ：" + test.product.getDiscountPrice());
//		 
//		 test.setOrgPic();
//		 System.out.println("图片=====>：" + test.product.getOrgPic());
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
//		 System.out.println("商品短名称====》:" + test.product.getShortName());
//		 
//		 test.setshortNameDetail();
//		 System.out.println("第二种商品短名称====》:" + test.product.getShortNameDetail());
//		 
//		 test.setCategory();
//		 System.out.println("中文原始分类====》:" + test.product.getCategory());
//		 
//		 test.setOriCatCode();
//		 System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
////
//		 test.setCheckOriCatCode();
//		 System.out.println("校验中文原始分类====》:" + test.product.getCheckOriCatCode());
//		
		 test.setPrice();
		 System.out.println("当前价格=====>:" + test.product.getPrice());
		 
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
//			url = new URL("http://searchex.yixun.com/html?path=705948");
//			List<String> list = test.nextUrlList(url);
//			for (int i = 0; i < list.size(); i++) {
//				System.out.println(list.get(i));
//			}
//			System.out.println(list.size());
//			String aa=test.nextUrl(url);
//			System.out.println("下一页===》"+aa);
//		} catch (MalformedURLException e) {
//		}
		
	}
	

}
