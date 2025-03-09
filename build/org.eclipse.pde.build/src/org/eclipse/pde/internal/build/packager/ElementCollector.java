/*******************************************************************************
 *  Copyright (c) 2005, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.Config;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

public class ElementCollector extends BuildDirector {
	public ElementCollector(AssemblyInformation assemblageInformation) {
		super(assemblageInformation);
	}

	@Override
	protected void generateIncludedFeatureBuildFile(BuildTimeFeature feature) throws CoreException {
		FeatureEntry[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (FeatureEntry referencedFeature : referencedFeatures) {
			String featureId = referencedFeature.getId();

			BuildTimeFeature nestedFeature = getSite(false).findFeature(featureId, null, true);

			try {
				generate(nestedFeature, false);
			} catch (CoreException exception) {
				//If the referenced feature is not optional, there is a real problem and the exception is re-thrown. 
				if (exception.getStatus().getCode() != EXCEPTION_FEATURE_MISSING || (exception.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !referencedFeature.isOptional())) {
					throw exception;
				}
			}
		}
	}

	@Override
	protected void collectElementToAssemble(BuildTimeFeature featureToCollect) {
		if (assemblyData == null) {
			return;
		}
		List<Config> correctConfigs = selectConfigs(featureToCollect);
		// Here, we could sort if the feature is a common one or not by comparing the size of correctConfigs
		for (Config config : correctConfigs) {
			assemblyData.addFeature(config, featureToCollect);
		}
	}

}
