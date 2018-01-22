package com.cn.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;


/**
 * Created by vicmob_yf002 on 2017/10/26.
 * json解析封装
 */

public class JsonUtils {

    public static JsonArray jsonParse(String str) {
        //Json的解析类对象
        JsonParser parser = new JsonParser();
        //将JSON的String 转成一个JsonArray对象
        JsonArray jsonArray = parser.parse(str).getAsJsonArray();
        return jsonArray;
    }
}
