/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.team.core.*;

public class BinaryProjectFilter extends ViewerFilter {

	/**
	 * Constructor for BinaryProjectFilter.
	 */
	public BinaryProjectFilter() {
		super();
	}

	/**
	 * @see ViewerFilter#select(Viewer, Object, Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IProject project = null;

		if (element instanceof IJavaProject) {
			project = ((IJavaProject) element).getProject();
		} else if (element instanceof IProject) {
			project = (IProject) element;
		}
		if (project != null) {
			if (isPluginProject(project) || isFeatureProject(project)) {
				return !isBinary(project);
			}
		}
		return true;
	}
	
	private boolean isPluginProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("plugin.xml")) //$NON-NLS-1$
			|| project.exists(new Path("fragment.xml")) || project.exists(new Path("META-INF/MANIFEST.MF")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private boolean isFeatureProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("feature.xml")); //$NON-NLS-1$
	}
	
	private boolean isBinary(IProject project) {
		try {
			String binary = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
			if (binary != null) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project);
				return provider==null || provider instanceof BinaryRepositoryProvider;
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return false;
	}
}
