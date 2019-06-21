package com.cqebd.live;

import androidx.annotation.NonNull;

/**
 * Created by @author xiaofu on 2019/5/16.
 */
public class StringUtils {
    public static String getS(@NonNull byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length);
    }

    public static String replace(String content){
        return content.replaceAll("","");
    }
}
