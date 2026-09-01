/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.anttasks.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.internal.runtime.XmlProcessorFactory;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class ApiToolingApiuseAntTaskTests extends AntRunnerTestCase {

	@Override
	public String getTestResourcesFolder() {
		return "apitooling.apiuse/"; //$NON-NLS-1$
	}

	private IFolder runTaskAndVerify(String resourceName) throws Exception, CoreException, ParserConfigurationException, SAXException, IOException {

		IFolder buildFolder = newTest(getTestResourcesFolder(), new String[] {
				resourceName, "profile" }); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("baseline_location", buildFolder.getFile("OSGiProduct.zip").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().append("report").toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("filter_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse(buildFolder.getFolder("allNonApiBundles").exists(), "allNonApiBundles must not exist"); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder reportFolder = buildFolder.getFolder("report"); //$NON-NLS-1$
		assertTrue(reportFolder.exists(), "report folder must exist"); //$NON-NLS-1$
		assertTrue(reportFolder.exists(), "xml folder must exist"); //$NON-NLS-1$
		assertTrue(reportFolder.getFile("meta.xml").exists(), "meta.xml must exist"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(reportFolder.getFile("not_searched.xml").exists(), "not_searched.xml must exist"); //$NON-NLS-1$ //$NON-NLS-2$
		return reportFolder;

	}

	@Test
	public void test1() throws Exception {
		IFolder reportFolder = runTaskAndVerify("test1"); //$NON-NLS-1$
		InputSource is = new InputSource(reportFolder.getFile("not_searched.xml").getContents()); //$NON-NLS-1$
		DocumentBuilder db = XmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
		Document doc = db.parse(is);

		NodeList elems = doc.getElementsByTagName("component"); //$NON-NLS-1$
		for (int index = 0; index < elems.getLength(); ++index) {
			String value = elems.item(index).getAttributes().getNamedItem("id").getNodeValue(); //$NON-NLS-1$
			boolean pass = false;
			if (value.startsWith("org.eclipse.osgi") || value.contains("illegaluse") || value.contains("oldstyle")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pass = true;
			}
			assertTrue(pass, value + " should have been filtered out."); //$NON-NLS-1$
		}
	}

	@Test
	public void test2() throws Exception {
		IFolder reportFolder = runTaskAndVerify("test2"); //$NON-NLS-1$
		IResource[] members = reportFolder.members();
		boolean valid = false;
		boolean validDir = false;
		for (IResource member : members) {
			if (!member.getLocation().toFile().isDirectory()) {
				continue;
			}
			valid = member.getName().startsWith("org.example"); //$NON-NLS-1$
			assertTrue(valid, member.getName() + " should have been filtered out"); //$NON-NLS-1$
			File[] dirs = member.getLocation().toFile().listFiles();
			for (File dir : dirs) {
				validDir = dir.getName().startsWith("org.example"); //$NON-NLS-1$
				assertTrue(validDir, dir.getName() + " should have been filtered out"); //$NON-NLS-1$
			}
		}
		assertTrue(valid, "None of the example plug-ins were scanned"); //$NON-NLS-1$
		assertTrue(validDir, "None of the example plug-ins were scanned"); //$NON-NLS-1$
	}

	@Test
	public void test3() throws Exception {
		IFolder reportFolder = runTaskAndVerify("test3"); //$NON-NLS-1$
		IResource[] members = reportFolder.members();
		boolean valid = false;
		boolean validDir = false;
		for (IResource member : members) {
			if (!member.getLocation().toFile().isDirectory()) {
				continue;
			}
			valid = member.getName().startsWith("org.example"); //$NON-NLS-1$
			assertTrue(valid, member.getName() + " should have been filtered out"); //$NON-NLS-1$
			File[] dirs = member.getLocation().toFile().listFiles();
			for (File dir : dirs) {
				validDir = dir.getName().startsWith("org.example"); //$NON-NLS-1$
				assertTrue(validDir, dir.getName() + " should have been filtered out"); //$NON-NLS-1$
			}
		}
		assertTrue(valid, "None of the example plug-ins were scanned"); //$NON-NLS-1$
		assertTrue(validDir, "None of the example plug-ins were scanned"); //$NON-NLS-1$
	}

	/**
	 * Tests that a use scan will find illegal use problems that can be filtered
	 */
	@Test
	public void testIllegalUse() throws Exception {
		IFolder reportFolder = runTaskAndVerify("testIllegalUse"); //$NON-NLS-1$
		IResource[] members = reportFolder.members();
		boolean valid = false;
		boolean validDir = false;
		for (IResource member : members) {
			if (!member.getLocation().toFile().isDirectory()) {
				continue;
			}
			valid = member.getName().startsWith("org.eclipse.osgi"); //$NON-NLS-1$
			assertTrue(valid, member.getName() + " should have been filtered out"); //$NON-NLS-1$
			File[] dirs = member.getLocation().toFile().listFiles();
			for (File dir : dirs) {
				validDir = dir.getName().startsWith("org.example.test.illegaluse"); //$NON-NLS-1$
				assertTrue(validDir, dir.getName() + " should have been filtered out"); //$NON-NLS-1$
			}
		}
		// This test is not working properly, see Bug 405302
		// assertTrue("The illegal use plug-in was not scanned", valid);
		// assertTrue("The illegal use plug-in was not scanned", validDir);
	}

	/**
	 * Tests that a use scan will find illegal use problems that can be filtered
	 */
	@Test
	public void testIllegalUseFiltered() throws Exception {
		IFolder reportFolder = runTaskAndVerify("testIllegalUseFiltered"); //$NON-NLS-1$
		IResource[] members = reportFolder.members();
		for (IResource member : members) {
			if (member.getLocation().toFile().isDirectory()) {
				fail(member.getName() + " should have been filtered using a .api_filters file"); //$NON-NLS-1$
			}
		}
	}

}
