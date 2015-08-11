package com.ym.nutch.plugin.parse;

import java.io.File;
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
import com.ym.nutch.exception.ProductPriceIsNullException;
import com.ym.nutch.obj.OriProduct;
import com.ym.nutch.parse.template.ParserParent;
import com.ym.nutch.plugin.util.ParserUtil;
import com.ym.nutch.plugin.util.StrUtil;
import com.ym.nutch.plugin.util.TemplateUtil;
import com.ym.nutch.plugin.util.URLUtil;

public class ParseJuMeiNew extends ParserParent {

	public static final Logger LOG = LoggerFactory.getLogger(ParseJuMeiNew.class);
	
	// http://mall.jumei.com/aloe/product_29216.html
	// http://mall.jumei.com/popular/product_7494.html
	// http://mall.jumei.com/za/product_34241.html
	// 商品页正则：url中包含一级品牌目录，不包含的，会有一次跳转
	private Pattern itemFlag1 = Pattern.compile("http://mall.jumei.com/([a-zA-Z]+)/(product_[\\d]+).html.*");
	private Pattern itemFlag2 = Pattern.compile("http://mall.jumei.com/(product_[\\d]+).html.*");// 标准商品地址
	//手机版商品url：http://m.jumei.com/i/MobileWap/mall_view?product_id=2332
	//http://m.jumei.com/i/MobileWap/deal_view?hash_id=bj140122p2925&tag=intro&parent_category_id=0&sort=popular_desc
	private Pattern itemFlag3 = Pattern.compile("http://m.jumei.com/i/mobilewap/([0-9a-zA-Z&=_?]+).*?");// 标准商品地址
	// http://bj.jumei.com/i/deal/bj140122p2925.html    
	private Pattern itemFlag4 = Pattern.compile("http://bj.jumei.com/i/deal/([0-9a-zA-Z]+).*?");
	private String baseItem = "http://mall.jumei.com/";
	
	// 分类列表正则
	private Pattern listFlag1 = Pattern.compile("http://mall.jumei.com/products/([\\d-]+).html.*");// group=1 for cid
	private Pattern listFlagCat = Pattern.compile("http://mall.jumei.com/products/[\\d-]+.html.*new_catid=([\\d]+).*");// group=1 for newCid
	private String baseList = "http://mall.jumei.com/products/";
	
	private List<String> seeds = new ArrayList<String>();
	
	public ParseJuMeiNew() {
		super();
		init();
	}

	public ParseJuMeiNew(DocumentFragment doc) {
		super(doc);
		init();
	}

	public void init() {
		sellerCode = "1031";
		seller = "聚美优品";
		idXPath = "//A[@id='btnilike']/@pid";
		priceXpath = "//SPAN[@id='mall_price']"+" | //SPAN[@class='newdeal_deal_price en']";
		marketPriceXpath="//SPAN[@id='info_market_price']"+" | //TD[@class='price en'][1]";
		brandXpath = "//SPAN[@itemprop='brand']"+" | //DIV[@class='newdeal_breadcrumbs']/DIV[3]/A";
		classicXpath = "//DIV[@class='location']"+" | //DIV[@class='newdeal_breadcrumbs']";
		productNameXpath="//H1[@class='title']/SPAN"+" | //DIV[@class='newdeal_breadcrumbs_wrap_b']";
		shortNameXpath="//DIV[@class='location']/H1";
		imgXpath = "//DIV[@class='pic']//descendant::IMG/@src"+" | //IMG[@id='product_img']/@src"+
		           " | //DIV[@class='newdeal_deal_left_black']/IMG/@src"+
		           " | //DIV[@class='newdeal_pic']/IMG/@src";
		mparasXpath = "//DIV[@id='product_parameter']/TABLE//descendant::TR";
		contentDetailXpath = "//DIV[@id='product_parameter']//descendant::DIV[@style]";
		nextPageXpath = "//A[@class='next']/@href";
		
		// 合法url规则(只接受，商品页+列表页)
		productFlag = itemFlag2;
		throughList.add(itemFlag1);
		throughList.add(itemFlag2);
		throughList.add(listFlag1);
		// 商品页地址规则
		productUrlList.add(itemFlag1);
		productUrlList.add(itemFlag2);
		productUrlList.add(itemFlag3);
		productUrlList.add(itemFlag4);
		// 提取外链xpath统一定义
		outLinkXpath.add("//DIV[@class='num_warp_list_name']/A/@href");// 分类列表页，提取商品url
		outLinkXpath.add("//UL[@class='mc_items']/LI/H3/A/@href"); // 商城首页，解析一级分类地址
//		outLinkXpath.add("//DIV[@class='sub_item']/H2/A/@href");// 商城首页，解析二级分类地址，暂不获取
		// 种子地址，不过滤
		seeds.add("http://mall.jumei.com");
	}

