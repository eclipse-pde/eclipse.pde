/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.packager;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.Config;

public class DeltaPackScriptGenerator extends PackageScriptGenerator {
	public DeltaPackScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) throws CoreException {
		super(directory, assemblageInformation, featureId);
	}

	protected void generateMainTarget() throws CoreException {
			script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
			doDeltaPack();
			script.printTargetEnd();
	}
	
	private void doDeltaPack() throws CoreException {
		Collection allPlugins = new HashSet();
		Collection allFeatures = new HashSet();
		Collection allRootFiles = new HashSet();
		
		for (Iterator allConfigs = getConfigInfos().iterator(); allConfigs.hasNext();) {
			Config element = (Config) allConfigs.next();
			allPlugins.addAll(assemblageInformation.getPlugins(element));
			allFeatures.addAll(assemblageInformation.getFeatures(element));
			allRootFiles.addAll(assemblageInformation.getRootFileProviders(element));
		}
		basicGenerateAssembleConfigFileTargetCall(new Config("delta", "delta", "delta"), allPlugins, allFeatures, allFeatures, allRootFiles);		
	}
}
