/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.Bundle;

/**
 * SchemaLoaderPerfTest
 *
 */
public class SchemaLoaderPerfTest extends PerformanceTestCase {

	private static final String F_FILENAME = 
		"/tests/performance/schema/navigatorContent.exsd"; //$NON-NLS-1$
	
	private static final int F_TEST_ITERATIONS = 3;

	private static final int F_WARMUP_ITERATIONS = 20;
	
	private static final int F_RUNS = 50;
	
	private static File fXSDFile;
	
	/**
	 * @return
	 */
	public static Test suite() {
		return new TestSuite(SchemaLoaderPerfTest.class);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.test.performance.PerformanceTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		MacroPlugin plugin = MacroPlugin.getDefault();
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
	public void testSchemaUnabbreviated() throws Exception {
		tagAsGlobalSummary("Loading Unabbreviated Schema", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		testSchemaLoader(false);
	}
	
	/**
	 * @throws Exception
	 */
	public void testSchemaAbbreviated() throws Exception {
		tagAsGlobalSummary("Loading Abbreviated Schema", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		testSchemaLoader(true);
	}
	
	/**
	 * @param abbreviated
	 * @throws Exception
	 */
	private void testSchemaLoader(boolean abbreviated) throws Exception {
		
		// Warm-up Iterations
		for (int i = 0; i < F_WARMUP_ITERATIONS; i++) {
			loadSchema(abbreviated);
		}		
		// Test Iterations
		for (int j = 0; j < F_RUNS; j++) {
			startMeasuring();
			for (int i = 0; i < F_TEST_ITERATIONS; i++) {
				loadSchema(abbreviated);
			}
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();
	}
	
	/**
	 * @param abbreviated
	 * @throws Exception
	 */
	private void loadSchema(boolean abbreviated) throws Exception {
		File schemaFile = fXSDFile;
		SchemaDescriptor descriptor = new SchemaDescriptor(schemaFile);
		Schema schema = (Schema)descriptor.getSchema(abbreviated);
		if (schema.getName() == null) {
			throw new Exception("ERROR: Extension point schema name missing"); //$NON-NLS-1$
		}		
	}
	
}
