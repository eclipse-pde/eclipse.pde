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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ApiToolingApiFreezeAntTaskTests extends AntRunnerTestCase {

	@Override
	public String getTestResourcesFolder() {
		return "apitooling.apifreeze/"; //$NON-NLS-1$
	}

	@Test
	public void test1() throws Exception {
		runTaskAndVerify("test1"); //$NON-NLS-1$
	}

	private void runTaskAndVerify(String resourceName) throws Exception, CoreException, ParserConfigurationException, SAXException, IOException {

		IFolder buildFolder = newTest(getTestResourcesFolder(), new String[] {
				resourceName, "profile" }); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("baseline_location", buildFolder.getFile("rcpapp_1.0.0.zip").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("profile_location", buildFolder.getFile("rcpapp_2.0.0.zip").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().append("report").toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("filter_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists()); //$NON-NLS-1$ //$NON-NLS-2$
		IFile reportFile = buildFolder.getFile("report.xml"); //$NON-NLS-1$
		assertTrue("report.xml must exist", reportFile.exists()); //$NON-NLS-1$
		InputSource is = new InputSource(reportFile.getContents());
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(is);
		NodeList elems = doc.getElementsByTagName("delta"); //$NON-NLS-1$
		boolean found = false;
		for (int i = 0; i < elems.getLength(); i++) {
			Node node = elems.item(i).getAttributes().getNamedItem("componentId"); //$NON-NLS-1$
			assertFalse("org.example.rcpintro should have been filtered out.", node.getNodeValue().startsWith("org.example.rcpintro")); //$NON-NLS-1$ //$NON-NLS-2$
			if (node.getNodeValue().startsWith("org.example.rcpmail")) { //$NON-NLS-1$
				found = true;
			}
		}
		assertTrue("org.example.rcpmail", found); //$NON-NLS-1$
	}

	@Test
	public void test2() throws Exception {
		runTaskAndVerify("test2"); //$NON-NLS-1$
	}

	@Test
	public void test3() throws Exception {
		runTaskAndVerify("test3"); //$NON-NLS-1$
	}

	@Test
	public void test4() throws Exception {
		try {
			runTaskAndVerify("test4"); //$NON-NLS-1$
			fail("Should not be there"); //$NON-NLS-1$
		} catch (Exception e) {
			assertEquals("Wrong exception", "org.apache.tools.ant.BuildException", e.getClass().getCanonicalName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
