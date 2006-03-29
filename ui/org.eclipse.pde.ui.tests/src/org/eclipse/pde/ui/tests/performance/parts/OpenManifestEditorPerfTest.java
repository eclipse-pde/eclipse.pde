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

import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class OpenManifestEditorPerfTest extends PerformanceTestCase {

	public static Test suite() {
		return new TestSuite(OpenManifestEditorPerfTest.class);
	}
	
	public void testOpen() throws Exception {
		tagAsGlobalSummary("Open Plug-in Editor", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		PDECore.getDefault().getModelManager().findEntry("org.eclipse.core.runtime");
		for (int i = 0; i < 20; i++) {
			startMeasuring();
			ManifestEditor.openPluginEditor("org.eclipse.core.runtime"); //$NON-NLS-1$
			stopMeasuring();
			page.closeAllEditors(false);
		}	
		commitMeasurements();
		assertPerformance();
	}
	
}
