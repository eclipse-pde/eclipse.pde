/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.PDECore;

public class PDE {
	private PDE() { // static use only
	}

	public static final String PLUGIN_ID = "org.eclipse.pde"; //$NON-NLS-1$

	public static final String MANIFEST_BUILDER_ID = PLUGIN_ID + "." + "ManifestBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SCHEMA_BUILDER_ID = PLUGIN_ID + "." + "SchemaBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_NATURE = PLUGIN_ID + "." + "FeatureNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_NATURE = PLUGIN_ID + "." + "UpdateSiteNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_BUILDER_ID = PLUGIN_ID + "." + "FeatureBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_BUILDER_ID = PLUGIN_ID + "." + "UpdateSiteBuilder"; //$NON-NLS-1$ //$NON-NLS-2$

	// TODO: rename to isPluginProject?
	public static boolean hasPluginNature(IProject project) {
		return hasNature(project, PLUGIN_NATURE);
	}

	// TODO: rename to isFeatureProject?
	public static boolean hasFeatureNature(IProject project) {
		return hasNature(project, FEATURE_NATURE);
	}

	public static boolean hasUpdateSiteNature(IProject project) {
		return hasNature(project, SITE_NATURE);
	}

	public static boolean hasJavaNature(IProject project) {
		return hasNature(project, JavaCore.NATURE_ID);
	}

	public static boolean isBndProject(IProject project) {
		return hasNature(project, BndProject.NATURE_ID);
	}

	private static boolean hasNature(IProject project, String nature) {
		try {
			return project.hasNature(nature);
		} catch (CoreException e) {
			PDECore.log(e);
			return false;
		}
	}

}
