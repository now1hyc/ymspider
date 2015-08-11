package com.ym.nutch.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class GsonParser {

	public static final Logger log = LoggerFactory.getLogger(GsonParser.class);

	private static Gson gson = new Gson();

	private String data;
	private GsonParam p;

	public static void main(String[] es) {
		String json = "{\"cmd\":\"0001\",\"app\":\"android\"}";
		json = "sfdsdfdsffs";
		GsonParser jp = new GsonParser(json);
		System.out.println(jp.getP().getCmd());

		System.out.println(safeJson(json));
	}

	public GsonParser(String data) {
		this.data = data;
		try {
			this.p = gson.fromJson(this.data, GsonParam.class);
		} catch (JsonParseException e) {
			log.error("init Gson error:" + e);
			this.p = new GsonParam();
		}
	}

	public static boolean safeJson(String data) {
		if (StrUtil.isEmpty(data)) {
			return false;
		}
		try {
			gson.fromJson(data, GsonParam.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public GsonParam getP() {
		return p;
	}

	public void setP(GsonParam p) {
		this.p = p;
	}

}
