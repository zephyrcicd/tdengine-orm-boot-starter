package com.zephyrcicd.tdengineorm.util;


import org.jetbrains.annotations.ApiStatus;

/**
 * 字符串工具类（仅供内部使用）
 */
@ApiStatus.Internal
public class StringUtil {

    /**
     * 为字符串添加前后缀
     *
     * @param str 字符串
     * @param fix 前后缀
     * @return 添加前后缀后的字符串
     */
    public static String makeSurroundWith(String str, String fix) {
        if (str == null) {
            return null;
        }

        String result = addPrefixIfNot(str, fix);
        result = addSuffixIfNot(result, fix);
        return result;
    }

    /**
     * 为字符串添加单引号
     *
     * @param str 字符串
     * @return 添加单引号后的字符串
     */
    public static String addSingleQuotes(String str) {
        return makeSurroundWith(str, "'");
    }

    /**
     * 如果字符串不以指定前缀开头，则添加前缀
     *
     * @param str    字符串
     * @param prefix 前缀
     * @return 添加前缀后的字符串
     */
    private static String addPrefixIfNot(String str, String prefix) {
        if (str == null || prefix == null) {
            return str;
        }
        return str.startsWith(prefix) ? str : prefix + str;
    }

    /**
     * 如果字符串不以指定后缀结尾，则添加后缀
     *
     * @param str    字符串
     * @param suffix 后缀
     * @return 添加后缀后的字符串
     */
    private static String addSuffixIfNot(String str, String suffix) {
        if (str == null || suffix == null) {
            return str;
        }
        return str.endsWith(suffix) ? str : str + suffix;
    }
}
