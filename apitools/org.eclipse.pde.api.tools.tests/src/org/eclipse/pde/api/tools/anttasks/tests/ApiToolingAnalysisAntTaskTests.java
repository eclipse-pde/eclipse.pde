/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.anttasks.tests;

import java.util.Properties;

import org.eclipse.core.resources.IFolder;

public class ApiToolingAnalysisAntTaskTests extends AntRunnerTestCase {

	private static final String BUILD_EXCEPTION_CLASS_NAME = "org.apache.tools.ant.BuildException";

	public String getTestResourcesFolder() {
		return "apitooling.analysis/";
	}
	protected IFolder newTest(String resources) throws Exception {
		return super.newTest(getTestResourcesFolder() + resources);
	}

	public void test1() throws Exception {
		IFolder buildFolder = newTest("test1");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists());
		IFolder folder = buildFolder.getFolder("deltatest");
		assertTrue("deltatest folder must exist", folder.exists());
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists());
	}
	public void test2() throws Exception {
		IFolder buildFolder = newTest("test2");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			assertEquals("Wrong exception", BUILD_EXCEPTION_CLASS_NAME, e.getClass().getCanonicalName());
		}
	}
	public void test3() throws Exception {
		IFolder buildFolder = newTest("test3");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			assertEquals("Wrong exception", BUILD_EXCEPTION_CLASS_NAME, e.getClass().getCanonicalName());
		}
	}
	public void test4() throws Exception {
		IFolder buildFolder = newTest("test4");
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString();
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString());
		properties.put("report_location", buildFolder.getLocation().toOSString());
		try {
			runAntScript(buildXMLPath, new String[] {"run"}, buildFolder.getLocation().toOSString(), properties);
			assertFalse("An exception must occur", true);
		} catch (Exception e) {
			assertEquals("Wrong exception", BUILD_EXCEPTION_CLASS_NAME, e.getClass().getCanonicalName());
		}
	}
}
