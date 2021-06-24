package org.su18.memshell.scanner.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.su18.memshell.scanner.utils.ClassUtils;

import java.util.Set;

/**
 * @author su18
 */
public class SuClassWriter extends ClassWriter {

	private static final String OBJECT_CLASS = Object.class.getName().replace(".", "/");

	private final ClassLoader classLoader;

	public SuClassWriter(ClassReader classReader, int flags, ClassLoader classLoader) {
		super(classReader, flags);
		this.classLoader = classLoader;
	}

	/**
	 * 重写计算两个类的父类逻辑
	 *
	 * @param type1 类1
	 * @param type2 类2
	 * @return 父类名
	 */
	@Override
	protected String getCommonSuperClass(final String type1, final String type2) {
		String      className1 = ClassUtils.toJavaName(type1);
		String      className2 = ClassUtils.toJavaName(type2);
		ClassLoader loader     = classLoader;

		// 如果类加载器为空,可能是AppClassLoader类加载器加载的
		if (classLoader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}

		// 获取当前类的所有父类类名或知实现了的所有接口类类名
		Set<String> superClassList1 = ClassUtils.getSuperClassListByAsm(className1, loader);

		// 求A∩B,如果A∩B不为空,说明A是B的父类
		if (superClassList1.contains(className2)) {
			return type2;
		}

		Set<String> superClassList2 = ClassUtils.getSuperClassListByAsm(className2, loader);

		// 求B∩A,如果B∩A不为空,说明B是A的父类
		if (superClassList2.contains(className1)) {
			return type1;
		}

		// 取出A类的所有父类和B类的所有父类的交集类名称(排除Object类)
		for (String claName : superClassList1) {
			if (superClassList2.contains(claName)) {
				return ClassUtils.toAsmClassName(claName);
			}
		}

		return OBJECT_CLASS;
	}

}