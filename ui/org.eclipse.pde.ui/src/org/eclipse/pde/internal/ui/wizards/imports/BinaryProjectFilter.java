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

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class BinaryProjectFilter extends ViewerFilter {
	private static final QualifiedName IMPORTED_KEY = new QualifiedName("org.eclipse.pde.core", "imported");
	private static final QualifiedName TEAM_KEY = new QualifiedName("org.eclipse.team.core", "repository");

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
		return project.exists(new Path("plugin.xml"))
			|| project.exists(new Path("fragment.xml"));
	}
	
	private boolean isFeatureProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("feature.xml"));
	}
	
	private boolean isBinary(IProject project) {
		try {
			String value = project.getPersistentProperty(IMPORTED_KEY);
			if (value==null) return false;
			if (value.equals("external"))
				return true;
			if (value.equals("binary")) {
				if (project.getSessionProperty(TEAM_KEY)==null)
					return true;
			}
			return false;
		}
		catch (CoreException e) {
			return false;
		}
	}
}
