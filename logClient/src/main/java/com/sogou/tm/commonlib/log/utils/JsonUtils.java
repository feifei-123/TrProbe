package com.sogou.tm.commonlib.log.utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by zhangcb on 2018/7/20.
 */

public class JsonUtils {

    /**
     * JSON转对象
     * @param json JSON字符串
     * @param clazz 对象类型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        Gson gson = new Gson();
        T obj = gson.fromJson(json, clazz);

        return obj;
    }

    public static <T> T fromJson(String json, Type type) {

        Gson gson = new Gson();
        T obj = gson.fromJson(json, type);

        return obj;
    }

    /**
     * JSON转对象
     *
     * Type type = new TypeToken<List<类>>() {}.getType();
     *
     * @param json JSON字符串
     * @param typeOfT 对象类型
     * @return 对象
     */
    public static <T> List<T> fromJsonArray(String json, Type typeOfT) {

        Gson gson = new Gson();
        List<T> obj = gson.fromJson(json, typeOfT);

        return obj;
    }

    /**
     * 对象对JSON
     * @param obj 对象
     * @return json字符串
     */
    public static <T> String toJson(T obj) {
        Gson gson = new Gson();
        String json = gson.toJson(obj);

        return json;
    }

}
