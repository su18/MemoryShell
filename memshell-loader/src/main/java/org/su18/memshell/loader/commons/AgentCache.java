package org.su18.memshell.loader.commons;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Agent 缓存类
 *
 * @author su18
 */
public class AgentCache {

	private Instrumentation instrumentation;

	private final Set<String> modifiedClass = Collections.synchronizedSet(new HashSet<String>());

	private final Set<String> reTransformClass = Collections.synchronizedSet(new HashSet<String>());

	private final Set<ClassFileTransformer> transformers = Collections.synchronizedSet(new HashSet<ClassFileTransformer>());

	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	public void setInstrumentation(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	public Set<String> getModifiedClass() {
		return modifiedClass;
	}

	public Set<ClassFileTransformer> getTransformers() {
		return transformers;
	}

	public Set<String> getReTransformClass() {
		return reTransformClass;
	}

	public void clear() {
		instrumentation = null;
		modifiedClass.clear();
		reTransformClass.clear();
		transformers.clear();
	}

}