package org.su18.memshell.loader.utils;

import java.io.UnsupportedEncodingException;

import static org.su18.memshell.loader.commons.Constants.AGENT_NAME;
import static org.su18.memshell.loader.commons.Constants.ENCODING;

/**
 * 字符串编码处理工具类
 *
 * @author su18
 */
public class StringUtils {


	private static final String DEFAULT_ENCODING = System.getProperty("file.encoding");

	public static String charset(String str) {
		try {
			if (!ENCODING.equals(DEFAULT_ENCODING)) {
				return new String(str.getBytes(), DEFAULT_ENCODING);
			}

			return str;
		} catch (UnsupportedEncodingException e) {
			// ignore
		}

		return str;
	}

	public static void println(String str) {
		System.out.println("[ " + AGENT_NAME + " ] " + charset(str));
	}

	public static void print(String str) {
		System.out.println(charset(str));
	}

}