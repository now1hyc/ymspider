package com.ym.nutch.obj;

public class ImageProduct {
	private String smallPic;
	private String oriUrl;

	
	public ImageProduct(String smallPic, String oriUrl) {
		super();
		this.smallPic = smallPic;
		this.oriUrl = oriUrl;
	}

	public String getSmallPic() {
		return smallPic;
	}

	public void setSmallPic(String smallPic) {
		this.smallPic = smallPic;
	}

	public String getOriUrl() {
		return oriUrl;
	}

	public void setOriUrl(String oriUrl) {
		this.oriUrl = oriUrl;
	}

	@Override
	public String toString() {
		return "imageProduct [smallPic=" + smallPic + ", oriUrl=" + oriUrl
				+ "]";
	}

}
