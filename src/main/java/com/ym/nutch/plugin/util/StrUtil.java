package com.ym.nutch.plugin.util;

import java.math.BigDecimal;

public class StrUtil {
	
	public static String trim(String str) {
		return str == null ? "" : str.trim();
	}

	public static boolean isEmpty(String str) {
		return "".equals(trim(str));
	}

	public static int trimInteger(String str){
		if(str==null){
			return 0;
		}else{
			return Integer.parseInt(str);
		}
	}
	
	public static BigDecimal getBigDecimal(String doubleString){
		try{
			BigDecimal d = new BigDecimal(doubleString);
			return d;
		}catch(Exception ex){
			ex.printStackTrace();
			System.out.println("BigDecimal zhuan huan cuo wo");
			return new BigDecimal("0");
		}
	}
}
