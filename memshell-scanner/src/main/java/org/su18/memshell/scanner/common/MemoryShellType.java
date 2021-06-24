package org.su18.memshell.scanner.common;

/**
 * @author su18
 */
public enum MemoryShellType {
	/**
	 * 内存马类型
	 */
	SERVLET("javax.servlet.Servlet", "service"),
	FILTER("javax.servlet.Filter", "doFilter"),
	LISTENER("javax.servlet.ServletRequestListener", "request"),
	INTERCEPTOR("org.springframework.web.servlet.HandlerInterceptor", "preHandle"),
	CONTROLLER("org.springframework.stereotype.Controller", "null"),
	VALVE("org.apache.catalina.Valve", "invoke"),
	GRIZZLY("org.glassfish.grizzly.filterchain.Filter", "handle");


	/**
	 * hook 类型
	 */
	private final String hook;

	/**
	 * hook 方法
	 */
	private final String method;


	MemoryShellType(String hookClass, String hookMethod) {
		this.hook = hookClass;
		this.method = hookMethod;
	}

	public String getHook() {
		return hook;
	}

	public String getMethod() {
		return method;
	}
}
