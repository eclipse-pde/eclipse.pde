/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

/**
 * ICompCSDependency
 *
 */
public interface ICompCSDependency extends ICompCSObject {

	/**
	 * Attribute: task
	 */
	public void setFieldTask(String task);

	/**
	 * Attribute: task
	 *
	 */
	public String getFieldTask();

}
