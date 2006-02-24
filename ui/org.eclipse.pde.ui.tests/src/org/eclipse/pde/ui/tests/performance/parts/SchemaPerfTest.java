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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;

public class SchemaPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(SchemaPerfTest.class);
	}
	
	public void testLoadAllSchemas() throws Exception {
		tagAsSummary("Loading all schemas", Dimension.ELAPSED_PROCESS);
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getAllPlugins();
		startMeasuring();
		for (int i = 0; i < models.length; i++) {
			IPluginExtensionPoint[] extPoints = models[i].getPluginBase().getExtensionPoints();
			for (int j = 0; j < extPoints.length; j++) {
				PDECore.getDefault().getSchemaRegistry().getSchema(extPoints[j].getFullId());
			}
		}
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
}
