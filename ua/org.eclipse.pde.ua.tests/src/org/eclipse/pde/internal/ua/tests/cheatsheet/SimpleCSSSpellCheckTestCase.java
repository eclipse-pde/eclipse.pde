/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.tests.cheatsheet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSEditor;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSSourcePage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

/**
 * Tests the spelling annotations in a Simple Cheat sheet editor.
 *
 */
public class SimpleCSSSpellCheckTestCase extends AbstractCheatSheetModelTestCase {

	private IEditorPart fEditor;
	private IProject fProject;
	private static final String EDITOR_ID = "org.eclipse.pde.ua.ui.simpleCheatSheetEditor";

	protected void setUp() throws Exception {

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		fProject = root.getProject("ua.tests.cs");
		if (fProject.exists()) {
			fProject.delete(true, true, null);
		}
		fProject.create(null);
		fProject.open(null);
	}

	/**
	 * Testing for the spelling error annotation being present at the right location 
	 * in a string with double quotes
	 */
	public void testSpellingErrorInDoubleQuotedStringTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>\"Bodi\"</b>"); //The spelling error shall appear here
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 87, 2, 1);
	}

	/**
	 * Testing for the spelling error annotation being present at the right location 
	 * in a string with single quotes
	 */
	public void testSpellingErrorInSingleQuotedStringTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>'Bodi'</b>"); //The spelling error shall appear here
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 87, 2, 1);

	}

	/**
	 * Testing for the spelling error annotation being present at the right location 
	 * in a double quoted string containing single quotes
	 */
	public void testSpellingErrorInDoubleQuotedStringWithSingleQuotesTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>\"'single quoted string' Bodi\"</b>"); //The spelling error shall appear here
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 110, 2, 1);

	}
	
	/**
	 * Testing for the spelling error annotation being present at the right location 
	 * in a single quoted string containing double quotes
	 */
	public void testSpellingErrorInDoubleQuotedStringWithDoubleQuotesTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>'Bodi\" is not a correct spelling\"'</b>"); //The spelling error shall appear here
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 87, 2, 1);

	}
	private void validateAnnotations(String contents, int position, int totalAnnotationCount, int spellingAnnotationCount) {

		try {
			createAndOpenFile("SimpleCS.xml", contents);
		} catch (CoreException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			fail(e.getMessage());
		}

		SimpleCSEditor editor = (SimpleCSEditor) fEditor;
		SimpleCSSourcePage simpleCSSrcPage = (SimpleCSSourcePage) editor.getActivePageInstance();
		ISourceViewer sourceViewer = simpleCSSrcPage.getViewer();

		IAnnotationModel model = sourceViewer.getAnnotationModel();
		Iterator iter = model.getAnnotationIterator();
		int actualTotalAnnotationCount = 0;
		int actualSpellingAnnotationCount = 0;
		while (iter.hasNext()) {
			actualTotalAnnotationCount++;
			Annotation annotation = (Annotation) iter.next();
			if (annotation instanceof SpellingAnnotation) {
				actualSpellingAnnotationCount++;
				if (position != 0) {
					int offset = ((SpellingAnnotation) annotation).getSpellingProblem().getOffset();
					assertEquals(position, offset);
				}
			}
		}
		assertEquals(totalAnnotationCount, actualTotalAnnotationCount);
		assertEquals(spellingAnnotationCount, actualSpellingAnnotationCount);

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		page.closeAllEditors(false);
	}

	/**
	 * The spelling error annotation should not appear for spelling errors in an XML tag
	 */
	public void testNoSpellingAnnotationForXMLTag() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>\"Body\"</b>");
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}
	
	/**
	 * Testing for the multiple spelling error annotations
	 */
	public void testMultipleSpellingErrorsTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Tital\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>\"Bodi\"</b>");
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Itim\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 4, 3);
	}

	/**
	 * Testing for the zero spelling error annotations
	 */
	public void testZeroSpellingErrorsTestCase() {
		StringBuffer csText = new StringBuffer();

		csText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		csText.append("<cheatsheet");
		csText.append("title=\"Title\">");
		csText.append("<intro>");
		csText.append("<description>");
		csText.append("<b>\"Body\"</b>");
		csText.append("</description>");
		csText.append("</intro>");
		csText.append("<item");
		csText.append("title=\"Item\">");
		csText.append("<description>");
		csText.append("<b>Body</b>");
		csText.append("</description>");
		csText.append("</item>");
		csText.append("</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}
	private void createAndOpenFile(String fileName, String fileContents) throws CoreException, IOException {
		IPath path = fProject.getLocation();
		path = path.append(fileName);
		IFile projectFile = fProject.getFile(fileName);
		File file = path.toFile();
		if (projectFile.exists()) {
			projectFile.delete(true, null);
		}
		file.createNewFile();
		projectFile.create(new FileInputStream(file), true, null);

		FileOutputStream fos = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		bw.write(fileContents);
		bw.flush();
		bw.close();

		projectFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		fEditor = IDE.openEditor(page, projectFile, EDITOR_ID, true);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		SimpleCSEditor editor = (SimpleCSEditor) fEditor;
		PDESourcePage pdeSrcPage = (PDESourcePage) editor.setActivePage(SimpleCSInputContext.CONTEXT_ID);
		IDocumentProvider dp = pdeSrcPage.getDocumentProvider();
		fDocument = (Document) dp.getDocument(fEditor.getEditorInput());
	}
}
