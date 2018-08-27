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

public class ExtensionAttributeTestCase extends ExtensionTestCase {

	public void testAddNewExtensionAttribute1LF() throws Exception {
		testAddNewExtensionAttribute1(LF);
	}

	public void testAddNewExtensionAttribute2LF() throws Exception {
		testAddNewExtensionAttribute2(LF);
	}

	public void testAddNewExtensionAttribute3LF() throws Exception {
		testAddNewExtensionAttribute3(LF);
	}

	public void testAddNewExtensionAttribute1CRLF() throws Exception {
		testAddNewExtensionAttribute1(CRLF);
	}

	public void testAddNewExtensionAttribute2CRLF() throws Exception {
		testAddNewExtensionAttribute2(CRLF);
	}

	public void testAddNewExtensionAttribute3CRLF() throws Exception {
		testAddNewExtensionAttribute3(CRLF);
	}

	private void testAddNewExtensionAttribute1(String newLine) throws Exception {
		StringBuilder sb = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		testAddNewExtensionAttribute(sb, newLine);
	}

	private void testAddNewExtensionAttribute2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample />");
		buffer.append(newLine);
		buffer.append("</extension>");
		testAddNewExtensionAttribute(buffer, newLine);
	}

	private void testAddNewExtensionAttribute3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample />");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testAddNewExtensionAttribute(buffer, newLine);
	}

	public void testAddExtensionAttribute1LF() throws Exception {
		testAddExtensionAttribute1(LF);
	}

	public void testAddExtensionAttribute2LF() throws Exception {
		testAddExtensionAttribute2(LF);
	}

	public void testAddExtensionAttribute3LF() throws Exception {
		testAddExtensionAttribute3(LF);
	}

	public void testAddExtensionAttribute1CRLF() throws Exception {
		testAddExtensionAttribute1(CRLF);
	}

	public void testAddExtensionAttribute2CRLF() throws Exception {
		testAddExtensionAttribute2(CRLF);
	}

	public void testAddExtensionAttribute3CRLF() throws Exception {
		testAddExtensionAttribute3(CRLF);
	}

	private void testAddExtensionAttribute1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append("<sample id=\"org.eclipse.pde.sample1\"/></extension>");
		testAddExtensionAttribute(buffer, newLine);
	}

	private void testAddExtensionAttribute2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample id=\"org.eclipse.pde.sample1\"/>");
		buffer.append(newLine);
		buffer.append("</extension>");
		testAddExtensionAttribute(buffer, newLine);
	}

	private void testAddExtensionAttribute3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample id=\"org.eclipse.pde.sample1\"/>");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testAddExtensionAttribute(buffer, newLine);
	}

	public void testAddNewMultipleAttributes1LF() throws Exception {
		testAddNewMultipleAttributes1(LF);
	}

	public void testAddNewMultipleAttributes2LF() throws Exception {
		testAddNewMultipleAttributes2(LF);
	}

	public void testAddNewMultipleAttributes3LF() throws Exception {
		testAddNewMultipleAttributes3(LF);
	}

	public void testAddNewMultipleAttributes1CRLF() throws Exception {
		testAddNewMultipleAttributes1(CRLF);
	}

	public void testAddNewMultipleAttributes2CRLF() throws Exception {
		testAddNewMultipleAttributes2(CRLF);
	}

	public void testAddNewMultipleAttributes3CRLF() throws Exception {
		testAddNewMultipleAttributes3(CRLF);
	}

	private void testAddNewMultipleAttributes1(String newLine) throws Exception {
		StringBuilder sb = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		testAddNewMultipleExtensionAttributes(sb, newLine);
	}

	private void testAddNewMultipleAttributes2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample />");
		buffer.append(newLine);
		buffer.append("</extension>");
		testAddNewMultipleExtensionAttributes(buffer, newLine);
	}

	private void testAddNewMultipleAttributes3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample />");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testAddNewMultipleExtensionAttributes(buffer, newLine);
	}

	public void testRemoveExtensionAttribute1LF() throws Exception {
		testRemoveExtensionAttribute1(LF);
	}

	public void testRemoveExtensionAttribute2LF() throws Exception {
		testRemoveExtensionAttribute2(LF);
	}

	public void testRemoveExtensionAttribute3LF() throws Exception {
		testRemoveExtensionAttribute3(LF);
	}

	public void testRemoveExtensionAttribute1CRLF() throws Exception {
		testRemoveExtensionAttribute1(CRLF);
	}

	public void testRemoveExtensionAttribute2CRLF() throws Exception {
		testRemoveExtensionAttribute2(CRLF);
	}

	public void testRemoveExtensionAttribute3CRLF() throws Exception {
		testRemoveExtensionAttribute3(CRLF);
	}

	private void testRemoveExtensionAttribute1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">\t");
		buffer.append("<sample id=\"org.eclipse.pde.sample1\"/>\t</extension>");
		testRemoveExtensionAttribute(buffer, newLine);
	}

	private void testRemoveExtensionAttribute2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample id=\"org.eclipse.pde.sample1\"/>");
		buffer.append(newLine);
		buffer.append("</extension>");
		testRemoveExtensionAttribute(buffer, newLine);
	}

	private void testRemoveExtensionAttribute3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample id=\"org.eclipse.pde.sample1\"/>");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testRemoveExtensionAttribute(buffer, newLine);
	}

	public void testRemoveMultipleExtensionAttributes1LF() throws Exception {
		testRemoveMultipleExtensionAttributes1(LF);
	}

	public void testRemoveMultipleExtensionAttributes2LF() throws Exception {
		testRemoveMultipleExtensionAttributes2(LF);
	}

	public void testRemoveMultipleExtensionAttributes3LF() throws Exception {
		testRemoveMultipleExtensionAttributes3(LF);
	}

	public void testRemoveMultipleExtensionAttributes1CRLF() throws Exception {
		testRemoveMultipleExtensionAttributes1(CRLF);
	}

	public void testRemoveMultipleExtensionAttributes2CRLF() throws Exception {
		testRemoveMultipleExtensionAttributes2(CRLF);
	}

	public void testRemoveMultipleExtensionAttributes3CRLF() throws Exception {
		testRemoveMultipleExtensionAttributes3(CRLF);
	}

	private void testRemoveMultipleExtensionAttributes1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">\t");
		buffer.append("<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/></extension>");
		testRemoveMultipleExtensionAttributes(buffer, newLine);
	}

	private void testRemoveMultipleExtensionAttributes2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/>");
		buffer.append(newLine);
		buffer.append("</extension>");
		testRemoveMultipleExtensionAttributes(buffer, newLine);
	}

	private void testRemoveMultipleExtensionAttributes3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/>");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testRemoveMultipleExtensionAttributes(buffer, newLine);
	}

	public void testChangeExtensionAttribute1LF() throws Exception {
		testChangeExtensionAttribute1(LF);
	}

	public void testChangeExtensionAttribute2LF() throws Exception {
		testChangeExtensionAttribute2(LF);
	}

	public void testChangeExtensionAttribute3LF() throws Exception {
		testChangeExtensionAttribute3(LF);
	}

	public void testChangeExtensionAttribute1CRLF() throws Exception {
		testChangeExtensionAttribute1(CRLF);
	}

	public void testChangeExtensionAttribute2CRLF() throws Exception {
		testChangeExtensionAttribute2(CRLF);
	}

	public void testChangeExtensionAttribute3CRLF() throws Exception {
		testChangeExtensionAttribute3(CRLF);
	}

	private void testChangeExtensionAttribute1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append("<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/></extension>");
		testChangeExtensionAttribute(buffer, newLine);
	}

	private void testChangeExtensionAttribute2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/>");
		buffer.append(newLine);
		buffer.append("</extension>");
		testChangeExtensionAttribute(buffer, newLine);
	}

	private void testChangeExtensionAttribute3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t\t\t<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/>");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testChangeExtensionAttribute(buffer, newLine);
	}

	private void testAddNewExtensionAttribute(StringBuilder buffer, String newLine) throws Exception {
		StringBuilder sb = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];

		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		elem.setAttribute("id", "org.eclipse.pde.sample1");

		IPluginExtension ext = reloadModel();
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
	}

	private void testAddExtensionAttribute(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];

		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("name", "pde sample");

		IPluginExtension ext = reloadModel();

		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 2);
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}

	private void testAddNewMultipleExtensionAttributes(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		elem.setAttribute("id", "org.eclipse.pde.sample1");
		elem.setAttribute("name", "pde sample");

		IPluginExtension ext = reloadModel();

		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}

	private void testRemoveExtensionAttribute(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];

		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", null);

		IPluginExtension ext = reloadModel();

		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 0);
	}

	private void testRemoveMultipleExtensionAttributes(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		assertTrue(child instanceof IPluginElement);
		assertEquals(((IPluginElement) child).getAttributeCount(), 3);

		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", null);
		elem.setAttribute("perspectiveId", null);

		IPluginExtension ext = reloadModel();

		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 1);
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}

	private void testChangeExtensionAttribute(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", "org.eclipse.pde.sample2");

		IPluginExtension ext = reloadModel();

		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 3);
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample2");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
		assertEquals(elem.getAttribute("perspectiveId").getValue(), "org.eclipse.pde.ui.PDEPerspective");
	}
}
