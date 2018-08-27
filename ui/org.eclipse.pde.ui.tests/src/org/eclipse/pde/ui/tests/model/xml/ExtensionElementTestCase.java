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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;

public class ExtensionElementTestCase extends ExtensionTestCase {

	public void testAddNewExtensionElement1LF() throws Exception {
		testAddNewExtensionElement1(LF);
	}

	public void testAddNewExtensionElement2LF() throws Exception {
		testAddNewExtensionElement2(LF);
	}

	public void testAddNewExtensionElement3LF() throws Exception {
		testAddNewExtensionElement3(LF);
	}

	public void testAddNewExtensionElement1CRLF() throws Exception {
		testAddNewExtensionElement1(CRLF);
	}

	public void testAddNewExtensionElement2CRLF() throws Exception {
		testAddNewExtensionElement2(CRLF);
	}

	public void testAddNewExtensionElement3CRLF() throws Exception {
		testAddNewExtensionElement3(CRLF);
	}

	private void testAddNewExtensionElement1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\"></extension>");
		testAddNewExtensionElement(buffer, newLine);
	}

	private void testAddNewExtensionElement2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testAddNewExtensionElement(buffer, newLine);
	}

	private void testAddNewExtensionElement3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("</extension>");
		testAddNewExtensionElement(buffer, newLine);
	}

	public void testAddExtensionElement1LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(LF);
		testAddExtensionElement(buffer, LF);
	}

	public void testAddExtensionElement2LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(LF);
		testAddExtensionElement(buffer, LF);
	}

	public void testAddExtensionElement3LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(LF);
		testAddExtensionElement(buffer, LF);
	}

	public void testAddExtensionElement1CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(CRLF);
		testAddExtensionElement(buffer, CRLF);
	}

	public void testAddExtensionElement2CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(CRLF);
		testAddExtensionElement(buffer, CRLF);
	}

	public void testAddExtensionElement3CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(CRLF);
		testAddExtensionElement(buffer, CRLF);
	}

	private StringBuilder getSingleElementBuffer1(String newLine) throws Exception {
		return new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
	}

	private StringBuilder getSingleElementBuffer2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("<sample />");
		buffer.append(newLine);
		return buffer.append("</extension>");
	}

	private StringBuilder getSingleElementBuffer3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t\t<sample />");
		buffer.append(newLine);
		return buffer.append("\t</extension>");
	}

	public void testAddNewMultipleExtensionElements1LF() throws Exception {
		testAddNewMultipleExtensionElements1(LF);
	}

	public void testAddNewMultipleExtensionElements2LF() throws Exception {
		testAddNewMultipleExtensionElements2(LF);
	}

	public void testAddNewMultipleExtensionElements3LF() throws Exception {
		testAddNewMultipleExtensionElements3(LF);
	}

	public void testAddNewMultipleExtensionElements1CRLF() throws Exception {
		testAddNewMultipleExtensionElements1(CRLF);
	}

	public void testAddNewMultipleExtensionElements2CRLF() throws Exception {
		testAddNewMultipleExtensionElements2(CRLF);
	}

	public void testAddNewMultipleExtensionElements3CRLF() throws Exception {
		testAddNewMultipleExtensionElements3(CRLF);
	}

	private void testAddNewMultipleExtensionElements1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\"></extension>");
		testAddNewMultipleExtensionElements(buffer, newLine);
	}

	private void testAddNewMultipleExtensionElements2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testAddNewMultipleExtensionElements(buffer, newLine);
	}

	private void testAddNewMultipleExtensionElements3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("</extension>");
		testAddNewMultipleExtensionElements(buffer, newLine);
	}

	public void testAddMultipleExtensionElements1LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(LF);
		testAddMultipleExtensionElements(buffer, LF);
	}

	public void testAddMultipleExtensionElements2LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(LF);
		testAddMultipleExtensionElements(buffer, LF);
	}

	public void testAddMultipleExtensionElements3LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(LF);
		testAddMultipleExtensionElements(buffer, LF);
	}

	public void testAddMultipleExtensionElements1CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(CRLF);
		testAddMultipleExtensionElements(buffer, CRLF);
	}

	public void testAddMultipleExtensionElements2CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(CRLF);
		testAddMultipleExtensionElements(buffer, CRLF);
	}

	public void testAddMultipleExtensionElements3CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(CRLF);
		testAddMultipleExtensionElements(buffer, CRLF);
	}

	public void testRemoveExtensionElement1LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(LF);
		testRemoveExtensionElement(buffer, LF);
	}

	public void testRemoveExtensionElement2LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(LF);
		testRemoveExtensionElement(buffer, LF);
	}

	public void testRemoveExtensionElement3LF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(LF);
		testRemoveExtensionElement(buffer, LF);
	}

	public void testRemoveExtensionElement1CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer1(CRLF);
		testRemoveExtensionElement(buffer, CRLF);
	}

	public void testRemoveExtensionElement2CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer2(CRLF);
		testRemoveExtensionElement(buffer, CRLF);
	}

	public void testRemoveExtensionElement3CRLF() throws Exception {
		StringBuilder buffer = getSingleElementBuffer3(CRLF);
		testRemoveExtensionElement(buffer, CRLF);
	}

	public void testRemoveMultipleExtensionElements1LF() throws Exception {
		testRemoveMultipleExtensionElements1(LF);
	}

	public void testRemoveMultipleExtensionElements2LF() throws Exception {
		testRemoveMultipleExtensionElements2(LF);
	}

	public void testRemoveMultipleExtensionElements3LF() throws Exception {
		testRemoveMultipleExtensionElements3(LF);
	}

	public void testRemoveMultipleExtensionElements1CRLF() throws Exception {
		testRemoveMultipleExtensionElements1(CRLF);
	}

	public void testRemoveMultipleExtensionElements2CRLF() throws Exception {
		testRemoveMultipleExtensionElements2(CRLF);
	}

	public void testRemoveMultipleExtensionElements3CRLF() throws Exception {
		testRemoveMultipleExtensionElements3(CRLF);
	}

	private void testRemoveMultipleExtensionElements1(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\"><sample />");
		buffer.append("<sample1/><sample2 /></extension>");
		testRemoveMulitpleExtensionElements(buffer, newLine);
	}

	private void testRemoveMultipleExtensionElements2(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t<sample />");
		buffer.append(newLine);
		buffer.append("\t<sample1/>");
		buffer.append(newLine);
		buffer.append("\t<sample2 />");
		buffer.append(newLine);
		buffer.append("</extension>");
		testRemoveMulitpleExtensionElements(buffer, newLine);
	}

	private void testRemoveMultipleExtensionElements3(String newLine) throws Exception {
		StringBuilder buffer = new StringBuilder("\t<extension point=\"org.eclipse.pde.ui.samples\">");
		buffer.append(newLine);
		buffer.append("\t\t<sample />");
		buffer.append("<sample1/>");
		buffer.append("<sample2 />");
		buffer.append(newLine);
		buffer.append("\t</extension>");
		testRemoveMulitpleExtensionElements(buffer, newLine);
	}

	private void testAddNewExtensionElement(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);

		IPluginExtension ext = extensions[0];
		ext.add(createElement("sample", ext));

		reload();

		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}

	private void testAddExtensionElement(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginExtension ext = extensions[0];
		ext.add(createElement("sample1", ext));

		ext = reloadModel();
		assertEquals(ext.getChildCount(), 2);
		assertEquals(ext.getChildren()[0].getName(), "sample");
	}

	private void testAddNewMultipleExtensionElements(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);

		IPluginExtension ext = extensions[0];
		ext.add(createElement("sample1", ext));
		ext.add(createElement("sample2", ext));

		ext = reloadModel();
		assertEquals(ext.getChildCount(), 2);
		assertEquals(ext.getChildren()[0].getName(), "sample1");
		assertEquals(ext.getChildren()[1].getName(), "sample2");
	}

	private void testAddMultipleExtensionElements(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginExtension ext = extensions[0];

		ext.add(createElement("sample1", ext));
		ext.add(createElement("sample2", ext));

		ext = reloadModel();
		assertEquals(ext.getChildCount(), 3);
		assertEquals(ext.getChildren()[0].getName(), "sample");
		assertEquals(ext.getChildren()[1].getName(), "sample1");
		assertEquals(ext.getChildren()[2].getName(), "sample2");
	}

	private void testRemoveExtensionElement(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginExtension ext = extensions[0];
		assertEquals(ext.getChildCount(), 1);

		ext.remove(ext.getChildren()[0]);

		ext = reloadModel();
		assertEquals(ext.getChildCount(), 0);
	}

	private void testRemoveMulitpleExtensionElements(StringBuilder buffer, String newLine) throws Exception {
		setXMLContents(buffer, newLine);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		IPluginExtension ext = extensions[0];
		assertEquals(ext.getChildCount(), 3);
		assertEquals(ext.getPoint(), "org.eclipse.pde.ui.samples");
		IPluginObject[] children = ext.getChildren();
		assertEquals(children[0].getName(), "sample");
		assertEquals(children[1].getName(), "sample1");
		assertEquals(children[2].getName(), "sample2");

		ext.remove(children[0]);
		ext.remove(children[2]);

		ext = reloadModel();
		assertEquals(ext.getChildCount(), 1);
		assertEquals(ext.getChildren()[0].getName(), "sample1");
	}

	private IPluginElement createElement(String name, IPluginExtension parent) throws CoreException {
		IPluginElement result = fModel.getFactory().createElement(parent);
		result.setName(name);
		return result;
	}

}
