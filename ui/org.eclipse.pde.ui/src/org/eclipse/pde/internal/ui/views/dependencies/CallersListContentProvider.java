/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
	@Override
	public Object[] getElements(Object inputElement) {
		// input IPluginModelBase
		BundleDescription desc = null;
		if (inputElement instanceof IPluginModelBase) {
			desc = ((IPluginModelBase) inputElement).getBundleDescription();
		} else if (inputElement instanceof BundleDescription) {
			desc = (BundleDescription) inputElement;
		}
		if (desc != null) {
			Set<BundleDescription> callers = new HashSet<>();
			Set<BundleDescription> candidates = new HashSet<>();
			candidates.addAll(findReferences(desc));
			while (!candidates.isEmpty()) {
				Set<BundleDescription> newCandidates = new HashSet<>();
				for (Iterator<BundleDescription> it = candidates.iterator(); it.hasNext();) {
					BundleDescription o = it.next();
					it.remove();
					if (!callers.contains(o)) {
						callers.add(o);
						newCandidates.addAll(findReferences(o));
					}
				}
				candidates = newCandidates;

			}

			return callers.toArray();
		}
		return new Object[0];
	}
}
