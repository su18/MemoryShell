package org.su18.memshell.scanner.asm;

/**
 * 自定义方法描述类
 *
 * @author su18
 */
public class SuMethodDesc {

	private final SuClassDesc suClassDesc;

	private final int methodAccess;

	private final String methodSignature;

	private final String[] exceptions;

	private final String methodName;

	private final String methodArgsDesc;

	public SuMethodDesc(SuClassDesc suClassDesc, int methodAccess, String methodSignature,
	                    String[] exceptions, String methodName, String methodArgsDesc) {

		this.suClassDesc = suClassDesc;
		this.methodAccess = methodAccess;
		this.methodSignature = methodSignature;
		this.exceptions = exceptions;
		this.methodName = methodName;
		this.methodArgsDesc = methodArgsDesc;
	}

	public SuClassDesc getSuClassDesc() {
		return suClassDesc;
	}

	public int getMethodAccess() {
		return methodAccess;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public String[] getExceptions() {
		return exceptions;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodArgsDesc() {
		return methodArgsDesc;
	}

}