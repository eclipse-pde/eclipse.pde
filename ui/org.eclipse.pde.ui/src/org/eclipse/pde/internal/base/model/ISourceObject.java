/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.base.model;

import org.eclipse.core.runtime.CoreException;

/**
 * This interface indicates that a model object is created by
 * parsing an editable source file and can be traced back
 * to a particular location in the file.
 */
public interface ISourceObject {
/**
 * Returns the line in the source file where the source
 * representation of this object starts, or -1 if not known.
 */
	public int getStartLine();
}
