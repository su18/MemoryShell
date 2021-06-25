package org.su18.memshell.loader;

import org.su18.memshell.loader.commons.AgentCache;
import org.su18.memshell.loader.utils.StringUtils;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.su18.memshell.loader.commons.Constants.*;

/**
 * suagent 入口类，支持 agent/attach 两种模式
 *
 * @author su18
 */
public class Agent {

	private static SuClassLoader suClassLoader;

	private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("^1\\.[0-5]");

	private static final AgentCache RASP_AGENT_CACHES = new AgentCache();

	/**
	 * 获取类加载器
	 *
	 * @return 返回 SuClassLoader
	 */
	public static synchronized SuClassLoader getSuClassLoader() {
		return suClassLoader;
	}

	/**
	 * 设置类加载器
	 *
	 * @param jarFile jar 文件路径
	 */
	private static synchronized void setSuClassLoader(URL jarFile) {
		if (suClassLoader == null) {
			suClassLoader = new SuClassLoader(jarFile, Agent.class.getClassLoader());
		}
	}

	/**
	 * 获取 memshell scanner jar 文件 File 对象
	 *
	 * @param loaderFile Agent文件
	 * @return 获取Agent URL地址
	 */
	private static File getScannerJarFileUrl(File loaderFile) {
		return new File(loaderFile.getParentFile(), AGENT_FILE_NAME);
	}

