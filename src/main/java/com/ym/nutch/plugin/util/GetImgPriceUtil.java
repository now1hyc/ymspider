package com.ym.nutch.plugin.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.seaning.www.codes.biao.ImageAnalytic;

public class GetImgPriceUtil {
	private static final Log logger = LogFactory.getLog(GetImgPriceUtil.class);
	private static ImageAnalytic imgAnaly = new ImageAnalytic();

	public static String getPriceAsStr(String url) {

		String strPri = "";
		try {
			strPri = imgAnaly.getNumByUrl(url);
			// Double dd = Double.parseDouble(strPri);
			// strPri = dd.toString();
		} catch (Exception e) {
			logger.info("价格图片解析不成功!");
			return null;
		}

		return strPri;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
