/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class StructureXMLModelTestCase extends XMLModelTestCase {

	// Plugin#write(String indent, PrintWriter writer)
	// ~ line 99
	static String FIRST_INDENT = "   "; //$NON-NLS-1$
	// PluginElement#ATTRIBUTE_SHIFT
	// ~ line 35
	static String ATTRIBUTE_SHIFT = "      "; //$NON-NLS-1$
	// PluginElement#ELEMENT_SHIFT
	// ~ line 37
	static String ELEMENT_SHIFT = "   "; //$NON-NLS-1$

	public void testStructureAddExtensionLF() throws Exception {
		addExtension(LF);
	}

	public void testStructureAddExtensionCRLF() throws Exception {
		addExtension(CRLF);
	}

	public void testStructureAddElementLF() throws Exception {
		addElement(LF);
	}

	public void testStructureAddElementCRLF() throws Exception {
		addElement(CRLF);
	}

	public void testStructureAddAttributeLF() throws Exception {
		addAttributesToElement(LF);
	}

	public void testStructureAddAttributeCRLF() throws Exception {
		addAttributesToElement(CRLF);
	}

	public void testStructureBreakOpenElementLF() throws Exception {
		breakOpenElement(LF);
	}

	public void testStructureBreakOpenElementCRLF() throws Exception {
		breakOpenElement(CRLF);
	}

	public void testStructurePreserveCommentInRootLF() throws Exception {
		preserveCommentAddExtension(LF);
	}

	public void testStructurePreserveCommentInRootCRLF() throws Exception {
		preserveCommentAddExtension(CRLF);
	}

	public void testStructurePreserveCommentInExtensionLF() throws Exception {
		preserveContainedCommentAddElement(LF);
	}

	public void testStructurePreserveCommentInExtensionCRLF() throws Exception {
		preserveContainedCommentAddElement(CRLF);
	}

	private void addExtension(String lineDelim) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension ext = fModel.getFactory().createExtension();
		ext.setPoint("org.eclipse.pde2");
		fModel.getPluginBase().add(ext);

		reload();

		String newContents = fDocument.get();
		sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde2\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		assertEquals(fDocument.get(), newContents);
	}

	private void addElement(String lineDelim) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		IPluginElement element = fModel.getFactory().createElement(extensions[0]);
		element.setName("child");
		extensions[0].add(element);

		reload();

		String newContents = fDocument.get();
		sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<child></child>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		assertEquals(fDocument.get(), newContents);
	}

	private void addAttributesToElement(String lineDelim) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<child/>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		IPluginObject[] children = extensions[0].getChildren();

		assertEquals(1, children.length);
		assertTrue(children[0] instanceof IPluginElement);

		((IPluginElement) children[0]).setAttribute("id", "a");
		((IPluginElement) children[0]).setAttribute("name", "test");

		reload();

		String newContents = fDocument.get();
		sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<child");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("id=\"a\"");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("name=\"test\"/>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		assertEquals(fDocument.get(), newContents);
	}

	private void breakOpenElement(String lineDelim) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<child/>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		IPluginObject[] children = extensions[0].getChildren();
		assertEquals(1, children.length);
		assertTrue(children[0] instanceof IPluginElement);

		IPluginElement element = fModel.getFactory().createElement(children[0]);
		element.setName("subchild");
		((IPluginElement) children[0]).add(element);

		reload();

		String newContents = fDocument.get();
		sb = new StringBuilder();
		sb.append(FIRST_INDENT);
		sb.append("<extension");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ATTRIBUTE_SHIFT);
		sb.append("point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<child>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append(ELEMENT_SHIFT);
		sb.append("<subchild></subchild>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append(ELEMENT_SHIFT);
		sb.append("</child>");
		sb.append(lineDelim);
		sb.append(FIRST_INDENT);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		assertEquals(fDocument.get(), newContents);
	}

	private void preserveCommentAddExtension(String lineDelim) throws Exception {
		String comment = "<!-- THIS IS A COMMENT -->";
		StringBuilder sb = new StringBuilder(comment);
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension ext = fModel.getFactory().createExtension();
		ext.setPoint("org.eclipse.pde");
		fModel.getPluginBase().add(ext);

		reload();

		int commentIndex = fDocument.get().indexOf(comment);
		assertTrue(commentIndex != -1);
		IDocumentElementNode parent = (IDocumentElementNode) fModel.getPluginBase();
		assertTrue(commentIndex >= parent.getOffset());
		assertTrue(commentIndex + comment.length() <= parent.getOffset() + parent.getLength());
	}

	private void preserveContainedCommentAddElement(String lineDelim) throws Exception {
		String comment = "<!-- THIS IS A COMMENT INSIDE THE EXTENSION -->";
		StringBuilder sb = new StringBuilder();
		sb.append("<extension point=\"org.eclipse.pde\">");
		sb.append(lineDelim);
		sb.append(comment);
		sb.append(lineDelim);
		sb.append("</extension>");
		setXMLContents(sb, lineDelim);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		IPluginElement element = fModel.getFactory().createElement(extensions[0]);
		element.setName("element");
		extensions[0].add(element);

		reload();

		int commentIndex = fDocument.get().indexOf(comment);
		assertTrue(commentIndex != -1);
		IDocumentElementNode parent = (IDocumentElementNode) fModel.getPluginBase();
		assertTrue(commentIndex >= parent.getOffset());
		assertTrue(commentIndex + comment.length() <= parent.getOffset() + parent.getLength());
	}
}
