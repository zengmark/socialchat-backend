package com.socialchat.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;

/**
 * @author 清闲
 * @description: 生成验证码工具类
 */
public class CodeGeneratorUtil {
    /**
     * 生成指定长度的验证码
     *
     * @param length 长度
     * @return
     */
    public static String generateCode(int length) {
        return UUID.randomUUID().toString().substring(0, length);
    }

    /**
     * 雪花算法生成用户注册的id
     */
    public static long snowflake() {
        return IdUtil.getSnowflakeNextId();
    }
}
