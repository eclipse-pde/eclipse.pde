/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.test.performance.PerformanceTestCase;

public class PDEModelManagerPerfTest extends PerformanceTestCase {

	/**
	 * @return
	 */
	public static Test suite() {
		return new TestSuite(PDEModelManagerPerfTest.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}


	/**
	 * @param file
	 * @throws Exception
	 */
	public void testModelManagerLoad() throws Exception {
		startMeasuring();
		PDECore.getDefault().getModelManager().getExternalModelManager()
		.getAllModels();
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
}
