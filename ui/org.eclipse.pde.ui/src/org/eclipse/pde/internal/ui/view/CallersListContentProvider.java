/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class CallersListContentProvider extends CallersContentProvider
		implements IStructuredContentProvider {

	public CallersListContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		// input IPluginModelBase
		if (inputElement instanceof IPluginModelBase) {
			IPluginBase pluginBase = ((IPluginModelBase) inputElement)
					.getPluginBase();

			Set callers = new HashSet();
			Set candidates = new HashSet();
			candidates.addAll(findReferences(pluginBase.getId()));
			while (!candidates.isEmpty()) {
				Set newCandidates = new HashSet();
				for (Iterator it = candidates.iterator(); it.hasNext();) {
					Object o = it.next();
					it.remove();
					IPluginBase caller = (IPluginBase) o;
					if (!callers.contains(caller)) {
						callers.add(caller);
						newCandidates.addAll(findReferences(caller.getId()));
					}
				}
				candidates = newCandidates;

			}

			return callers.toArray();
		}
		return new Object[0];
	}
}
