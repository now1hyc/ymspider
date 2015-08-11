package com.ym.nutch.obj;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class OriProduct implements Serializable {
	private static final long	serialVersionUID	= 1L;

	private String				pid;
	private String				classic;
	private String				category;
	private String				theThird;
	private String				shortName;
	private String				productName;
	private String				title;
	private String				keyword;
	private String				price;
	private String				maketPrice;
	private String				discountPrice;
	private String				contents;
	private String				mparams;
	private String				orgPic;
	private String				smallPic;
	private String				bigPic;
	private String				sellerCode;
	private String				seller;
	private String				product_url;
	private String				opid;
	private String				brand;
	private int					isCargo;
	protected Timestamp			creatTime			= new Timestamp(System.currentTimeMillis());
	protected Timestamp			updateTime			= creatTime;
	protected int				state				= 0;
	protected String			opCode				= "";
	protected String			oriCatCode;
	protected String			checkOriCatCode;
	protected String			shortNameDetail;
	protected String			limitTime;
	protected String			d1ratio;
	protected String			d2ratio;
	protected int				totalComment;
	protected String			commentStar;
	protected String			commentPercent;
	protected int				hasComment			= 0;

	protected List<Comment>		clist;

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getClassic() {
		return classic;
	}

	public void setClassic(String classic) {
		this.classic = classic;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getTheThird() {
		return theThird;
	}

	public void setTheThird(String theThird) {
		this.theThird = theThird;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getMaketPrice() {
		return maketPrice;
	}

	public void setMaketPrice(String maketPrice) {
		this.maketPrice = maketPrice;
	}

	public String getDiscountPrice() {
		return discountPrice;
	}

	public void setDiscountPrice(String discountPrice) {
		this.discountPrice = discountPrice;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public String getMparams() {
		return mparams;
	}

	public void setMparams(String mparams) {
		this.mparams = mparams;
	}

	public String getOrgPic() {
		return orgPic;
	}

	public void setOrgPic(String orgPic) {
		this.orgPic = orgPic;
	}

	public String getSmallPic() {
		return smallPic;
	}

	public void setSmallPic(String smallPic) {
		this.smallPic = smallPic;
	}

	public String getBigPic() {
		return bigPic;
	}

	public void setBigPic(String bigPic) {
		this.bigPic = bigPic;
	}

	public String getSellerCode() {
		return sellerCode;
	}

	public void setSellerCode(String sellerCode) {
		this.sellerCode = sellerCode;
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(String seller) {
		this.seller = seller;
	}

	public String getProduct_url() {
		return product_url;
	}

	public void setProduct_url(String product_url) {
		this.product_url = product_url;
	}

	public String getOpid() {
		return opid;
	}

	public void setOpid(String opid) {
		this.opid = opid;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public int getIsCargo() {
		return isCargo;
	}

	public void setIsCargo(int isCargo) {
		this.isCargo = isCargo;
	}

	public Timestamp getCreatTime() {
		return creatTime;
	}

	public void setCreatTime(Timestamp creatTime) {
		this.creatTime = creatTime;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getOpCode() {
		return opCode;
	}

	public void setOpCode(String opCode) {
		this.opCode = opCode;
	}

	public String getOriCatCode() {
		return oriCatCode;
	}

	public void setOriCatCode(String oriCatCode) {
		this.oriCatCode = oriCatCode;
	}

	public String getCheckOriCatCode() {
		return checkOriCatCode;
	}

	public void setCheckOriCatCode(String checkOriCatCode) {
		this.checkOriCatCode = checkOriCatCode;
	}

	public String getShortNameDetail() {
		return shortNameDetail;
	}

	public void setShortNameDetail(String shortNameDetail) {
		this.shortNameDetail = shortNameDetail;
	}

	public String getLimitTime() {
		return limitTime;
	}

	public void setLimitTime(String limitTime) {
		this.limitTime = limitTime;
	}

	public String getD1ratio() {
		return d1ratio;
	}

	public void setD1ratio(String d1ratio) {
		this.d1ratio = d1ratio;
	}

	public String getD2ratio() {
		return d2ratio;
	}

	public void setD2ratio(String d2ratio) {
		this.d2ratio = d2ratio;
	}

	public int getTotalComment() {
		return totalComment;
	}

	public void setTotalComment(int totalComment) {
		this.totalComment = totalComment;
	}

	public String getCommentStar() {
		return commentStar;
	}

	public void setCommentStar(String commentStar) {
		this.commentStar = commentStar;
	}

	public String getCommentPercent() {
		return commentPercent;
	}

	public void setCommentPercent(String commentPercent) {
		this.commentPercent = commentPercent;
	}

	public int getHasComment() {
		return hasComment;
	}

	public void setHasComment(int hasComment) {
		this.hasComment = hasComment;
	}

	public List<Comment> getClist() {
		return clist;
	}

	public void setClist(List<Comment> clist) {
		this.clist = clist;
	}

}