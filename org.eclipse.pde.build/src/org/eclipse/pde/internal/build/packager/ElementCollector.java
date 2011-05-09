/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.util.Iterator;
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

	protected void generateIncludedFeatureBuildFile(BuildTimeFeature feature) throws CoreException {
		FeatureEntry[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = referencedFeatures[i].getId();

			BuildTimeFeature nestedFeature = getSite(false).findFeature(featureId, null, true);

			try {
				generate(nestedFeature, false);
			} catch (CoreException exception) {
				//If the referenced feature is not optional, there is a real problem and the exception is re-thrown. 
				if (exception.getStatus().getCode() != EXCEPTION_FEATURE_MISSING || (exception.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !referencedFeatures[i].isOptional()))
					throw exception;
			}
		}
	}

	protected void collectElementToAssemble(BuildTimeFeature featureToCollect) {
		if (assemblyData == null)
			return;
		List correctConfigs = selectConfigs(featureToCollect);
		// Here, we could sort if the feature is a common one or not by comparing the size of correctConfigs
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			assemblyData.addFeature(config, featureToCollect);
		}
	}

}
