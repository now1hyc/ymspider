package com.ym.nutch.plugin.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDealUtil {

	public static String dealClassic(String classis,List<String> filters){
		
		StringBuffer sb = new StringBuffer();
		
		String[] classises = classis.split("\\[xm99\\]");
		
		boolean flag = true;
		for(int i=0;i<classises.length;i++){
			flag = true;
			for(String f:filters){
				if(f.trim().length()>0){
					if(classises[i].toLowerCase().indexOf(f.toLowerCase())>=0){
						flag = false;
						break;
					}
				}
			}
			if(flag){
				sb.append(classises[i].trim()).append("[xm99]");	
			}
		}
		
		String result = sb.toString();//过滤结尾的[xm99]字符串
		while(result.endsWith("[xm99]")){
			result = result.substring(0,result.length()-"[xm99]".length());
		}
		return result;
	}
	//mparams=品牌=孔艺轩[xm99]型号=TJYA641[xm99]类别=水槽及配件[xm99] [xm99]1.水槽主体[xm99]2.龙头[xm99]龙头三选一，拍下水留言备注货号，不备注一律发默认款[xm99]3.下水器[xm99]3.下水管[xm99]5.三角阀[xm99]6.龙头进水管[xm99]水槽安装示意图=[xm99] [xm99] [xm99] [xm99]
	
	public static String dealMparam(String params,String value){
		String[] paramses = params.split("\\[xm99\\]");
		for(String param:paramses){
			if(param.indexOf(value)>=0){
				if(param.split("=")[1]==null || param.split("=")[1].equals("null")){
					continue;
				}
				return param.split("=")[1];
			}
		}
		return "";
	}

	public static Map<String,String> dealCategory(String brand,String classic){
		String[] brandStrings = brand.split(" |\\(|（");
		Map<String,String> map = new HashMap<String,String>();
		String category = "";
		String shortName = "";
		String brandTemp = brand;
		String[] classicArr = classic.split("\\[xm99\\]");
		String c = "";
		int end = 0;
		for(int i=0;i<classicArr.length;i++){
			c = classicArr[i].toLowerCase().replaceAll("\\s+","");
			if("".equals(c.trim())){
				continue;
			}
			for(String brandName:brandStrings){
				brandName = brandName.toLowerCase().replaceAll("\\s+","");
				if(c.length()>=brandName.length()){
					if(c.indexOf(brandName)>=0){
						end = i;
						break;
					}
				}else{
					if(brandName.indexOf(c)>=0){
						end = i;
						break;
					}
				}
			}
			if(end>0){
				break;
			}
		}
		if(end>0){
			if(end==classicArr.length-2){//说明品牌放到倒数第二级
				shortName = classicArr[classicArr.length-1];
			}else{
				shortName = classicArr[classicArr.length-1];
			}
			StringBuffer sb = new StringBuffer();
			for(int i=0;i<end;i++){
				if(i!=end-1){
					sb.append(classicArr[i].trim()).append("[xm99]");	
				}else{
					sb.append(classicArr[i].trim());
				}
			}
			category = sb.toString();
			if("".equals(brandTemp)){
				brandTemp = classicArr[end];	
			}
		}
		map.put("category",category);
		map.put("shortName",shortName);
		map.put("brand",brandTemp);
		return map;
	}

	public static void main(String[] args) {
		Map map = dealCategory("爱尔玛","首页 [xm99] 数码/相机[xm99] 摄影配件 [xm99] 清洁用品 [xm99]Erma爱尔玛[xm99]Erma爱尔玛 [xm99]Erma Erma爱尔玛 ERMA 爱尔玛 超极细纤维镜头布 便携型");
        System.out.println(map.get("category"));
        System.out.println(map.get("shortName"));
        System.out.println(map.get("brand"));
/*        String brand = dealMparam("型号=null[xm99]品牌=Apple 苹果[xm99]型号=MC696CHA[xm99]颜色=绿色[xm99]类型=MP3[xm99]存储=null[xm99]容量=16GB[xm99]扩展卡=不支持[xm99]扩展卡类型=无[xm99]菜单语言=多语言[xm99]屏幕=null[xm99]屏幕范围=0-2寸[xm99]屏幕尺寸=2英寸以下[xm99]显示屏类型=触摸屏[xm99]分辨率=240*240[xm99]操作方式=触摸操作[xm99]性能=null[xm99]音频格式=AAC (8 至 320 Kbps)、受保护 AAC (来自 iTunes Store)、HE-AAC、MP3 (8 至 320 Kbps)、MP3 VBR、Audible (2、3、4 类格式 Audible Enhanced Audio、AAX 和 AAX+)、Apple Lossless、AIFF 以及 WAV[xm99]视频格式=不支持[xm99]图片=不支持[xm99]其他功能=null[xm99]录音功能=不支持[xm99]FM功能=支持[xm99]电子书功能=不支持[xm99]电池=null[xm99]续航时间=锂电池，24 小时音乐播放时间[xm99]规格=null[xm99]接口=USB 2.0[xm99]附件=null[xm99]附件描述=iPod nano、Apple 耳机、Dock Connector to USB 线缆、快速入门指南以及重要的产品信息","品牌");
        System.out.println(brand);*/

//		String brand = "华硕（ASUS）";
//		String[] brandStrings = brand.split(" |\\(|\\（");
//		for(String str:brandStrings){
//			System.out.println("str===="+str);
//		}
        
	}
}