	/**
	 * 卸载 suagent
	 */
	private static void detachAgent() {
		synchronized (RASP_AGENT_CACHES) {
			Set<ClassFileTransformer> transformers    = RASP_AGENT_CACHES.getTransformers();
			Instrumentation           instrumentation = RASP_AGENT_CACHES.getInstrumentation();

			if (instrumentation != null) {
				Class<?>[]  loadedClass    = instrumentation.getAllLoadedClasses();
				Set<String> reTransformSet = new HashSet<String>();

				reTransformSet.addAll(RASP_AGENT_CACHES.getReTransformClass());
				reTransformSet.addAll(RASP_AGENT_CACHES.getModifiedClass());

				// 注销已注册的Transformer
				for (Iterator<ClassFileTransformer> iterator = transformers.iterator(); iterator.hasNext(); ) {
					ClassFileTransformer transformer = iterator.next();
					instrumentation.removeTransformer(transformer);
					iterator.remove();
					StringUtils.println("Removing Transformer: " + transformer.getClass() + " Success");
				}

				// 恢复所有已经被 suagent reTransform、modified的类
				for (Class<?> clazz : loadedClass) {
					for (Iterator<String> iterator = reTransformSet.iterator(); iterator.hasNext(); ) {
						String className = iterator.next();

						if (clazz.getName().equals(className) && instrumentation.isModifiableClass(clazz)) {
							try {
								instrumentation.retransformClasses(clazz);
								iterator.remove();

								StringUtils.println("ReTransform " + clazz);
							} catch (UnmodifiableClassException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// 清空缓存的Agent对象
				RASP_AGENT_CACHES.clear();
			}

			// 关闭RASP 类加载器
			if (suClassLoader != null && suClassLoader.closeClassLoader()) {
				StringUtils.println("Release SuAgent Resource Success");
				suClassLoader = null;
			}

			StringUtils.println("Detach Success");
		}
	}

	/**
	 * 加载RASP Agent
	 *
	 * @param arg  参数
	 * @param inst Instrumentation
	 */
	private static void loadAgent(final String arg, final Instrumentation inst) {
		String[] args = arg != null ? arg.split("\\s+") : new String[0];

		synchronized (RASP_AGENT_CACHES) {
			try {
				if (args.length > 0) {
					// 处理RASP Agent卸载事件
					if ("detach".equalsIgnoreCase(args[0])) {
						detachAgent();
						return;
					} else if ("attach".equalsIgnoreCase(args[0]) && RASP_AGENT_CACHES.getInstrumentation() != null) {
						// 处理重复attach问题
						StringUtils.println(AGENT_NAME + "已经存在，请勿重复安装!");
						return;
					}
				}

				File loaderFile   = getLoaderFile();
				File agentFile    = getScannerJarFileUrl(loaderFile);
				URL  agentFileUrl = agentFile.toURI().toURL();

				// 设置 suagent 类加载器
				setSuClassLoader(agentFileUrl);

				// 缓存inst对象
				RASP_AGENT_CACHES.setInstrumentation(inst);

				// 将Agent添加到BootstrapClassLoader
				inst.appendToBootstrapClassLoaderSearch(new JarFile(loaderFile));

				// 加载 scanner jar
				suClassLoader.loadAgent(agentFile, arg, inst, RASP_AGENT_CACHES);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * 获取 loader URL路径
	 *
	 * @return loader URL
	 */
	private static URL getLoaderFileUrl() {
		return Agent.class.getProtectionDomain().getCodeSource().getLocation();
	}

	/**
	 * 获取 loader 目录文件
	 *
	 * @return loader文件
	 */
	private static File getLoaderFile() {
		return new File(getLoaderFileUrl().getFile());
	}

	/**
	 * 附加Agent到JVM进程
	 *
	 * @param processId 进程 ID
	 * @param args      参数
	 * @param loader    VirtualMachine 代理类
	 */
	public static void attachJvm(String processId, String args, VirtualMachineProxy loader) {
		try {
			URL    loaderFileUrl = getLoaderFileUrl();
			Object vm            = loader.attach(processId);
			loader.loadAgent(vm, new File(loaderFileUrl.toURI()).getAbsolutePath(), args);
			loader.detach(vm);
		} catch (Exception e) {
			StringUtils.println("Attach To JVM Exception: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * 打印使用用例
	 *
	 * @param loader JVM虚拟机
	 */
	private static void printUsage(VirtualMachineProxy loader) {
		StringUtils.println(AGENT_NAME + " (Java Agent)");
		StringUtils.println("Usage: java -jar " + AGENT_LOADER_FILE_NAME + " [Options]");
		StringUtils.println("  1) detach [Java PID]");
		StringUtils.println("  2) attach [Java PID]");
		StringUtils.println("\r\n");
		StringUtils.println("EXAMPLES :");
		StringUtils.println("  java -jar " + AGENT_LOADER_FILE_NAME + " attach 10001");
		StringUtils.println("  java -jar " + AGENT_LOADER_FILE_NAME + " detach 10001");
		StringUtils.println("\r\n");
		StringUtils.println("JVM PID List:");

		try {
			Map<String, String> processMap = loader.listJvmPid();

			for (String processId : processMap.keySet()) {
				String name = processMap.get(processId);
				StringUtils.println("PID:" + processId + "\tProcessName:" + ("".equals(name) ? "NONE" : name));
			}
		} catch (Exception e) {
			StringUtils.println("Load JVM PID Exception:" + e);
			e.printStackTrace();
		}
	}

	public static void premain(String args, Instrumentation inst) {
		loadAgent(args, inst);
	}

	public static void agentmain(String args, Instrumentation inst) {
		loadAgent(args, inst);
	}

	/**
	 * 主程序入口
	 *
	 * @param args 参数
	 */
	public static void main(String[] args) {
		VirtualMachineProxy loader = new VirtualMachineProxy();

		if (args.length == 0) {
			printUsage(loader);
			return;
		}

		// 判断当前 JDK 版本
		String javaVersion = System.getProperty("java.version");
		if (JAVA_VERSION_PATTERN.matcher(javaVersion).find()) {
			System.err.println("JDK Version: " + javaVersion + ". JDK Version Can Not Less Than 1.6!");
		}

		if ("attach".equalsIgnoreCase(args[0]) || "detach".equalsIgnoreCase(args[0])) {
			attachJvm(args[1].trim(), args[0], loader);
		} else {
			printUsage(loader);
		}
	}

}
