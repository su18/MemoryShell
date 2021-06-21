package org.su18.memshell.test.apusic;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * @author su18
 */
public class NewFilter implements Filter {

	/**
	 * 初始化 filter
	 *
	 * @param filterConfig FilterConfig
	 */
	@Override
	public void init(FilterConfig filterConfig) {
	}

	/**
	 * doFilter 方法处理过滤器逻辑
	 *
	 * @param servletRequest  ServletRequest
	 * @param servletResponse ServletResponse
	 * @param filterChain     FilterChain
	 * @throws IOException      抛出异常
	 * @throws ServletException 抛出异常
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		// 给下一个过滤器
		filterChain.doFilter(new FilterRequest((HttpServletRequest) servletRequest), servletResponse);
	}

	/**
	 * 销毁时执行的方法
	 */
	@Override
	public void destroy() {
	}

	/**
	 * 自定义 FilterRequest 重写 getParameter 方法处理 id 值
	 */
	class FilterRequest extends HttpServletRequestWrapper {

		public FilterRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getParameter(String name) {
			if ("id".equals(name)) {
				String originalId = super.getParameter(name);

				if (originalId != null && !originalId.isEmpty()) {
					int idNum = (Integer.parseInt(originalId) + 1);
					return Integer.toString(idNum);
				}
			}
			return super.getParameter(name);
		}
	}
}