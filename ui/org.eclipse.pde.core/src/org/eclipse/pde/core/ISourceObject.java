/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.core;

/**
 * This interface indicates that a model object is created by
 * parsing an editable source file and can be traced back
 * to a particular location in the file.
 */
public interface ISourceObject {
	/**
	 * Returns the line in the source file where the source
	 * representation of this object starts, or -1 if not known.
	 * @return the first line in the source file 
	 */
	public int getStartLine();
}