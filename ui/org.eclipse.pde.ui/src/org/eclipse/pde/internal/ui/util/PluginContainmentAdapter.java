/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IContainmentAdapter;

public class PluginContainmentAdapter implements IContainmentAdapter {

	public PluginContainmentAdapter() {
		super();
	}

	@Override
	public boolean contains(Object containmentContext, Object element, int flags) {
		if (!(containmentContext instanceof PersistablePluginObject) || element == null)
			return false;

		IResource resource = ((PersistablePluginObject) containmentContext).getResource();
		return element.equals(resource);
	}

}