	@Override
	public void setId() throws ProductIDIsNullException {
		Node idNode = TemplateUtil.getNode(root, idXPath);
		String pid = idNode.getTextContent().replaceAll("[\\s]", "");
		if (pid != null || !"".equals(pid)) {
			String id = sellerCode + pid;
			product.setPid(id);
			product.setOpid(pid);
		} else {
			throw new ProductIDIsNullException(url + "-->Error---Product Id Is Null！");
		}
	}

	@Override
	public void setPrice() throws ProductPriceIsNullException {
		String price = null;
		Node priceNode = TemplateUtil.getNode(root, priceXpath);
		if (priceNode != null) {
			price = trimPrice(priceNode.getTextContent()).replaceAll("[\\s]", "");
			product.setPrice(price);
		} else {
			throw new ProductPriceIsNullException(url
					+ "未能获取商品销售价格！setPrice异常不匹配");
		}
	}

	// 品牌
	@Override
	public void setBrand() {
		Node brandNode = TemplateUtil.getNode(root, brandXpath);
		if (brandNode!=null) {
			String brand=brandNode.getTextContent().trim();
			if(brand!=null && !"".equals(brand)){
				product.setBrand(brand);
			}
		}else{
			LOG.info("未能正常获取该商品的品牌信息！ " +url);
		}
	}

