/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.pde.ui.tests.PDETestsPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.Bundle;

/**
 * AbstractSchemaPerfTest
 *
 */
public abstract class AbstractSchemaPerfTest extends PerformanceTestCase {

	protected int fTestIterations;

	protected int fWarmupIterations;

	protected int fRuns;

	protected static final String F_FILENAME = "/tests/performance/schema/navigatorContent.exsd"; //$NON-NLS-1$

	protected static File fXSDFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setUpSchemaFile();
		setUpIterations();
	}

	/**
	 *
	 */
	protected void setUpIterations() {
		fTestIterations = 5;
		fWarmupIterations = 50;
		fRuns = 200;
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 */
	private void setUpSchemaFile() throws Exception, IOException {
		PDETestsPlugin plugin = PDETestsPlugin.getDefault();
		if (plugin == null)
			throw new Exception("ERROR:  Macro plug-in uninitialized"); //$NON-NLS-1$
		Bundle bundle = plugin.getBundle();
		if (bundle == null)
			throw new Exception("ERROR:  Bundle uninitialized"); //$NON-NLS-1$
		URL url = bundle.getEntry(F_FILENAME);
		if (url == null)
			throw new Exception("ERROR:  URL not found:  " + F_FILENAME); //$NON-NLS-1$
		String path = FileLocator.resolve(url).getPath();
		if ("".equals(path)) //$NON-NLS-1$
			throw new Exception("ERROR:  URL unresolved:  " + F_FILENAME); //$NON-NLS-1$
		fXSDFile = new File(path);
	}

	/**
	 * @throws Exception
	 */
	protected void executeTestRun() throws Exception {
		// Warm-up Iterations
		for (int i = 0; i < fWarmupIterations; i++) {
			executeTest();
		}
		// Test Iterations
		for (int j = 0; j < fRuns; j++) {
			startMeasuring();
			for (int i = 0; i < fTestIterations; i++) {
				executeTest();
			}
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * @throws Exception
	 */
	protected abstract void executeTest() throws Exception;

}
