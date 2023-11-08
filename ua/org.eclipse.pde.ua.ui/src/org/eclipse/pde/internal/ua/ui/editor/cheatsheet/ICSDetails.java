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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.swt.widgets.Composite;

/**
 * ICSDetails
 */
public interface ICSDetails {

	/**
	 * @param parent
	 */
	public void createDetails(Composite parent);

	/**
	 *
	 */
	public void updateFields();

	/**
	 *
	 */
	public void hookListeners();

}
