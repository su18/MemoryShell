package org.su18.memshell.scanner.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.su18.memshell.loader.commons.AgentCache;
import org.su18.memshell.scanner.BootStrap;
import org.su18.memshell.scanner.common.MemoryShellType;

import java.util.Map;

/**
 * 自定义 ClassVisitor
 *
 * @author su18
 */
public class SuClassVisitor extends ClassVisitor implements Opcodes {

	private final int api;

	private final ClassWriter classVisitor;

	private final String className;

	private final ClassLoader loader;

	private final byte[] classfileBuffer;

	private final AgentCache agentCache;

	private SuClassDesc raspClassDesc;


	public SuClassVisitor(final int api, final ClassWriter cw, final String className,
	                      final ClassLoader loader, final byte[] classfileBuffer,
	                      final AgentCache agentCache) {

		super(api, cw);

		this.api = api;
		this.classVisitor = cw;
		this.className = className;
		this.loader = loader;
		this.classfileBuffer = classfileBuffer;
		this.agentCache = agentCache;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		raspClassDesc = new SuClassDesc(
				api, version, access, className.replace("/", "."), signature, superName.replace("/", "."),
				interfaces, loader, classfileBuffer, classVisitor
		);

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(final int access, String methodName, final String methodDesc,
	                                 String signature, String[] exceptions) {

		MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);

		// 判断类名是否匹配
		for (Map.Entry<Class<?>, String> entry : BootStrap.shellClassMap.entrySet()) {
			if (entry.getKey().getName().equals(raspClassDesc.getClassName())) {
				String type = entry.getValue();

				// 从 MemoryShellType 中寻找对应的
				if (type != null && methodName.startsWith(MemoryShellType.valueOf(type).getMethod())) {
					final SuMethodDesc raspMethodDesc = new SuMethodDesc(
							raspClassDesc, access, signature, exceptions, methodName, methodDesc
					);

					agentCache.getModifiedClass().add(className);
					return new SuMethodVisitor(raspMethodDesc, mv);
				}
			}
		}

		return mv;
	}
}