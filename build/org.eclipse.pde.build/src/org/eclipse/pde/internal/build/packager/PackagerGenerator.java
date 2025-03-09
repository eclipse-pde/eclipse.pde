/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

public class PackagerGenerator extends BuildScriptGenerator {
	private String featureList = null;
	private String propertyFile;
	private boolean groupConfigs2;

	public PackagerGenerator() {
		generateBuildScript = false;
		children = true;
		groupConfigs2 = false;
	}

	public void setFeatureList(String features) {
		featureList = features;
	}

	@Override
	protected void sortElements(List<String> features, List<String> plugins) {
		String[] allFeatures = Utils.getArrayFromString(featureList);
		for (String feature2 : allFeatures) {
			features.add(feature2);
		}
	}

	@Override
	protected void generatePackageScripts(AssemblyInformation assemblageInformation, String[] featureInfo, BuildTimeSiteFactory factory) throws CoreException {
		PackageScriptGenerator assembler = null;
		if (groupConfigs2) {
			assembler = new DeltaPackScriptGenerator(workingDirectory, assemblageInformation, featureInfo[0]);
		} else {
			assembler = new PackageScriptGenerator(workingDirectory, assemblageInformation, featureInfo[0]);
		}

		assembler.setSignJars(signJars);
		assembler.setGenerateJnlp(generateJnlp);
		assembler.setArchivesFormat(getArchivesFormat());
		assembler.setPropertyFile(propertyFile);
		assembler.setBackwardCompatibleName(true);
		assembler.setBuildSiteFactory(factory);
		assembler.setProduct(product);
		assembler.setProductQualifier(productQualifier);
		assembler.setVersionsList(generateVersionsList);
		assembler.generate();
	}

	public void setPropertyFile(String propertyFile) {
		this.propertyFile = propertyFile;
	}

	public void groupConfigs(boolean value) {
		groupConfigs2 = value;
	}
}
