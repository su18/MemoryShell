package org.su18.memshell.agent;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

/**
 * ClassFileTransformer 实现类
 *
 * @author su18
 */
public class TestTransformer implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		try {
			className = className.replace("/", ".");

			// 忽略无用类
			if (!className.startsWith("org.su18")) {
				System.out.println(className);

				ClassPool      cp        = ClassPool.getDefault();
				ClassClassPath classPath = new ClassClassPath(classBeingRedefined);
				cp.insertClassPath(classPath);
				CtClass  cc = cp.get(className);
				CtMethod m  = cc.getDeclaredMethod("getQueryString");

				try {
					m.addLocalVariable("elapsedTime", CtClass.longType);
					m.insertBefore(insertSource());
					byte[] byteCode = cc.toBytecode();
					cc.detach();
					System.out.println("retransform: " + className);

					// dump 出经过 retransform 的 class
					dumpClass(className, byteCode);
					return byteCode;
				} catch (Exception ignored) {

				}

			}


		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String insertSource() {
		return "return \"the world is full of kings and queens who blind your eyes and steal your dreams\";";

	}

	private static void dumpClass(String className, byte[] classfileBuffer) throws IOException {
		String regexp = "\\b(ApplicationFilterChain|ResponseFacade|RequestFacade)$";

		if (Pattern.compile(regexp).matcher(className).find()) {
			className = className.substring(className.lastIndexOf(".") + 1);

			FileOutputStream fos = new FileOutputStream("/Users/phoebe/IdeaProjects/MemoryShell/" +
					"memshell-inject/target/" + className + ".class");
			fos.write(classfileBuffer);
			fos.close();
		}
	}


}
