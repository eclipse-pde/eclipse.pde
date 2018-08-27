/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.eclipse.core.resources.IFolder;
import org.junit.Test;

public class ApiToolingAnalysisAntTaskTests extends AntRunnerTestCase {
	@Override
	public String getTestResourcesFolder() {
		return "apitooling.analysis/"; //$NON-NLS-1$
	}

	@Override
	protected IFolder newTest(String resources) throws Exception {
		return super.newTest(getTestResourcesFolder() + resources);
	}

	@Test
	public void test1() throws Exception {
		IFolder buildFolder = newTest("test1"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists()); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder folder = buildFolder.getFolder("deltatest"); //$NON-NLS-1$
		assertTrue("deltatest folder must exist", folder.exists()); //$NON-NLS-1$
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void test2() throws Exception {
		IFolder buildFolder = newTest("test2"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		try {
			runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
			fail("An exception must occur"); //$NON-NLS-1$
		} catch (Exception e) {
			checkBuildException(e);
		}
	}

	@Test
	public void test3() throws Exception {
		IFolder buildFolder = newTest("test3"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		try {
			runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
			fail("An exception must occur"); //$NON-NLS-1$
		} catch (Exception e) {
			checkBuildException(e);
		}
	}

	@Test
	public void test4() throws Exception {
		IFolder buildFolder = newTest("test4"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("current_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		try {
			runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
			fail("An exception must occur"); //$NON-NLS-1$
		} catch (Exception e) {
			checkBuildException(e);
		}
	}

	/**
	 * Test for with just Exclude list
	 */
	@Test
	public void test5() throws Exception {
		IFolder buildFolder = newTest("test5"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists()); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder folder = buildFolder.getFolder("deltatest"); //$NON-NLS-1$
		assertTrue("deltatest folder must exist", folder.exists()); //$NON-NLS-1$
		folder = buildFolder.getFolder("deltatest1"); //$NON-NLS-1$
		assertTrue("deltatest1 folder must exist", folder.exists()); //$NON-NLS-1$
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Test for with just Include list
	 */
	@Test
	public void test6() throws Exception {
		IFolder buildFolder = newTest("test6"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists()); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder folder = buildFolder.getFolder("deltatest2"); //$NON-NLS-1$
		assertTrue("deltatest2 folder must exist", folder.exists()); //$NON-NLS-1$
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Test for with both Exclude and Include list
	 */
	@Test
	public void test7() throws Exception {
		IFolder buildFolder = newTest("test7"); //$NON-NLS-1$
		String buildXMLPath = buildFolder.getFile("build.xml").getLocation().toOSString(); //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put("reference_location", buildFolder.getFile("before").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("current_location", buildFolder.getFile("after").getLocation().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("report_location", buildFolder.getLocation().toOSString()); //$NON-NLS-1$
		runAntScript(buildXMLPath, new String[] { "run" }, buildFolder.getLocation().toOSString(), properties); //$NON-NLS-1$
		assertFalse("allNonApiBundles must not exist", buildFolder.getFolder("allNonApiBundles").exists()); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder folder = buildFolder.getFolder("deltatest"); //$NON-NLS-1$
		assertTrue("deltatest folder must exist", folder.exists()); //$NON-NLS-1$
		assertTrue("report.xml file must be there", folder.getFile("report.xml").exists()); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
