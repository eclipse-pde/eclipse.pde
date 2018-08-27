/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class BasicXMLTestCase extends XMLModelTestCase {

	public void testReadSimpleExtension() {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load();

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}

	public void testReadSimpleExtensionOneLine() {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, ""); // "" means no newline at the end of file
		load();

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
	}

	public void testReadMultilineSimpleExtension() {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension ");
		sb.append(LF);
		sb.append("point=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("<sample />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		load();

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}

	public void testAddSimpleExtension() throws Exception {
		setXMLContents(null, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 0);

		IExtensionsModelFactory factory = fModel.getFactory();
		IPluginExtension ext = factory.createExtension();
		ext.setPoint("org.eclipse.pde.ui.samples");
		IPluginElement obj = factory.createElement(ext);
		obj.setName("sample");
		ext.add(obj);
		fModel.getPluginBase().add(ext);

		reload();

		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}

	public void testRemoveSimpleExtension() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);

		fModel.getPluginBase().remove(extensions[0]);

		reload();

		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 0);
	}

	// bug 220178
	public void testRemoveChildNode() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		sb.append("<extension id=\"org.eclipse.pde.ui.samples2\"><sample /></extension>");
		setXMLContents(sb, LF);
		load(true);

		IDocumentElementNode parent = (IDocumentElementNode) fModel.getPluginBase();
		IDocumentElementNode child1 = parent.getChildAt(0);
		assertNotNull(child1);
		IDocumentElementNode child2 = parent.getChildAt(1);
		assertNotNull(child2);

		IDocumentElementNode result = parent.removeChildNode(child2);
		assertNotNull(result);
		assertEquals(child2, result);
	}

	//bug 285134
	public void testSingleQuoteAttribute() {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension point='org.eclipse.pde.ui.samples'><sample /></extension>");
		setXMLContents(sb, LF);
		load();

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}

}
