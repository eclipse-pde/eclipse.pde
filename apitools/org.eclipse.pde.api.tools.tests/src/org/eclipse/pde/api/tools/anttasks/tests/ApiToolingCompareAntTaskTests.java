/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.anttasks.tests;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ApiToolingCompareAntTaskTests extends AntRunnerTestCase {

	public String getTestResourcesFolder() {
		return "apitooling.compare/";
	}
	
	public void test1() throws Exception {		
		runTaskAndVerify("test1");	
	}

	private void runTaskAndVerify(String resourceName) throws Exception,
			CoreException, ParserConfigurationException, SAXException,
			IOException {
		
		IFolder buildFolder = newTest(getTestResourcesFolder(), new String[]{resourceName, "profile"});
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("baseline_location", buildFolder.getFile("rcpapp_1.0.0.zip").getLocation().toOSString());
		properties.put("profile_location", buildFolder.getFile("rcpapp_2.0.0.zip").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().append("report").toOSString());
		properties.put("filter_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("report");
		assertTrue("report folder must exist", folder.exists());
		assertTrue("report xml must exist", folder.getFile("compare.xml").exists());
		InputSource is = new InputSource(folder.getFile("compare.xml").getContents());
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(is);
		NodeList elems = doc.getElementsByTagName("delta");
		boolean found = false;
		for (int i = 0; i < elems.getLength(); i++) {
			String value = elems.item(i).getAttributes().getNamedItem("componentId").getNodeValue();
			assertFalse("org.example.rcpintro should have been filtered out.", value.startsWith("org.example.rcpintro"));
			if (value.startsWith("org.example.rcpmail"))
				found = true;
		}
		assertTrue("org.example.rcpmail should be present", found);
	}
	
	public void test2() throws Exception {
		runTaskAndVerify("test2");
	}
	
	public void test3() throws Exception {
		runTaskAndVerify("test3");
	}

}
