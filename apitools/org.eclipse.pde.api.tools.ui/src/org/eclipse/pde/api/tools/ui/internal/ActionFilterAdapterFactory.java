/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ui.IActionFilter;


/**
 * API Tools UI adapter factory
 */
class ActionFilterAdapterFactory implements IAdapterFactory {
	/**
	 * @see IAdapterFactory#getAdapter(Object, Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object obj, Class<T> adapterType) {
		if (adapterType == IActionFilter.class) {
			if (obj instanceof IJavaElement) {
				return (T) new JavaElementActionFilter();
			}
		}
		return null;
	}

	/**
	 * @see IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IActionFilter.class };
	}
}