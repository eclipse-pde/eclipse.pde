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

import junit.framework.*;

import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.test.performance.*;

public class SchemaPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(SchemaPerfTest.class);
	}
	
	public void testLoadAllSchemas() throws Exception {
		ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
		startMeasuring();
		long start = System.currentTimeMillis();
		for (int i = 0; i < entries.length; i++) {
			IPluginModelBase model = entries[i].getActiveModel();
			if (model == null)
				continue;
			IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
			for (int j = 0; j < extPoints.length; j++) {
				PDECore.getDefault().getSchemaRegistry().getSchema(extPoints[j].getFullId());
			}
		}
		System.out.println("Time elapsed: " + (System.currentTimeMillis() - start) + " ms");
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
}
