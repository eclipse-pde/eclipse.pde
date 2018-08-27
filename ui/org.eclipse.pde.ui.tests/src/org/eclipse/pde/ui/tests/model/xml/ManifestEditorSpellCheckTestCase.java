/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.model.xml;

import java.io.*;
import java.util.Iterator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

/**
 * Test cases for ensuring the source page in Manifest Editor for plug-in.xml
 * remains untouched by spell check changes.
 *
 * bug #286203
 */
public class ManifestEditorSpellCheckTestCase extends XMLModelTestCase {

	private IEditorPart fEditor;
	private IProject fProject;
	private static final String EDITOR_ID = "org.eclipse.pde.ui.manifestEditor";

	@Override
	protected void setUp() throws Exception {

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		fProject = root.getProject("model.tests.editor");
		if (fProject.exists()) {
			fProject.delete(true, true, null);
		}
		fProject.create(null);
		fProject.open(null);
		super.setUp();

		StringBuilder buffer = new StringBuilder("<extension point=\"mispel\"><sample />");
		buffer.append("<sample1 id=\"tast\" /><sample2 /></extension>");
		setXMLContents(buffer, LF);
	}

	/**
	 * Checks that no spelling annotations are created.
	 */
	public void testNoSpellingAnnotation(){

		try {
			createAndOpenFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR, fDocument.get());
		} catch (CoreException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			fail(e.getMessage());
		}

		ManifestEditor editor = (ManifestEditor) fEditor;
		ManifestSourcePage simpleCSSrcPage = (ManifestSourcePage) editor.getActivePageInstance();
		ISourceViewer sourceViewer = simpleCSSrcPage.getViewer();

		IAnnotationModel model = sourceViewer.getAnnotationModel();
		Iterator<Annotation> iter = model.getAnnotationIterator();
		int spellingAnnotations = 0;
		while (iter.hasNext()) {
			Annotation annotation = iter.next();
			if (annotation instanceof SpellingAnnotation) {
				spellingAnnotations++;
			}
		}
		assertEquals(0, spellingAnnotations);

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		page.closeAllEditors(false);
	}

	private void createAndOpenFile(String fileName, String fileContents) throws  CoreException, IOException{
		IPath path = fProject.getLocation();
		path = path.append(fileName);
		IFile projectFile = fProject.getFile(fileName);
		File file = path.toFile();
		if (projectFile.exists()){
			projectFile.delete(true, null);
		}
		file.createNewFile();
		projectFile.create(new FileInputStream(file), true, null);

		try (FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				BufferedWriter bw = new BufferedWriter(osw)) {
			bw.write(fileContents);
			bw.flush();
		}

		projectFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		fEditor = IDE.openEditor(PDEPlugin.getActivePage(), projectFile, EDITOR_ID, true);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		PDEFormEditor editor = (PDEFormEditor) fEditor;
		editor.setActivePage(PluginInputContext.CONTEXT_ID);
	}
}
