package org.eclipse.pde.ui.tests.performance.parts;

import junit.framework.*;

import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;
import org.eclipse.test.performance.*;
import org.eclipse.ui.*;

public class OpenManifestEditorPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(OpenManifestEditorPerfTest.class);
	}
	
	public void testOpen() throws Exception {
		tagAsGlobalSummary("Open Plug-in Editor", Dimension.CPU_TIME); //$NON-NLS-1$
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IPluginModelBase model = PDECore.getDefault().getModelManager().findPlugin("org.eclipse.jdt.ui", null, IMatchRules.NONE);
		assertFalse("org.eclipse.jdt.ui model does not exist", model == null);
		model.setEnabled(true);

		for (int i = 0; i < 20; i++) {
			startMeasuring();
			ManifestEditor.openPluginEditor("org.eclipse.jdt.ui"); //$NON-NLS-1$
			stopMeasuring();
			page.closeAllEditors(false);
		}	
		commitMeasurements();
		assertPerformance();
	}
	
}
