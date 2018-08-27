/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.search.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.XmlSearchReporter;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.After;
import org.junit.Test;

/**
 * Tests the API use specific implementations of {@link IApiSearchReporter}
 * and {@link IApiSearchRequestor}
 *
 * @since 1.0.1
 */
public class UseSearchTests extends SearchTest {

	static IPath TMP_PATH = TestSuiteHelper.getUserDirectoryPath().append("use-search-tests"); //$NON-NLS-1$
	static IPath XML_PATH = TMP_PATH.append("xml"); //$NON-NLS-1$
	static IPath HTML_PATH = TMP_PATH.append("html"); //$NON-NLS-1$
	final HashMap<String, HashSet<String>> usedprojects = new HashMap<>();

	@Override
	@After
	public void tearDown() throws Exception {
		scrubReportLocation(TMP_PATH.toFile());
		super.tearDown();
	}

	/**
	 * Asserts the the report was created with the correct folder structure
	 * @param reportroot
	 */
	private void assertXMLReport(IPath reportroot){
		File root = reportroot.toFile();
		assertTrue("the report root must exist", root.exists()); //$NON-NLS-1$
		File[] files = root.listFiles((FileFilter) pathname -> pathname.isDirectory());
		int flength = files.length;
		int epsize = this.usedprojects.size();
		assertEquals("the used project roots must be the same as we are expecting", flength, epsize); //$NON-NLS-1$
		assertFalse("The files list should be not be greater than the expected used projects", flength > epsize); //$NON-NLS-1$
		assertFalse("The files list should be not be less than the expected used projects", flength < epsize); //$NON-NLS-1$
		HashSet<String> names = null;
		File[] projects = null;
		for (int i = 0; i < files.length; i++) {
			names = this.usedprojects.get(files[i].getName());
			assertNotNull("the expeced set of using project names should exist", names); //$NON-NLS-1$
			projects = files[i].listFiles();
			assertTrue("the only files should be the folders for the projects using ["+files[i].getName()+"]", projects.length == names.size()); //$NON-NLS-1$ //$NON-NLS-2$
			for (File project : projects) {
				if (!project.isDirectory()) {
					reportFailure("Unexpected non-folder entry found: [" + project.getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (!names.remove(project.getName())) {
					reportFailure("Unexpected folder entry in the report location: [" + project.getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			assertTrue("All of the using projects should have been detected", names.isEmpty()); //$NON-NLS-1$
		}
	}

	/**
	 * Cleans the location if it exists
	 * @param file
	 */
	private void scrubReportLocation(File file) {
		if(file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()) {
					scrubReportLocation(files[i]);
				}
				else {
					files[i].delete();
				}
			}
			file.delete();
		}
	}

	/**
	 * Sets the projects that we are expecting to be using the given project
	 * @param project the projects we are saying should be used
	 * @param projects the project that we are expecting to be using the given project(s)
	 */
	void setProjectsUsedBy(String[] project, String[][] projects) {
		if(projects == null || project == null) {
			this.usedprojects.clear();
		}
		else {
			HashSet<String> names = null;
			for (int i = 0; i < projects.length; i++) {
				names = new HashSet<>(projects[i].length);
				for (int j = 0; j < projects[i].length; j++) {
					names.add(projects[i][j]);
				}
				this.usedprojects.put(project[i], names);
			}
		}
	}

	/**
	 * Returns the default composite search reporter which has the {@link TestReporter}
	 * and the {@link XMLApiSearchReporter} in it
	 * @param path
	 * @param debug
	 * @return the default composite search reporter
	 */
	IApiSearchReporter getCompositeReporter(String path, boolean debug) {
		IApiSearchReporter[] reporters = new IApiSearchReporter[2];
		reporters[0] = TEST_REPORTER;
		reporters[1] = new XmlSearchReporter(XML_PATH.toOSString(), debug);
		return new TestCompositeSearchReporter(this, reporters);
	}

	/**
	 * Tests that the XML reporter is generating XMl files at the correct
	 * location, with no components excluded from the search
	 */
	@Test
	public void testSearchXmlReporterNoExclusions() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestBaseline());
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME},
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, getCompositeReporter(XML_PATH.toOSString(), false), null);
			setProjectsUsedBy(
					new String[] {getProjectId(P1_NAME, DEFAULT_VERSION), getProjectId(P2_NAME, DEFAULT_VERSION)},
					new String[][] {{getProjectId(P2_NAME, DEFAULT_VERSION), getProjectId(P3_NAME, DEFAULT_VERSION)}, {getProjectId(P3_NAME, DEFAULT_VERSION)}});
			assertXMLReport(XML_PATH);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.toString()); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that the XML reporter is generating XMl files at the correct location,
	 * with no components excluded from the search
	 */
	@Test
	public void testSearchXmlReporterNoExclusionsWithDebug() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestBaseline());
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME},
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, getCompositeReporter(XML_PATH.toOSString(), true), null);
			setProjectsUsedBy(
					new String[] {getProjectId(P1_NAME, DEFAULT_VERSION), getProjectId(P2_NAME, DEFAULT_VERSION)},
					new String[][] {{getProjectId(P2_NAME, DEFAULT_VERSION), getProjectId(P3_NAME, DEFAULT_VERSION)}, {getProjectId(P3_NAME, DEFAULT_VERSION)}});
			assertXMLReport(XML_PATH);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.toString()); //$NON-NLS-1$
		}
	}

	String getProjectId(String project, String version) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(project).append(" ").append('(').append(version).append(')'); //$NON-NLS-1$
		return buffer.toString();
	}
}
