package org.su18.memshell.scanner;

import org.apache.commons.io.FileUtils;
import org.su18.memshell.loader.SuClassLoader;
import org.su18.memshell.loader.commons.AgentCache;
import org.su18.memshell.loader.utils.StringUtils;
import org.su18.memshell.scanner.common.MemoryShellType;
import org.su18.memshell.scanner.utils.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.su18.memshell.loader.commons.Constants.AGENT_NAME;
import static org.su18.memshell.loader.commons.Constants.BANNER_FILE_NAME;
import static org.su18.memshell.scanner.common.MemoryShellType.CONTROLLER;
import static org.su18.memshell.scanner.common.MemoryShellType.values;

/**
 * 通过反射调用的 BootStrap 入口类
 *
 * @author su18
 */
public class BootStrap {

	public static ClassLoader classLoader;

	public static Map<Class<?>, String> keyClassMap = new ConcurrentHashMap<Class<?>, String>();

	public static List<String> riskAnnotations = new ArrayList<String>();

	public static Map<Class<?>, String> shellClassMap = new ConcurrentHashMap<Class<?>, String>();


	static {
		riskAnnotations.add("org.springframework.stereotype.Controller");
		riskAnnotations.add("org.springframework.web.bind.annotation.RestController");
		riskAnnotations.add("org.springframework.web.bind.annotation.RequestMapping");
		riskAnnotations.add("org.springframework.web.bind.annotation.GetMapping");
		riskAnnotations.add("org.springframework.web.bind.annotation.PostMapping");
		riskAnnotations.add("org.springframework.web.bind.annotation.PatchMapping");
		riskAnnotations.add("org.springframework.web.bind.annotation.PutMapping");
		riskAnnotations.add("org.springframework.web.bind.annotation.Mapping");
	}


	/**
	 * 启动RASP Agent
	 *
	 * @param inst   Agent inst
	 * @param loader RASP 类加载器
	 * @param cache  RASP Agent缓存对象
	 */
	public static void bootStrap(Instrumentation inst, SuClassLoader loader, AgentCache cache) {

		// 打印 logo
		printLogo();

		// 储存 classloader
		classLoader = loader;

		// 创建自定义 ClassFileTransformer 并缓存
		ClassFileTransformer raspTransformer = new SuTransformer(cache);
		cache.getTransformers().add(raspTransformer);

		// 添加 transformer 到 JVM
		inst.addTransformer(raspTransformer, true);

		// 获取所有已加载的类
		Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();

		StringUtils.println("Investigating All Loaded Classed,total amount: " + allLoadedClasses.length);

		for (Class<?> clazz : allLoadedClasses) {

			// 不处理抽象类和接口
			if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {

				// 获取包名是黑名单的类（并不是一个体面的方式，但是挺暴力的）
				if (clazz.getName().startsWith("net.rebeyond.") && clazz.getName().startsWith("com.metasploit.")) {
					StringUtils.println("High-Risk Package Name In System:" + clazz.getName());
					StringUtils.print(ClassUtils.getClassInfo(clazz));
					continue;
				}

				// 提取关键类
				extractKeyClass(clazz);
			}
		}

		StringUtils.println("Extract Key Class Finished,Checking Memory Shell...");

		for (Map.Entry<Class<?>, String> entry : keyClassMap.entrySet()) {

			// 根据该 class 在其 ClassLoader 中是否存有 Resource 判断是否在磁盘上存在其文件
			// 若没有，则可能为内存马
			if (ClassUtils.checkClassIsNotExists(entry.getKey())) {
				StringUtils.println("Find " + entry.getValue() + "-TYPED Memory Shell [ Resource Missing ]");
				StringUtils.print(ClassUtils.getClassInfo(entry.getKey()));
				shellClassMap.put(entry.getKey(), entry.getValue());
			}
		}


		StringUtils.println("Checking Memory Shell Finished,Trying To Hook Shell Class..");

		// 将 shell class 加入 HOOK 进行处理
		for (Map.Entry<Class<?>, String> shellEntry : shellClassMap.entrySet()) {
			try {
				if (inst.isModifiableClass(shellEntry.getKey())) {
					inst.retransformClasses(shellEntry.getKey());
					cache.getReTransformClass().add(shellEntry.getKey().getName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * 提取关键类放在 keyClassMap 中
	 *
	 * @param clazz Class
	 */
	private static void extractKeyClass(Class<?> clazz) {

		// 忽略动态代理类
		if (clazz.getName().startsWith("com.sun.proxy")) {
			return;
		}

		// 获取 clazz 的全部父类和接口
		Set<String> set = ClassUtils.getSuperClassListByAsm(clazz, clazz.getClassLoader());

		// 获取带有 controller 和 mapping 注解的类（Spring Controller，确实没有太好的办法）（适配框架）
		if (ClassUtils.isUseAnnotations(clazz, riskAnnotations)) {
			keyClassMap.put(clazz, CONTROLLER.name());
			return;
		}

		for (MemoryShellType value : values()) {
			if (set.contains(value.getHook())) {
				keyClassMap.put(clazz, value.name());
				return;
			}
		}
	}

	/**
	 * 打印 suagent logo
	 */
	private static void printLogo() {
		String banner = getBanner();
		StringUtils.print("\n" + banner + "[ " + AGENT_NAME + " v1.0.0 ] by su18\n");
	}

	/**
	 * 获取Agent Banner信息
	 *
	 * @return 获取Banner
	 */
	public static String getBanner() {
		try {
			return FileUtils.readFileToString(
					new File(
							new File(
									BootStrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile(),
							BANNER_FILE_NAME));
		} catch (IOException e) {
			return "[ This is Banner ]";
		}


	}

}
