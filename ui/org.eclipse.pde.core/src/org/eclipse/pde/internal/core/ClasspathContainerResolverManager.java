/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      IBM Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IBundleClasspathResolver;

/**
 * Manager for client contributed IBundleClasspathResolvers. Collects the resolvers from the <code>
 * org.eclipse.pde.core.bundleClasspathResolvers</code> extension point.  Classpath resolvers
 * can then be asked to provide additional bundle classpath and source lookup entries for a project.
 * 
 * @see IBundleClasspathResolver
 */
public class ClasspathContainerResolverManager {

	private static final String POINT_ID = "org.eclipse.pde.core.bundleClasspathResolvers"; //$NON-NLS-1$
	private static final String ATT_NATURE = "nature"; //$NON-NLS-1$
	private static final String ATT_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Returns all classpath resolvers contributed via extension point that support the given project's nature.
	 * 
	 * @param project project to check nature of
	 * @return all classpath resolvers that support the nature, possibly empty
	 */
	public IBundleClasspathResolver[] getBundleClasspathResolvers(IProject project) {
		List result = new ArrayList();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(POINT_ID);
		for (int i = 0; i < elements.length; i++) {
			String attrNature = elements[i].getAttribute(ATT_NATURE);
			try {
				if (project.isNatureEnabled(attrNature)) {
					result.add(elements[i].createExecutableExtension(ATT_CLASS));
				}
			} catch (CoreException e) {
				PDECore.log(e.getStatus());
			}
		}

		return (IBundleClasspathResolver[]) result.toArray(new IBundleClasspathResolver[result.size()]);
	}

}
