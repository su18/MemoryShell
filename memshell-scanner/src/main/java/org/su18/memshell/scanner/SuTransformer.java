package org.su18.memshell.scanner;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.su18.memshell.loader.commons.AgentCache;
import org.su18.memshell.scanner.asm.SuClassVisitor;
import org.su18.memshell.scanner.asm.SuClassWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ASM9;

/**
 * 自定义 ClassFileTransformer ，处理指定的 class
 *
 * @author su18
 */
public class SuTransformer implements ClassFileTransformer {

	private final AgentCache agentCache;

	SuTransformer(AgentCache agentCache) {
		this.agentCache = agentCache;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> clazz,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		className = className.replace("/", ".");

		// 创建ClassReader对象，读取字节码
		ClassReader classReader = new ClassReader(classfileBuffer);

		ClassWriter cw = new SuClassWriter(classReader, COMPUTE_FRAMES, loader);

		try {
			SuClassVisitor classVisitor = new SuClassVisitor(
					ASM9, cw, className, loader, classfileBuffer, agentCache
			);

			classReader.accept(classVisitor, EXPAND_FRAMES);

			classfileBuffer = cw.toByteArray();
			// 查看被改的类
			dumpClass(className, classfileBuffer);
		} catch (Throwable t) {
			t.printStackTrace();
		}


		return classfileBuffer;
	}

	/**
	 * dump 出类字节码进行查看
	 *
	 * @param className       类名
	 * @param classfileBuffer 类字节码
	 * @throws IOException 抛出异常
	 */
	private static void dumpClass(String className, byte[] classfileBuffer) throws IOException {
		String regexp = "\\b(TestFilter|TestServlet|TestValve|TestListener)$";

		if (Pattern.compile(regexp).matcher(className).find()) {
			className = className.substring(className.lastIndexOf(".") + 1);
			FileOutputStream fos = new FileOutputStream("/Users/phoebe/IdeaProjects/MemoryShell/dump/" + className + ".class");
			fos.write(classfileBuffer);
			fos.close();
		}
	}
}
