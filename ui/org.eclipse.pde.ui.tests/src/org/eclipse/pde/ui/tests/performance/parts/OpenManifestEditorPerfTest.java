/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import java.io.File;
import java.net.URL;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class OpenManifestEditorPerfTest extends PerformanceTestCase {

	private static final String F_PLUGIN_FILE = "/tests/performance/plugin/org.eclipse.jdt.ui/plugin.xml"; //$NON-NLS-1$

	private static final String F_MANIFEST_FILE = "/tests/performance/manifest/org.eclipse.jdt.ui/MANIFEST.MF"; //$NON-NLS-1$

	private static final int F_TEST_ITERATIONS = 25;

	private static final int F_WARMUP_ITERATIONS = 10;

	private static File fPluginFile;

	private static File fManifestFile;

	private static IWorkbenchPage fActivePage;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Bundle bundle = getBundle();
		// Get plug-in.xml file
		fPluginFile = getFile(bundle, F_PLUGIN_FILE);
		// Get MANIFEST.MF file
		fManifestFile = getFile(bundle, F_MANIFEST_FILE);
		// Get the active workbench page
		fActivePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		// Disable code folding feature
		disableCodeFoldingFeature();
	}

	private void disableCodeFoldingFeature() throws Exception {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED)) {
			store.setValue(IPreferenceConstants.EDITOR_FOLDING_ENABLED, false);
		}
	}

	private Bundle getBundle() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(OpenManifestEditorPerfTest.class);
		if (bundle == null) {
			throw new Exception("ERROR:  Bundle uninitialized"); //$NON-NLS-1$
		}
		return bundle;
	}

	private File getFile(Bundle bundle, String filename) throws Exception {
		URL url = bundle.getEntry(filename);
		if (url == null) {
			throw new Exception("ERROR:  URL not found:  " + filename); //$NON-NLS-1$
		}
		String path = FileLocator.resolve(url).getPath();
		if ("".equals(path)) { //$NON-NLS-1$
			throw new Exception("ERROR:  URL unresolved:  " + filename); //$NON-NLS-1$
		}
		return new File(path);
	}

	public void testEditorOpenXML() throws Exception {
		tagAsSummary("Open Plug-in Editor: plugin.xml", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		executeTestRun(fPluginFile);
	}

	public void testEditorOpenMF() throws Exception {
		tagAsSummary("Open Plug-in Editor: MANIFEST.MF", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		executeTestRun(fManifestFile);
	}

	private void executeTestRun(File file) throws Exception {
		// Create the file editor input
		IFileStore store = EFS.getStore(file.toURI());
		FileStoreEditorInput editorInput = new FileStoreEditorInput(store);
		// Warm-up Iterations
		for (int i = 0; i < F_WARMUP_ITERATIONS; i++) {
			IEditorPart editorPart = openEditor(editorInput);
			closeEditor(editorPart);
		}
		// Test Iterations
		for (int i = 0; i < F_TEST_ITERATIONS; i++) {
			startMeasuring();
			IEditorPart editorPart = openEditor(editorInput);
			stopMeasuring();
			closeEditor(editorPart);
		}
		commitMeasurements();
		assertPerformance();
	}

	private IEditorPart openEditor(IEditorInput editorInput) throws Exception {
		// Open the editor
		return IDE.openEditor(fActivePage, editorInput, IPDEUIConstants.MANIFEST_EDITOR_ID, true);
	}

	private void closeEditor(IEditorPart editorPart) throws Exception {
		// Close the editor
		fActivePage.closeEditor(editorPart, false);
	}

}
