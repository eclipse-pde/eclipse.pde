/*******************************************************************************
 *  Copyright (c) 2023, 2024 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class BndProject extends BaseProject {

	public static final String NATURE_ID = PDE_PLUGIN_ID + ".BndNature"; //$NON-NLS-1$

	public static final String INSTRUCTIONS_FILE_EXTENSION = ".bnd"; //$NON-NLS-1$

	public static final String INSTRUCTIONS_FILE = "pde" + INSTRUCTIONS_FILE_EXTENSION; //$NON-NLS-1$

	public static final String BUILDER_ID = PDE_PLUGIN_ID + ".BndBuilder"; //$NON-NLS-1$

	@Override
	public void configure() throws CoreException {
		addToBuildSpec(BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(BUILDER_ID);
	}

	public static boolean isBndProject(IProject project) {
		return hasNature(project, NATURE_ID);
	}

}
