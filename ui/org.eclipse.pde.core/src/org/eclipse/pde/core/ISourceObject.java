/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;
/**
 * This interface indicates that a model object is created by parsing an
 * editable source file and can be traced back to a particular location in the
 * file.
 * 
 * @since 2.0
 */
public interface ISourceObject {
	/**
	 * Returns the line in the source file where the source representation of
	 * this object starts, or -1 if not known.
	 * 
	 * @return the first line in the source file
	 */
	public int getStartLine();
	/**
	 * Returns the line in the source file where the source representation of
	 * this object stops, or -1 if not known.
	 * 
	 * @return the last line in the source file
	 */
	public int getStopLine();
}