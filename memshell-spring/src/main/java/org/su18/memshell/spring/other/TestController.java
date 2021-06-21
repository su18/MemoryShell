package org.su18.memshell.spring.other;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 动态添加的 controller
 *
 * @author su18
 */
@Controller
@RequestMapping(value = "/su18")
public class TestController {

	@GetMapping
	public void index(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.getWriter().println("su18 is here~");
	}


}