package com.ym.nutch.parse.template;



public class PriceTool {
	public static double getdRatio(double dRatio){
		if (dRatio >=1 || dRatio <=0 ) return 0 ;
		String dRatioM100 = String.valueOf(dRatio *100);
		String dRatioM100Array[] = dRatioM100.split("\\.");
		int dRatioM100ArrayLen = dRatioM100Array.length ;
		//刚好是小数点后有两位
		if (dRatioM100ArrayLen == 1){
			//刚好是小数点后大于两位
		}else if(dRatioM100ArrayLen == 2){
			double dRatio1 = Double.parseDouble(dRatioM100Array[0]);
			double dRatio2 = Double.parseDouble(dRatioM100Array[1]);
			if (dRatio1==0&&dRatio2>0){
				dRatio=0.01;
			}else{
				dRatio = dRatio1/100 ;
			}
			
		}else{
			//其他情况为出错
			dRatio = 0 ;
		}
		return dRatio ;
	}
	
}



