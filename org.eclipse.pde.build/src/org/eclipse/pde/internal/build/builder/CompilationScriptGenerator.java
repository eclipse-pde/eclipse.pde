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
	private boolean parallel = true;
	private int threadCount = 0;
	private int threadsPerProcessor = 3;

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

	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}

	public void setThreadCount(int count) {
		if (count != -1)
			this.threadCount = count;
	}

	public void setThreadsPerProcessor(int threads) {
		if (threads != -1)
			this.threadsPerProcessor = threads;
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

		Set bucket = null;
		if (parallel) {
			bucket = new HashSet();
			script.printParallel(threadCount, threadsPerProcessor);
		}

		for (Iterator iterator = sortedPlugins.iterator(); iterator.hasNext();) {
			BundleDescription bundle = (BundleDescription) iterator.next();
			// Individual source bundles have empty build.jars targets, skip them
			if (Utils.isSourceBundle(bundle))
				continue;

			if (parallel) {
				if (requiredInBucket(bundle, bucket)) {
					script.printEndParallel();
					script.printParallel(threadCount, threadsPerProcessor);
					bucket.clear();
				}
				bucket.add(new Long(bundle.getBundleId()));
			}

			IPath location = Utils.makeRelative(new Path(getLocation(bundle)), basePath);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), TARGET_BUILD_JARS, null, null, null);
		}

		if (parallel)
			script.printEndParallel();
	}

	private boolean requiredInBucket(BundleDescription bundle, Set bucket) {
		Properties properties = (Properties) bundle.getUserObject();
		if (properties != null) {
			String required = properties.getProperty(PROPERTY_REQUIRED_BUNDLE_IDS);
			if (required != null) {
				String[] ids = Utils.getArrayFromString(required, ":"); //$NON-NLS-1$
				for (int i = 0; i < ids.length; i++) {
					try {
						if (bucket.contains(new Long(ids[i]))) {
							return true;
						}
					} catch (NumberFormatException e) {
						//ignore
					}
				}
			}
		}
		return false;
	}
}