	public boolean isSafeUrl(String url){
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
	 * 
	 * @param baseUrl
	 * @param content
	 * @return
	 
	@Override
	public List<imageProduct> nextUrlList(URL baseUrl,String content) {
		List<imageProduct> urlList = new ArrayList<imageProduct>();
		Set<imageProduct> urlSet = new LinkedHashSet<imageProduct>();
		URL urls = null;
		try {
			String urlTemp = baseUrl.toString().toLowerCase();
			if(urlTemp.matches("http\\://m\\.jumei\\.com/i/mobilewap/([a-zA-Z_]+)\\?([a-zA-Z_]+)\\_id\\=([a-zA-Z0-9]+).*?")){
				String imgXpath="//INPUT[@type='image']/@src"+" | //P[@class='productPic']/IMG/@src";
				Node imgNode=TemplateUtil.getNode(root,imgXpath );
				if(imgNode!=null){
					String smallPic=imgNode.getTextContent();
					if(!StrUtil.isEmpty(smallPic)){
						try{
							if(!StrUtil.isEmpty(urlTemp) && !StrUtil.isEmpty(smallPic)){
								imageProduct image=new imageProduct(smallPic,urlTemp);
								urlSet.add(image);
								LOG.info("imageProduct :"+image);
							}
						}catch(Exception e){
							LOG.info(this.getClass().getSimpleName()+"  resolveURL Error! url: "+baseUrl.toString());
						}
					}
				}
			}else{
				Document doc = Jsoup.parse(content);
				Elements elements=doc.getElementsByTag("A");
				if(elements.size() > 0){
					for (int i = 0; i < elements.size(); i++) {
						Element urlListEle = elements.get(i);
						String productUrl=urlListEle.attr("href");
						String smallPic="";
						try{
							smallPic=urlListEle.getElementsByClass("item_image").get(0).getElementsByTag("img").attr("data-original");
						}catch(Exception ex){
							continue;
						}
//						if(smallPic.contains("no_90_90.png")){
//							smallPic=urlListEle.getElementsByTag("img").attr("imgdata");
//						}else if(smallPic.contains("no_308_108.png") || smallPic.contains("no_100_100.png")){
//							smallPic=urlListEle.getElementsByTag("img").attr("imgsrc");
//						}
						if(!smallPic.startsWith("http://") || smallPic.startsWith("http://s1.jmstatic.com/"))
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
			}
		} catch (Exception e) {
			LOG.info(this.getClass().getSimpleName()+"  Parser Error! url: "+baseUrl.toString());
		}
		urlList.addAll(urlSet);
		return urlList;
	}
	*/
	
	public String formatSafeUrl(String url) {
		if(StrUtil.isEmpty(url)){
			return url;
		}
		Matcher m = null;
		String cat = "";
		// 商品页格式化去重
		if(isProductPage(url)){
			String pid = "";
			m = itemFlag1.matcher(url);
			// 带品牌的url直接转为不带品牌的，多一次跳转，却避免了重复商品多个url
			if(m.find()){
//				String brand = m.group(1);
				pid = m.group(2);
//				return baseItem + brand + "/" + pid + ".html";
				url=baseItem + pid + ".html";
				return url;
			}
			m = itemFlag2.matcher(url);
			if(m.find()){
				if(url.endsWith("html")){
					return url;
				}
				pid = m.group(1);
				url=baseItem + pid + ".html";
				return url;
			}
		}
		// 分类列表页格式化去重
		m = listFlag1.matcher(url);
		if(m.find()){
			cat = m.group(1);
			JumeiParam jp = new JumeiParam(cat);
			m = listFlagCat.matcher(url);
			if(m.find()){
				String newCat = m.group(1);
				jp.setP5(newCat);
			}
			url = baseList + jp.formCat();
		}
		return url;
	}
	
	
	class JumeiParam{
		private String c1;// 品牌
		private String c2;// 分类
		private String c3;// 功效
		private String p4 = "11";// 排序规则
		private String p5 = "1";// 分页码
		private String split = "-";
		private int len = 0;
		public JumeiParam() {
		}
		public String formCat(){
			return new StringBuffer(c1).append(split).append(c2).append(split).append(c3)
					.append(split).append(p4).append(split).append(p5).append(".html").toString();
		}
		public JumeiParam(String param){
			String[] params = param.split(this.split);
			this.len = params.length; 
			if(this.len >= 3){
				this.c1 = params[0];
				this.c2 = params[1];
				this.c3 = params[2];
			}
			if(this.len >= 5){
				this.p4 = params[3];
				this.p5 = params[4];
			}
			// http://mall.jumei.com/products/0-0-65-11-3.html
			// http://mall.jumei.com/products/16-0-14-11-1.html
			if(this.c2.equals("0")){
				this.c1 = "0";
				this.c3 = "0";
			}
		}
		public String getC1() {
			return c1;
		}
		public String getC2() {
			return c2;
		}
		public String getC3() {
			return c3;
		}
		public String getP4() {
			return p4;
		}
		public String getP5() {
			return p5;
		}
		public int getLen() {
			return len;
		}
		public void setC1(String c1) {
			this.c1 = c1;
		}
		public void setC2(String c2) {
			this.c2 = c2;
		}
		public void setC3(String c3) {
			this.c3 = c3;
		}
		public void setP4(String p4) {
			this.p4 = p4;
		}
		public void setP5(String p5) {
			this.p5 = p5;
		}
	}
	
	// 下一页的url
	@Override
	public String nextUrl(URL baseUrl) {
		String nextUrl = "";
		Node nextUrlNode = TemplateUtil.getNode(root, nextPageXpath);
		if (nextUrlNode != null) {
			String urlTarget = nextUrlNode.getTextContent();
			int index = urlTarget.indexOf("loadData('");
			if (index >= 0) {
				index = index + "loadData('".length();
				int end = urlTarget.indexOf("'", index + 1);
				urlTarget = urlTarget.substring(index, end);
			} else {
				try {
					URL urls = URLUtil.resolveURL(baseUrl, urlTarget);
					nextUrl = urls.toString();
				} catch (Exception e) {
					LOG.error("->nextUrl error url=" + baseUrl + ",info=" + e);
				}
			}
		}
		if(isSafeUrl(nextUrl)){
			return formatSafeUrl(nextUrl);
		}
		return "";
	}

	
	@Override
	public void setShortName() {
        if(!"".equals(shortNameXpath)){
        	Node shortNameNode=TemplateUtil.getNode(root, shortNameXpath);
        	if(shortNameNode!=null){
        		String shortName=shortNameNode.getTextContent().trim();
        		if(shortName!=null && !"".equals(shortName)){
        			product.setShortName(shortName);
        		}
        	}else{
        		LOG.info("没有正常获取该商品的短名称  "+url);
        	}
        }
	}
	
	public static void main(String[] args) throws Exception, ProductIDIsNullException {
		ParseJuMeiNew jumei=new ParseJuMeiNew();
		jumei.test1();
	}
	public void test1() throws Exception, ProductIDIsNullException{
		ParserUtil util = new ParserUtil();
		DocumentFragment root = util.getRoot(new File("D:\\mm\\wangzhan\\JuMei.html"), "utf-8");
		ParseJuMeiNew test = new ParseJuMeiNew(root);
		test.product = new OriProduct();
		
		test.setId();
		System.out.println("id=====>: "+test.product.getPid()+">>>>" +test.product.getOpid());
		
    	test.setProductName();
    	System.out.println("商品名称======>:"+test.product.getProductName());
    	
    	test.setClassic();
    	System.out.println("导航=====>: "+test.product.getClassic());
    	
		test.setBrand();
		System.out.println("品牌=====>："+ test.product.getBrand());
		
		test.setContents();
		System.out.println("content=====>："+test.product.getContents());
		
		test.setOrgPic();
		System.out.println("图片=====>："+test.product.getOrgPic());
		
		test.setPrice();
		System.out.println("价钱=====> ："+test.product.getPrice());
		
		test.setMparams();
    	System.out.println("mp内容=====> ："+test.product.getMparams());

    	test.setTitle();
    	System.out.println("标题=====>："+test.product.getTitle());
    	
    	test.setKeyword();
    	System.out.println("关键字=====>: "+ test.product.getKeyword());
    	
    	test.setShortName();
    	System.out.println("短名称=====>："+ test.product.getShortName());

		test.setshortNameDetail();
		System.out.println("第二种商品短名称======>:" + test.product.getShortNameDetail());
    	
		test.setIsCargo();
		System.out.println("是否有货====》:"+test.product.getIsCargo());

		test.setCategory();
		System.out.println("中文原始分类====》:" + test.product.getCategory());
		 
		test.setOriCatCode();
	    System.out.println("编码原始分类====》:" + test.product.getOriCatCode());
	    
	    test.setCheckOriCatCode();
	    System.out.println("中文混合原始分类====》:" + test.product.getCheckOriCatCode());
		 
		
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
		
//		String url="http://www.gome.com.cn/category/cat10000049-00-0-36-0-0-0-0-8-0-0.html";
//		URL urls = new URL(url);
//		List<String> list = test.nextUrlList(urls);
//		for(String str : list){
//			System.out.println(str);
//		}
//		System.out.println(list.size());
//		String aa = test.nextUrl(urls);
//		 System.out.println("下一页--->"+aa);
		
}
}
	
	