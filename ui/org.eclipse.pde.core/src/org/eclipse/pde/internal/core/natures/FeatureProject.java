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

public class FeatureProject extends BaseProject {

	public static final String NATURE = PDE_PLUGIN_ID + ".FeatureNature"; //$NON-NLS-1$
	public static final String BUILDER_ID = PDE_PLUGIN_ID + ".FeatureBuilder"; //$NON-NLS-1$

	@Override
	public void configure() throws CoreException {
		addToBuildSpec(BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(BUILDER_ID);
	}

	public static boolean isFeatureProject(IProject project) {
		return hasNature(project, NATURE);
	}

}
