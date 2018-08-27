/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.BundleDescription;

public class CallersContentProvider extends DependenciesViewPageContentProvider {
	public CallersContentProvider(DependenciesView view) {
		super(view);
	}

	protected Collection<BundleDescription> findReferences(BundleDescription desc) {
		if (desc != null) {
			// don't return any callers for fragments (since no one can depend on a fragment
			if (desc.getHost() == null) {
				BundleDescription[] dependents = desc.getDependents();
				return Arrays.asList(dependents);
			}
			// for fragment, need to get the host bundle
			return Arrays.asList(desc.getHost().getHosts());
		}
		return Collections.emptyList();
	}

}
