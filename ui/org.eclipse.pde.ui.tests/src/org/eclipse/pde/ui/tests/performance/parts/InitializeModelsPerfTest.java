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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
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
	
	public void testModels() throws Exception {
		tagAsGlobalSummary("Initialize Plug-ins (no caching)", Dimension.CPU_TIME);
		String path = ExternalModelManager.getEclipseHome().toOSString();
		startMeasuring();
		new PDEState(PluginPathFinder.getPluginPaths(path), true, new NullProgressMonitor());
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
	public void testCachedModels() throws Exception {
		tagAsGlobalSummary("Initialize Plug-ins (with caching)", Dimension.CPU_TIME);
		String path = ExternalModelManager.getEclipseHome().toOSString();
		new PDEState(PluginPathFinder.getPluginPaths(path), true, new NullProgressMonitor());
		startMeasuring();
		new PDEState(PluginPathFinder.getPluginPaths(path), true, new NullProgressMonitor());
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

	
}
