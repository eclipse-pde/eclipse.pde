/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
