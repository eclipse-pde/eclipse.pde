package org.eclipse.pde.internal.editor.schema;

public interface ICloneablePropertySource {
	public Object doClone();
	boolean isCloneable();
}
