/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.internal.core.ExecutionEnvironmentAnalyzer;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.PluginPathFinder;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;

public class InitializeModelsPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(InitializeModelsPerfTest.class);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		deleteContent(new File(PDECore.getDefault().getStateLocation().toOSString()));
		ExecutionEnvironmentAnalyzer.getKnownExecutionEnvironments();
	}
	
	public void testModels() throws Exception {
		tagAsGlobalSummary("Initialize Plug-ins (no caching)", Dimension.ELAPSED_PROCESS);
		URL[] paths = getURLs();
		startMeasuring();
		new PDEState(paths, false, new NullProgressMonitor());
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
	public void testCachedModels() throws Exception {
		tagAsSummary("Initialize Plug-ins (with caching)", Dimension.ELAPSED_PROCESS);
		URL[] paths = getURLs();
		new PDEState(paths, true, new NullProgressMonitor());
		startMeasuring();
		new PDEState(paths, true, new NullProgressMonitor());
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
	protected void tearDown() throws Exception {
		deleteContent(new File(PDECore.getDefault().getStateLocation().toOSString()));
	}
	
	private void deleteContent(File curr) {
		if (curr.exists()) {
			if (curr.isDirectory()) {
				File[] children = curr.listFiles();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						deleteContent(children[i]);
					}
				}
			}
			curr.delete();
		}
	}
	
	private URL[] getURLs() {
		String path = ExternalModelManager.getEclipseHome().toOSString();
		URL[] paths = PluginPathFinder.getPluginPaths(path);
		// FAIR ANALYSIS: this is the number of plug-ins in 3.1.x
		URL[] result = new URL[89];
		System.arraycopy(paths, 0, result, 0, 89);
		return result;
	}

	
}
