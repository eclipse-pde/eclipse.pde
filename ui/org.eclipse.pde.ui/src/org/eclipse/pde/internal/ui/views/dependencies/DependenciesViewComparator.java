/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class DependenciesViewComparator extends ViewerComparator {

	private static DependenciesViewComparator fComparator = null;

	private DependenciesViewComparator() {
		super();
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return getId(e1).compareTo(getId(e2));
	}

	private String getId(Object obj) {
		BundleDescription desc = null;
		if (obj instanceof ImportPackageSpecification) {
			return ((ImportPackageSpecification) obj).getName();
		} else if (obj instanceof BundleSpecification) {
			desc = (BundleDescription) ((BundleSpecification) obj).getSupplier();
		} else if (obj instanceof BundleDescription)
			desc = (BundleDescription) obj;
		if (desc != null)
			return PDEPlugin.getDefault().getLabelProvider().getObjectText(desc);
		return ""; //$NON-NLS-1$
	}

	public static DependenciesViewComparator getViewerComparator() {
		if (fComparator == null)
			fComparator = new DependenciesViewComparator();
		return fComparator;
	}

}
