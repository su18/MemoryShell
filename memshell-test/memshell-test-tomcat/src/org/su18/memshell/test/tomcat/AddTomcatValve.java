package org.su18.memshell.test.tomcat;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.valves.ValveBase;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * 访问这个 Servlet 将会动态添加自定义 Valve
 * 测试版本 Tomcat 8.5.31
 *
 * @author su18
 */
public class AddTomcatValve extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// 从 request 中获取 servletContext
			ServletContext servletContext = req.getServletContext();

			// 如果已有此 servletName 的 Servlet，则不再重复添加
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

			// 添加自定义 Valve
			o.addValve(new TestValve());

			resp.getWriter().println("tomcat valve added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static class TestValve extends ValveBase {

		@Override
		public void invoke(Request request, Response response) throws IOException, ServletException {
			response.getWriter().println("I come here first!");
			// 不要忘了继续调用下一个 valve
			this.getNext().invoke(request, response);
		}
	}


}
