package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public interface ICloneablePropertySource {
	public Object doClone();
	boolean isCloneable();
}
