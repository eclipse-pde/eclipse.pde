/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.packager;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.Config;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

public class DeltaPackScriptGenerator extends PackageScriptGenerator {
	public DeltaPackScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) {
		super(directory, assemblageInformation, featureId);
		groupConfigs = true;
	}

	@Override
	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection<BundleDescription> binaryPlugins, Collection<BuildTimeFeature> binaryFeatures, Collection<BuildTimeFeature> allFeatures, Collection<BuildTimeFeature> rootFiles) throws CoreException {
		super.basicGenerateAssembleConfigFileTargetCall(new Config("delta", "delta", "delta"), binaryPlugins, binaryFeatures, allFeatures, rootFiles); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
