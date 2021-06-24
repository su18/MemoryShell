package org.su18.memshell.scanner.asm;

import org.objectweb.asm.ClassWriter;

/**
 * Class 信息描述类
 *
 * @author su18
 */
public class SuClassDesc {

	/**
	 * ASM版本
	 */
	private final int api;

	/**
	 * JDK版本
	 */
	private final int version;

	/**
	 * 类访问级别
	 */
	private final int access;

	/**
	 * 类名
	 */
	private final String className;

	/**
	 * 签名
	 */
	private final String signature;

	/**
	 * 父类名
	 */
	private final String superClassName;

	/**
	 * 实现的所有的接口名
	 */
	private final String[] interfacesClass;

	/**
	 * 类加载器
	 */
	private final ClassLoader classLoader;

	/**
	 * 类字节码
	 */
	private final byte[] classfileBuffer;

	private final ClassWriter classVisitor;

	public SuClassDesc(final int api, final int version, final int access,
	                   final String className, final String signature,
	                   final String superClassName, final String[] interfacesClass,
	                   final ClassLoader classLoader, final byte[] classfileBuffer,
	                   final ClassWriter classVisitor) {

		this.api = api;
		this.version = version;
		this.access = access;
		this.className = className;
		this.signature = signature;
		this.superClassName = superClassName;
		this.interfacesClass = interfacesClass;
		this.classLoader = classLoader;
		this.classfileBuffer = classfileBuffer;
		this.classVisitor = classVisitor;
	}

	public int getApi() {
		return api;
	}

	public int getVersion() {
		return version;
	}

	public int getAccess() {
		return access;
	}

	public String getClassName() {
		return className;
	}

	public String getSignature() {
		return signature;
	}

	public String getSuperClassName() {
		return superClassName;
	}

	public String[] getInterfacesClass() {
		return interfacesClass;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public byte[] getClassfileBuffer() {
		return classfileBuffer;
	}

}