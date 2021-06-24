package org.su18.memshell.loader.commons;

/**
 * 常量配置类
 *
 * @author su18
 */
public class Constants {

	/**
	 * Java内部包名过滤正则
	 */
	public static final String JAVA_INTERNAL_PACKAGES = "^(java|javax|jakarta|(com\\.)?sun)\\..*";

	public static final String AGENT_NAME = "suagent";

	public static final String ENCODING = "UTF-8";

	public static final String AGENT_FILE_NAME = AGENT_NAME + "-scanner.jar";

	/**
	 * 定义Agent loader文件名称
	 */
	public static final String AGENT_LOADER_FILE_NAME = AGENT_NAME + "-loader.jar";


	public static final String BANNER_FILE_NAME = "banner.txt";


}
