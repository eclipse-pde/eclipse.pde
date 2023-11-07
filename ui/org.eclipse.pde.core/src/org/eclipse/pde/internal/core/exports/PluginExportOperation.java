/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.util.Dictionary;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

public class PluginExportOperation extends FeatureBasedExportOperation {

	public PluginExportOperation(FeatureExportInfo info, String name) {
		super(info, name);
	}

	@Override
	protected void createPostProcessingFiles() {
		createPostProcessingFile(new File(fFeatureLocation, PLUGIN_POST_PROCESSING));
	}

	@Override
	protected State getState(String os, String ws, String arch) {
		// the way plug-in export works, the os, ws and arch should ALWAYS equal the target settings.
		if (os.equals(TargetPlatform.getOS()) && ws.equals(TargetPlatform.getWS()) && arch.equals(TargetPlatform.getOSArch()) && fStateCopy != null) {
			fStateCopy.resolve(true);
			return fStateCopy;
		}
		return super.getState(os, ws, arch);
	}

	@Override
	protected boolean shouldAddPlugin(BundleDescription bundle, Dictionary<String, String> environment) {
		// if there is an environment conflict
		boolean conflict = !super.shouldAddPlugin(bundle, environment);
		if (conflict) {
			// make a copy of the state if we haven't already
			if (fStateCopy == null) {
				copyState(TargetPlatformHelper.getState());
			}
			// replace the current BundleDescription with a copy who does not have the platform filter.  This will allow the plug-in to be resolved
			BundleDescription desc = fStateCopy.removeBundle(bundle.getBundleId());
			BundleDescription newDesc = fStateCopy.getFactory().createBundleDescription(desc.getBundleId(), desc.getSymbolicName(), desc.getVersion(), desc.getLocation(), desc.getRequiredBundles(), desc.getHost(), desc.getImportPackages(), desc.getExportPackages(), desc.isSingleton(), desc.attachFragments(), desc.dynamicFragments(), null, desc.getExecutionEnvironments(), desc.getGenericRequires(), desc.getGenericCapabilities());
			fStateCopy.addBundle(newDesc);
		}
		// always include plug-ins, even ones with environment conflicts
		return true;
	}
}
