/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

/**
 * 
 */
public class ExternalJavaSearchInitializer extends ClasspathContainerInitializer {

	/**
	 * Constructor for RequiredPluginsInitializer.
	 */
	public ExternalJavaSearchInitializer() {
		super();
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(IPath, IJavaProject)
	 */
	public void initialize(IPath containerPath, IJavaProject javaProject)
		throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		SearchablePluginsManager searchManager = manager.getSearchablePluginsManager();
		searchManager.updateClasspathContainer(javaProject);
	}
}
