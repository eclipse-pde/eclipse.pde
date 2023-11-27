/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

public class PluginProject extends BaseProject {

	public static final String MANIFEST_BUILDER_ID = PDE_PLUGIN_ID + ".ManifestBuilder"; //$NON-NLS-1$
	public static final String SCHEMA_BUILDER_ID = PDE_PLUGIN_ID + ".SchemaBuilder"; //$NON-NLS-1$
	public static final String NATURE = PDE_PLUGIN_ID + ".PluginNature"; //$NON-NLS-1$

	@Override
	public void configure() throws CoreException {
		addToBuildSpec(MANIFEST_BUILDER_ID);
		addToBuildSpec(SCHEMA_BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(MANIFEST_BUILDER_ID);
		removeFromBuildSpec(SCHEMA_BUILDER_ID);
	}

	public static boolean isPluginProject(IProject project) {
		return hasNature(project, NATURE);
	}

	public static boolean isJavaProject(IProject project) {
		return hasNature(project, JavaCore.NATURE_ID);
	}

}
