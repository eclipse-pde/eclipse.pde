/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

/**
 * Interface for tasks.
 */
public interface ITask {

	/**
	 * Print the information for this task to the given script. Use the given
	 * tab index for indenting.
	 * 
	 * @param script the script to print to
	 */
	public void print(AntScript script);
}
