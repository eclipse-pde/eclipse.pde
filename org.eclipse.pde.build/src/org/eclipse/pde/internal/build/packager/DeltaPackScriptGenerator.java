/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.packager;

import java.util.Collection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.Config;

public class DeltaPackScriptGenerator extends PackageScriptGenerator {
	public DeltaPackScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) {
		super(directory, assemblageInformation, featureId);
		groupConfigs = true;
	}

	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection binaryPlugins, Collection binaryFeatures, Collection allFeatures, Collection rootFiles) throws CoreException {
		super.basicGenerateAssembleConfigFileTargetCall(new Config("delta", "delta", "delta"), binaryPlugins, binaryFeatures, allFeatures, rootFiles); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
