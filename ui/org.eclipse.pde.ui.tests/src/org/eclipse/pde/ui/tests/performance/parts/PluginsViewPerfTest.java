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

import org.eclipse.pde.internal.ui.*;
import org.eclipse.test.performance.*;
import org.eclipse.ui.*;


public class PluginsViewPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(PluginsViewPerfTest.class);
	}
	
	public void testOpen() throws Exception {
		tagAsGlobalSummary("Open Plug-ins View", Dimension.CPU_TIME); //$NON-NLS-1$
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		startMeasuring();
		page.showView(PDEPlugin.PLUGINS_VIEW_ID);
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
}
