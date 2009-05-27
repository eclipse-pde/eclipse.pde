/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;

public class PDE {
	public static final String PLUGIN_ID = "org.eclipse.pde"; //$NON-NLS-1$

	public static final String MANIFEST_BUILDER_ID = PLUGIN_ID + "." + "ManifestBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SCHEMA_BUILDER_ID = PLUGIN_ID + "." + "SchemaBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_NATURE = PLUGIN_ID + "." + "FeatureNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_NATURE = PLUGIN_ID + "." + "UpdateSiteNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_BUILDER_ID = PLUGIN_ID + "." + "FeatureBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_BUILDER_ID = PLUGIN_ID + "." + "UpdateSiteBuilder"; //$NON-NLS-1$ //$NON-NLS-2$

	public static boolean hasPluginNature(IProject project) {
		try {
			return project.hasNature(PLUGIN_NATURE);
		} catch (CoreException e) {
			PDECore.log(e);
			return false;
		}
	}

	public static boolean hasFeatureNature(IProject project) {
		try {
			return project.hasNature(FEATURE_NATURE);
		} catch (CoreException e) {
			PDECore.log(e);
			return false;
		}
	}

	public static boolean hasUpdateSiteNature(IProject project) {
		try {
			return project.hasNature(SITE_NATURE);
		} catch (CoreException e) {
			PDECore.log(e);
			return false;
		}
	}

}
