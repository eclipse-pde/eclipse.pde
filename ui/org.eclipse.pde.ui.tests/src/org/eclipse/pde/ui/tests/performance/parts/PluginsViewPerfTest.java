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
		tagAsGlobalSummary("Open Plug-ins View", Dimension.CPU_TIME);
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		startMeasuring();
		page.showView(PDEPlugin.PLUGINS_VIEW_ID);
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
	}
	
}
