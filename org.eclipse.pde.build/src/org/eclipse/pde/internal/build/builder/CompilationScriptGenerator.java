/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.*;

/**
 *
 */
public class CompilationScriptGenerator extends AbstractScriptGenerator {

	private String featureId = "all"; //$NON-NLS-1$

	/** Contain the elements that will be assembled */
	protected AssemblyInformation assemblyData;
	protected BuildDirector director;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		openScript(getWorkingDirectory(), getScriptName());
		try {
			generateScript();
		} finally {
			closeScript();
		}
	}

	public void setAssemblyData(AssemblyInformation assemblyData) {
		this.assemblyData = assemblyData;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public void setDirector(BuildDirector director) {
		this.director = director;
	}

	protected String getScriptName() {
		return DEFAULT_COMPILE_NAME + '.' + featureId + ".xml"; //$NON-NLS-1$
	}

	private void generateScript() throws CoreException {
		generatePrologue();
		generatePlugins();
		generateEpilogue();
	}

	private void generatePrologue() {
		script.printProjectDeclaration("Compile " + featureId, TARGET_MAIN, null); //$NON-NLS-1$

		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateEpilogue() {
		script.printTargetEnd();
		script.printProjectEnd();
	}

	private void generatePlugins() throws CoreException {
		Set plugins = assemblyData.getAllCompiledPlugins();
		List sortedPlugins = Utils.extractPlugins(getSite(false).getRegistry().getSortedBundles(), plugins);
		IPath basePath = new Path(workingDirectory);
		for (Iterator iterator = sortedPlugins.iterator(); iterator.hasNext();) {
			BundleDescription bundle = (BundleDescription) iterator.next();
			// Individual source bundles have empty build.jars targets, skip them
			if (Utils.isSourceBundle(bundle))
				continue;
			IPath location = Utils.makeRelative(new Path(getLocation(bundle)), basePath);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), TARGET_BUILD_JARS, null, null, null);
		}
	}

}
