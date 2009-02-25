/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
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
