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

public class SwapXMLModelTestCase extends XMLModelTestCase {

	// all one one line
	public void testSwapTwoChildren() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("<child id=\"a\" /><child id=\"b\" />");
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}

	// all on diff line
	public void testSwapTwoChildren2() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("<child id=\"a\" />");
		sb.append(LF);
		sb.append("<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}

	// all on diff line with tabs
	public void testSwapTwoChildren3() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("\t<child id=\"a\" />");
		sb.append(LF);
		sb.append("\t<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}

	// some on diff lines with no spacing
	public void testSwapTwoChildren4() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("<child id=\"a\" />");
		sb.append(LF);
		sb.append("<child id=\"b\" />");
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}

	// some on diff lines with spacing
	public void testSwapTwoChildren5() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("\t<child id=\"a\" />");
		sb.append(LF);
		sb.append("\t<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}

	private void twoChildSwap() throws Exception {
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		IPluginObject[] children = extensions[0].getChildren();

		assertEquals(2, children.length);
		assertTrue(children[0] instanceof IPluginElement);
		assertTrue(children[1] instanceof IPluginElement);
		assertEquals("a", ((IPluginElement) children[0]).getAttribute("id").getValue());
		assertEquals("b", ((IPluginElement) children[1]).getAttribute("id").getValue());

		extensions[0].swap(children[0], children[1]);

		// move source edit - only one op
		reload();

		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);

		children = extensions[0].getChildren();

		assertEquals(2, children.length);
		assertTrue(children[0] instanceof IPluginElement);
		assertTrue(children[1] instanceof IPluginElement);
		assertEquals("b", ((IPluginElement) children[0]).getAttribute("id").getValue());
		assertEquals("a", ((IPluginElement) children[1]).getAttribute("id").getValue());
	}
}
