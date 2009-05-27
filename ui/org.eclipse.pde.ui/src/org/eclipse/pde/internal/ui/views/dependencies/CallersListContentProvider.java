/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.*;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class CallersListContentProvider extends CallersContentProvider implements IStructuredContentProvider {

	public CallersListContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		// input IPluginModelBase
		BundleDescription desc = null;
		if (inputElement instanceof IPluginModelBase) {
			desc = ((IPluginModelBase) inputElement).getBundleDescription();
		} else if (inputElement instanceof BundleDescription) {
			desc = (BundleDescription) inputElement;
		}
		if (desc != null) {
			Set callers = new HashSet();
			Set candidates = new HashSet();
			candidates.addAll(findReferences(desc));
			while (!candidates.isEmpty()) {
				Set newCandidates = new HashSet();
				for (Iterator it = candidates.iterator(); it.hasNext();) {
					Object o = it.next();
					it.remove();
					BundleDescription caller = (BundleDescription) o;
					if (!callers.contains(caller)) {
						callers.add(caller);
						newCandidates.addAll(findReferences(caller));
					}
				}
				candidates = newCandidates;

			}

			return callers.toArray();
		}
		return new Object[0];
	}
}
