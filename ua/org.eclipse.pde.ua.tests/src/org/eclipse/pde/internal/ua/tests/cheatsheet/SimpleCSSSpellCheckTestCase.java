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
package org.eclipse.pde.internal.ua.tests.cheatsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.*;
import java.util.Iterator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.junit.Before;

/**
 * Tests the spelling annotations in a Simple Cheat sheet editor.
 *
 */
public class SimpleCSSSpellCheckTestCase extends AbstractCheatSheetModelTestCase {

	private IEditorPart fEditor;
	private IProject fProject;
	private static final String EDITOR_ID = "org.eclipse.pde.ua.ui.simpleCheatSheetEditor";

	@Before
	public void setUpLocal() throws Exception {

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
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Bodi\"</b>"); //The spelling error shall appear here
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 97, 2, 1);
	}

	/**
	 * Testing for the spelling error annotation being present at the right location
	 * in a string with single quotes
	 */
	public void testSpellingErrorInSingleQuotedStringTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>'Bodi'</b>"); //The spelling error shall appear here
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 97, 2, 1);

	}

	/**
	 * Testing for the spelling error annotation being present at the right location
	 * in a double quoted string containing single quotes
	 */
	public void testSpellingErrorInDoubleQuotedStringWithSingleQuotesTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"'single quoted string' Bodi\"</b>"); //The spelling error shall appear here
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 120, 2, 1);

	}

	/**
	 * Testing for the spelling error annotation being present at the right location
	 * in a single quoted string containing double quotes
	 */
	public void testSpellingErrorInDoubleQuotedStringWithDoubleQuotesTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>'Bodi\" is not a correct spelling\"'</b>"); //The spelling error shall appear here
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 97, 2, 1);

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
		Iterator<Annotation> iter = model.getAnnotationIterator();
		int actualTotalAnnotationCount = 0;
		int actualSpellingAnnotationCount = 0;
		while (iter.hasNext()) {
			actualTotalAnnotationCount++;
			Annotation annotation = iter.next();
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
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}

	/**
	 * Testing for the multiple spelling error annotations
	 */
	public void testMultipleSpellingErrorsTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Tital\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Bodi\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Itim\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 4, 3);
	}

	/**
	 * Testing for the zero spelling error annotations
	 */
	public void testZeroSpellingErrorsTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText,"<item");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}

	/**
	 * The spelling errors in the XML comments should be ignored
	 */
	public void testSpellingErrorsInXMLCommentTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText, "<!-- Itm One -->");
		append(csText,"<item ");
		append(csText,"href=\"/org.eclipse.pde/about.html\" ");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}

	/**
	 * The spelling errors in the multiple line XML comments should be ignored
	 */
	public void testSpellingErrorsInMultiLineXMLCommentTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText, "<!-- Itm One ");
		append(csText, " commnt with spell error continues");
		append(csText, " comment ends --->");
		append(csText,"<item ");
		append(csText,"href=\"/org.eclipse.pde/about.html\" ");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}

	/**
	 * Testing for no unwanted spelling annotations due to single quote in XML comment
	 */
	public void testSingleQuoteInXMLCommentTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText, "<!-- Item's One -->"); // single quote in XML Comment
		append(csText,"<item ");
		append(csText,"href=\"/org.eclipse.pde/about.html\" ");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

		validateAnnotations(csText.toString(), 0, 1, 0);
	}

	/**
	 * Testing for no unwanted spelling annotations due to double quote in XML comment
	 */
	public void testDoubleQuoteInXMLCommentTestCase() {
		StringBuilder csText = new StringBuilder();

		append(csText,"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		append(csText,"<cheatsheet");
		append(csText,"title=\"Title\">");
		append(csText,"<intro>");
		append(csText,"<description>");
		append(csText,"<b>\"Body\"</b>");
		append(csText,"</description>");
		append(csText,"</intro>");
		append(csText, "<!-- Item\"s One -->"); // double quote in XML Comment
		append(csText,"<item ");
		append(csText,"href=\"/org.eclipse.pde/about.html\" ");
		append(csText,"title=\"Item\">");
		append(csText,"<description>");
		append(csText,"<b>Body</b>");
		append(csText,"</description>");
		append(csText,"</item>");
		append(csText,"</cheatsheet>");

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

		try (FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				BufferedWriter bw = new BufferedWriter(osw)) {
			bw.write(fileContents);
			bw.flush();
		}

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
		fDocument = dp.getDocument(fEditor.getEditorInput());
	}

	private void append(StringBuilder buffer, String text) {
		buffer.append(text + CRLF);
	}
}
