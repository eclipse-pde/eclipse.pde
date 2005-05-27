/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * ResourceExtender provides property testers for the XML expression language
 * evaluation. We provide a copy in PDE so that launch shortcuts can add
 * contextual launch enablement that does not require their plugins to be
 * loaded.
 */
public class ResourceExtender extends PropertyTester {

	private static final String PDE_NATURE = "PluginNature"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object,
	 *      java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	public boolean test(Object receiver, String method, Object[] args,
			Object expectedValue) {
		IResource resource = (IResource) ((IAdaptable) receiver)
				.getAdapter(IResource.class);
		if (resource != null) {
			if (PDE_NATURE.equals(method)) {
				try {
					IProject proj = resource.getProject();
					return proj.isAccessible()
							&& proj.hasNature("org.eclipse.pde.PluginNature"); //$NON-NLS-1$
				} catch (CoreException e) {
					return true;
				}
			}
		}
		return true;
	}

}
