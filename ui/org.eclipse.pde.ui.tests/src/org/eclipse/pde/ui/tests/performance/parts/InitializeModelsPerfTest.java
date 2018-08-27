/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;

/**
 * Calls the load target platform job which will take a resolved target definition and
 * initialize the PDE models from its content.
 *
 */
public class InitializeModelsPerfTest extends PerformanceTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		deleteContent(new File(PDECore.getDefault().getStateLocation().toOSString()));
		TargetPlatformHelper.getKnownExecutionEnvironments();
	}

	public void testModels() throws Exception {
		tagAsGlobalSummary("Initialize PDE Models", Dimension.ELAPSED_PROCESS);
		IPath testBundles = TargetPlatformPerfTest.extractTargetPerfTestPlugins();
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition originalTarget = tps.newTarget();
		originalTarget.setTargetLocations(new ITargetLocation[]{tps.newDirectoryLocation(testBundles.toPortableString())});
		tps.saveTargetDefinition(originalTarget);

		// Target resolution performance handled in TargetPlatformPerfTest
		originalTarget.resolve(new NullProgressMonitor());

		LoadTargetDefinitionJob load = new LoadTargetDefinitionJob(originalTarget);
		LoadTargetDefinitionJob clear = new LoadTargetDefinitionJob(null);

		// Warm-up Iterations
		for (int i = 0; i < 3; i++) {
			load.schedule();
			load.join();
			clear.schedule();
			clear.join();
		}
		// Test Iterations
		for (int i = 0; i < 50; i++) {
			startMeasuring();
			load.schedule();
			load.join();
			stopMeasuring();
			clear.schedule();
			clear.join();
		}
		commitMeasurements();
		assertPerformance();
	}

	@Override
	protected void tearDown() throws Exception {
		deleteContent(new File(PDECore.getDefault().getStateLocation().toOSString()));
	}

	private void deleteContent(File curr) {
		if (curr.exists()) {
			if (curr.isDirectory()) {
				File[] children = curr.listFiles();
				if (children != null) {
					for (File element : children) {
						deleteContent(element);
					}
				}
			}
			curr.delete();
		}
	}

}
