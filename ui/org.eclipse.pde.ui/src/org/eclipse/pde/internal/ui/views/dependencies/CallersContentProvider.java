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
import org.eclipse.osgi.service.resolver.BundleDescription;

public class CallersContentProvider extends DependenciesViewPageContentProvider {
	public CallersContentProvider(DependenciesView view) {
		super(view);
	}

	protected Collection findReferences(BundleDescription desc) {
		if (desc != null) {
			// don't return any callers for fragments (since no one can depend on a fragment
			if (desc.getHost() == null) {
				BundleDescription[] dependents = desc.getDependents();
				return Arrays.asList(dependents);
			}
		}
		return Collections.EMPTY_LIST;
	}

}
