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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ApiToolingApiuseAntTaskTests extends AntRunnerTestCase {

	public String getTestResourcesFolder() {
		return "apitooling.apiuse/";
	}
	
	public void test1() throws Exception {		
		IFolder reportFolder = runTaskAndVerify("test1");
		InputSource is = new InputSource(reportFolder.getFile("not_searched.xml").getContents());
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(is);

		NodeList elems = doc.getElementsByTagName("component");
		for (int index = 0; index < elems.getLength(); ++index) {
			String value = elems.item(index).getAttributes().getNamedItem("id").getNodeValue();
			boolean pass = false;
			if (value.startsWith("org.eclipse.osgi"))
				pass = true;
			assertTrue(value + " should have been filtered out.", pass);
		}
	}

	private IFolder runTaskAndVerify(String resourceName) throws Exception,
			CoreException, ParserConfigurationException, SAXException,
			IOException {
		
		IFolder buildFolder = newTest(getTestResourcesFolder(), new String[]{resourceName, "profile"});
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("baseline_location", buildFolder.getFile("OSGiProduct.zip").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().append("report").toOSString());
		properties.put("filter_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder reportFolder = buildFolder.getFolder("report");
		assertTrue("report folder must exist", reportFolder.exists());
		assertTrue("xml folder must exist", reportFolder.exists());
		assertTrue("meta.xml must exist", reportFolder.getFile("meta.xml").exists());
		assertTrue("not_searched.xml must exist", reportFolder.getFile("not_searched.xml").exists());
		return reportFolder;

	}
	
	public void test2() throws Exception {		
		IFolder reportFolder = runTaskAndVerify("test2");
		IResource[] members = reportFolder.members();
		for (int index = 0; index < members.length; index++) {
			if (!members[index].getLocation().toFile().isDirectory())
				continue;
			boolean valid = members[index].getName().startsWith("org.example");
			assertTrue(members[index].getName() + " should have been filtered out", valid);
			File[] dirs = members[index].getLocation().toFile().listFiles();
			for (int i = 0; i < dirs.length; i++) {
				boolean validDir = dirs[i].getName().startsWith("org.example");
				assertTrue(dirs[i].getName() + " should have been filtered out", validDir);
			}
		}
	}
	
	public void test3() throws Exception {	
		IFolder reportFolder = runTaskAndVerify("test3");
		IResource[] members = reportFolder.members();
		for (int index = 0; index < members.length; index++) {
			if (!members[index].getLocation().toFile().isDirectory())
				continue;
			boolean valid = members[index].getName().startsWith("org.example");
			assertTrue(members[index].getName() + " should have been filtered out", valid);
			File[] dirs = members[index].getLocation().toFile().listFiles();
			for (int i = 0; i < dirs.length; i++) {
				boolean validDir = dirs[i].getName().startsWith("org.example");
				assertTrue(dirs[i].getName() + " should have been filtered out", validDir);
			}
		}
	}
}
