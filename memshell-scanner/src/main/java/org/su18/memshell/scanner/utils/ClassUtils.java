package org.su18.memshell.scanner.utils;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

import static org.su18.memshell.loader.commons.Constants.AGENT_NAME;

/**
 * @author su18
 */
public class ClassUtils {

	/**
	 * 转换成Java内部命名方式
	 *
	 * @param className 类名
	 * @return Java类格式的类名称
	 */
	public static String toJavaName(String className) {
		return className.replace("/", ".");
	}


	/**
	 * 获取用于ASM调用的类名称
	 *
	 * @param className 类名
	 * @return ASM格式的Java类名称
	 */
	public static String toAsmClassName(String className) {
		return className.replace(".", "/");
	}


	/**
	 * 获取一个类的所有父类和实现的接口
	 *
	 * @param clazz       类
	 * @param classLoader 类加载
	 * @return 父类集合
	 */
	public static Set<String> getSuperClassListByAsm(Class<?> clazz, ClassLoader classLoader) {
		Set<String> superClassList  = new LinkedHashSet<String>();
		String      objectClassName = Object.class.getName();

		// 先使用 class 获取接口
		getAllFather(clazz, superClassList);

		try {
			getSuperClassListByAsm(clazz.getName(), classLoader, superClassList);

			// 把Object的位置放到最后,方便父类检测
			for (Iterator<String> it = superClassList.iterator(); it.hasNext(); ) {
				String name = it.next();

				if (objectClassName.equals(name)) {
					it.remove();
				}
			}

			superClassList.add(objectClassName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return superClassList;
	}

	public static Set<String> getSuperClassListByAsm(String className, ClassLoader classLoader) {
		Set<String> superClassList  = new LinkedHashSet<String>();
		String      objectClassName = Object.class.getName();

		try {
			getSuperClassListByAsm(className, classLoader, superClassList);

			// 把Object的位置放到最后,方便父类检测
			for (Iterator<String> it = superClassList.iterator(); it.hasNext(); ) {
				String name = it.next();

				if (objectClassName.equals(name)) {
					it.remove();
				}
			}

			superClassList.add(objectClassName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return superClassList;
	}

	/**
	 * 获取一个类的所有父类和实现的接口
	 *
	 * @param className      类名
	 * @param loader         类加载器
	 * @param superClassList 父类集合
	 */
	public static void getSuperClassListByAsm(String className, ClassLoader loader, Set<String> superClassList) {
		if (className != null) {
			try {
				// 如果传入类加载器为空，默认使用SystemClassLoader
				if (loader == null) {
					loader = ClassLoader.getSystemClassLoader();
				}

				superClassList.add(className);
				byte[] classBytes = getClassBytes(className, loader);

				// 忽略无法找到类字节码的class
				if (classBytes != null) {
					ClassReader classReader = new ClassReader(classBytes);

					// 父类
					String superClass = classReader.getSuperName();
					// 父接口
					String[] interfaces = classReader.getInterfaces();

					List<String> ls = new ArrayList<String>();

					// 添加父类
					if (superClass != null) {
						ls.add(superClass);
					}

					// 添加父类的所有接口
					Collections.addAll(ls, interfaces);

					// 遍历所有父类和接口
					for (String clazz : ls) {
						getSuperClassListByAsm(toJavaName(clazz), loader, superClassList);
					}
				}
			} catch (Exception e) {
				System.err.println(AGENT_NAME + "获取" + className + "类的父类异常");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 查找类对象，获取类字节码
	 *
	 * @param className   类名
	 * @param classLoader 类加载器
	 * @return 类字节码数组
	 */
	public static byte[] getClassBytes(String className, ClassLoader classLoader) {
		InputStream in = null;

		try {
			if (className.startsWith("[")) {
				return null;
			}

			String classRes = toAsmClassName(className) + ".class";

			in = ClassLoader.getSystemResourceAsStream(classRes);

			if (classLoader != null && in == null) {
				in = classLoader.getResourceAsStream(classRes);
			}

			if (in != null) {
				return IOUtils.toByteArray(in);
			}
			return null;
		} catch (IOException e) {
			return null;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}


	/**
	 * 打印 Class 信息
	 *
	 * @param clazz Class 类
	 * @return 返回字符串
	 */
	public static String getClassInfo(Class<?> clazz) {

		return String.format("|%80s|\n", "||||| Class Details |||||") +
				String.format("|%-80s|\n", " Class Name:" + clazz.getName()) +
				String.format("|%-80s|\n", " Class Loader:" + clazz.getClassLoader().getClass().getName()) +
				String.format("|%-80s|\n", " Resource Url:" + clazz.getClassLoader().getResource(clazz.getName())) +
				String.format("|%-80s|\n", " Interfaces name:" +
						(clazz.getInterfaces().length > 0 ? clazz.getInterfaces()[0].getName() : "")) +
				String.format("|%-80s|\n", " Super Class Name:" + clazz.getSuperclass().getName()) +
				String.format("|----------%70s|\n", "----------");
	}

	/**
	 * 判断目标类是否使用 List 中的注解
	 *
	 * @param clazz       Class
	 * @param annotations 注解 List
	 * @return 返回布尔值
	 */
	public static Boolean isUseAnnotations(Class<?> clazz, List<String> annotations) {
		try {
			Annotation[] da = clazz.getDeclaredAnnotations();
			if (da.length > 0) {
				for (Annotation annotation : da) {
					for (String aa : annotations) {
						if (annotation.annotationType().getName().equals(aa)) {
							return true;
						}
					}
				}
			}
		} catch (Throwable ignored) {

		}
		return false;
	}

	/**
	 * 检查目标类是否存在对应的 Resource
	 *
	 * @param clazz Class
	 * @return 返回布尔值
	 */
	public static Boolean checkClassIsNotExists(Class<?> clazz) {

		String      className     = clazz.getName();
		String      classNamePath = className.replace(".", "/") + ".class";
		ClassLoader loader        = clazz.getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}

		URL isExists = loader.getResource(classNamePath);
		if (isExists == null) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;

	}

	public static void getAllFather(Class<?> clazz, Set<String> set) {
		Class<?>[] interfaces = clazz.getInterfaces();
		Class<?>   superClass = clazz.getSuperclass();

		if (superClass != null) {
			set.add(superClass.getName());
			getAllFather(superClass, set);
		}

		for (Class<?> anInterface : interfaces) {
			set.add(anInterface.getName());
			getAllFather(anInterface, set);
		}
	}
}

