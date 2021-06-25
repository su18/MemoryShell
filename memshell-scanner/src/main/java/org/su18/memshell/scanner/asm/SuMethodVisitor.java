package org.su18.memshell.scanner.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.su18.memshell.loader.utils.StringUtils;

import static org.su18.memshell.scanner.common.MemoryShellType.*;

/**
 * 自定义 MethodVisitor
 *
 * @author su18
 */
public class SuMethodVisitor extends AdviceAdapter {

	/**
	 * 是否为静态方法
	 */

	private final SuMethodDesc suMethodDesc;


	protected SuMethodVisitor(SuMethodDesc suMethodDesc, MethodVisitor mv) {
		// 执行 super 方法
		super(
				suMethodDesc.getSuClassDesc().getApi(), mv, suMethodDesc.getMethodAccess(),
				suMethodDesc.getMethodName(), suMethodDesc.getMethodArgsDesc()
		);

		this.suMethodDesc = suMethodDesc;
	}

	/**
	 *
	 */
	@Override
	public void onMethodEnter() {

		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		Label label3 = new Label();
		visitTryCatchBlock(label0, label1, label2, Throwable.class.getName().replace(".", "/"));
		mark(label0);

		// 插入 hook 逻辑
		insertHook();

		mark(label1);
		goTo(label3);
		mark(label2);
		int throwableIndex = newLocal(Type.getType(Throwable.class));
		storeLocal(throwableIndex);
		mark(label3);
	}

	public void insertHook() {
		String methodName = suMethodDesc.getMethodName();
		String message = "Overwriting Byte Code To Class: [" + suMethodDesc.getSuClassDesc().getClassName() +
				"] Method [" + suMethodDesc.getMethodName() + "]";

		if (methodName.equals(SERVLET.getMethod()) || methodName.startsWith(LISTENER.getMethod()) ||
				(methodName.startsWith("do") && !"doFilter".equals(methodName))) {
			StringUtils.println(message);
			hookReturn();
		} else if (methodName.equals(FILTER.getMethod())) {
			StringUtils.println(message);
			hookFilter();
		} else if (methodName.equals(INTERCEPTOR.getMethod())) {
			StringUtils.println(message);
			hookReturnTrue();
		} else if (methodName.equals(VALVE.getMethod())) {
			StringUtils.println(message);
			// 暂时有问题
//			hookValve();
		} else if (methodName.startsWith(GRIZZLY.getMethod())) {
			StringUtils.println(message);
			hookGrizzly();
		}
	}

	/**
	 * 什么也不做直接 return
	 * servlet 和 listener 的内存马可以这么做
	 * return;
	 */
	public void hookReturn() {
		mv.visitInsn(RETURN);
	}


	/**
	 * 直接按照 Filter 的调用传入下一条 FilterChain 然后 return
	 * Filter 内存马可以这么做
	 * $3.doFilter($1, $2);return;
	 */
	public void hookFilter() {
		mv.visitVarInsn(ALOAD, 3);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/FilterChain", "doFilter", "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V", true);
		mv.visitInsn(RETURN);
	}


	/**
	 * 插入字节码 return true
	 * spring HandlerInterceptor preHandle 可以这么做
	 * return true;
	 */
	public void hookReturnTrue() {
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IRETURN);
	}

	/**
	 * 传入下一个 Valve 并 return
	 * Tomcat Valve 内存马可以这么做
	 * this.getNext().invoke($1,$2);return;
	 */
	public void hookValve() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/Valve", "getNext", "()Lorg/apache/catalina/Valve;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/catalina/Valve", "invoke", "(Lorg/apache/catalina/connector/Request;Lorg/apache/catalina/connector/Response;)V", true);
		mv.visitInsn(RETURN);
	}

	/**
	 * 调用 super 并返回
	 * Grizzly Filter 内存马可以这么做
	 * return super.handleRead($1);
	 */
	public void hookGrizzly() {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, "org/glassfish/grizzly/filterchain/BaseFilter", "handleRead", "(Lorg/glassfish/grizzly/filterchain/FilterChainContext;)Lorg/glassfish/grizzly/filterchain/NextAction;", false);
		mv.visitInsn(ARETURN);
	}
}