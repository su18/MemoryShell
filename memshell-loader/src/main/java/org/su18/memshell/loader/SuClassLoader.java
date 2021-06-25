package org.su18.memshell.loader;

import org.su18.memshell.loader.commons.AgentCache;
import org.su18.memshell.loader.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

import static org.su18.memshell.loader.commons.Constants.JAVA_INTERNAL_PACKAGES;

/**
 * 使用自定义 ClassLoader 加载自己的 jar
 *
 * @author su18
 */
public class SuClassLoader extends URLClassLoader {

	private String args;

	private Instrumentation instrumentation;

	private ClassFileTransformer raspClassFileTransformer;

	private File agentFile;


	public SuClassLoader(final URL url, final ClassLoader classLoader) {
		super(new URL[]{url}, classLoader);
	}


	public String getArgs() {
		return args;
	}

	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	public ClassFileTransformer getRaspClassFileTransformer() {
		return raspClassFileTransformer;
	}

	public void setRaspClassFileTransformer(ClassFileTransformer raspClassFileTransformer) {
		this.raspClassFileTransformer = raspClassFileTransformer;
	}

	/**
	 * 注册一个Jar文件到当前的类加载器
	 *
	 * @param url jar文件的url地址
	 */
	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	public File getAgentFile() {
		return agentFile;
	}

	/**
	 * 修改获取资源文件优先级，优先从自身的jar中找
	 *
	 * @param name 资源名称
	 * @return 资源文件URL地址
	 */
	@Override
	public URL getResource(String name) {
		URL url = findResource(name);

		if (url != null) {
			return url;
		}

		return super.getResource(name);
	}

	/**
	 * 修改获取资源文件优先级，优先从自身的jar中找
	 *
	 * @param name 资源名称
	 * @return 资源文件枚举URL地址
	 * @throws IOException 文件读取异常
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> urls = findResources(name);

		if (urls != null) {
			return urls;
		}

		return super.getResources(name);
	}

	/**
	 * 加载类资源对象
	 *
	 * @param name    类名
	 * @param resolve 是否需要resolve
	 * @return 返回类对象
	 * @throws ClassNotFoundException 类未找到异常
	 */
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		final Class<?> loadedClass = findLoadedClass(name);
		if (loadedClass != null) {
			return loadedClass;
		}

		// 忽略JDK自带的包
		if (name.matches(JAVA_INTERNAL_PACKAGES)) {
			return super.loadClass(name, resolve);
		}

		try {
			Class<?> clazz = findClass(name);

			if (resolve) {
				resolveClass(clazz);
			}

			return clazz;
		} catch (Exception e) {
			return super.loadClass(name, resolve);
		}
	}

	/**
	 * 加载RASP Agent
	 *
	 * @param agentFile Agent路径
	 * @param args      Agent参数
	 * @param inst      Instrumentation
	 * @param cache     Agent缓存
	 * @throws ClassNotFoundException    类未找到异常
	 * @throws NoSuchMethodException     方法未找到异常
	 * @throws InvocationTargetException 反射调用异常
	 * @throws IllegalAccessException    反射不正确的访问异常
	 */
	public void loadAgent(File agentFile, String args, Instrumentation inst, AgentCache cache) throws Exception {
		this.args = args;
		this.instrumentation = inst;
		this.agentFile = agentFile;

		// 添加Agent处理JAR到类加载器
		this.addURL(agentFile.toURL());

		// 反射调用 scanner 的 BootStrap 完成 Agent 启动
		Class<?> bootStrapClass = this.loadClass("org.su18.memshell.scanner.BootStrap");
		bootStrapClass.getMethod(
				"bootStrap", Instrumentation.class, SuClassLoader.class, AgentCache.class
		).invoke(args, inst, this, cache);
	}

	/**
	 * 关闭 RASP 类加载器,释放jar文件连接
	 */
	public boolean closeClassLoader() {
		try {
			// 如果URLClassLoader类有close方法则直接调用
			Class<?> clazz   = URLClassLoader.class;
			Method[] methods = clazz.getMethods();

			for (Method method : methods) {
				if ("close".equals(method.getName())) {
					method.invoke(this);

					return true;
				}
			}

			// 如果不能直接通过close方法关闭那么就需要反向查找所有已经打开了的jar文件并关闭了
			Field ucpField = clazz.getDeclaredField("ucp");
			ucpField.setAccessible(true);
			Object ucp = ucpField.get(this);

			Field loadersField = ucp.getClass().getDeclaredField("loaders");
			loadersField.setAccessible(true);
			List<?> loaders = (List<?>) loadersField.get(ucp);

			for (Object loader : loaders) {
				Class<?> jarLoaderClass = loader.getClass();
				Method   method         = jarLoaderClass.getDeclaredMethod("getJarFile");
				method.setAccessible(true);

				// 释放jar文件连接
				JarFile jarFile = (JarFile) method.invoke(loader);
				jarFile.close();

				StringUtils.println("Closed Jar: [" + jarFile.getName() + "]");
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

}
