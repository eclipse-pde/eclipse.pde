/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class BundleProjectTester extends PropertyTester {
	
	public static final String BUNDLE_PROPERTY = "BundleProject"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object,
	 *      java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	public boolean test(Object receiver, String method, Object[] args,
			Object expectedValue) {
		IProject project = (IProject)receiver;
		if (BUNDLE_PROPERTY.equals(method)) {
			try {
				return isBundleProject(project);
			} catch (CoreException e) {
				return true;
			}
		}
		return false;
	}

	protected boolean isBundleProject(IProject project) throws CoreException {
		return project != null 
				&& project.isOpen()
				&& project.getFile("META-INF/MANIFEST.MF").exists() //$NON-NLS-1$
				&& project.hasNature("org.eclipse.pde.PluginNature"); //$NON-NLS-1$
	}

}
