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
package org.eclipse.pde.api.tools.search.tests;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * General Search test, which contains utility methods, etc for search tests
 * @since 1.0.1
 */
public abstract class SearchTest extends TestCase {
	
	static final String TEST_SOURCE = "test-search";
	static final String BASELINE_DIR_NAME = "baseline";
	static final String BASELINE_NAME = "default_baseline";
	static final String SCOPE_NAME = "default_scope";
	static final IPath TEST_SRC_ROOT = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE);
	static final HashSet<String> DEFAULT_SCOPE_PROJECTS = new HashSet<String>();
	static final String P1_NAME = "a.b.c.P1";
	static final String P2_NAME = "x.y.z.P2";
	static final String P3_NAME = "l.m.n.P3";
	static final String DEFAULT_VERSION = "1.0.0";
	
	static {
		DEFAULT_SCOPE_PROJECTS.add(P1_NAME);
		DEFAULT_SCOPE_PROJECTS.add(P2_NAME);
		DEFAULT_SCOPE_PROJECTS.add(P3_NAME);
	}
	
	/**
	 * Simple test {@link IApiSearchReporter}
	 */
	TestReporter TEST_REPORTER = null;
	
	/**
	 * Simple test {@link IApiSearchRequestor}
	 */
	TestRequestor TEST_REQUESTOR = null;
	
	/**
	 * The default baseline from the '/test-search/baseline' directory
	 */
	IApiBaseline baseline = null;
	
	/**
	 * The default scope from the '/test-search/scope' directory
	 */
	IApiBaseline scope = null;
	
	/**
	 * @return the default test {@link IApiBaseline}
	 */
	protected IApiBaseline getTestBaseline() throws CoreException {
		if(this.baseline == null) {
			this.baseline = createBaseline(BASELINE_NAME, null);
		}
		return this.baseline;
	}
	
	/**
	 * Creates an {@link IApiBaseline} from all jar'd projects listed in the '/test-search/baseline' directory 
	 * @return a new {@link IApiBaseline}
	 */
	IApiBaseline createBaseline(final String name, final HashSet<String> projectnames) throws CoreException {
		File file = TEST_SRC_ROOT.append(BASELINE_DIR_NAME).toFile();
		if(!file.exists()) {
			fail("The baseline test directory must exist");
		}
		IApiBaseline base = ApiModelFactory.newApiBaseline(name, Util.getEEDescriptionFile());
		File[] jars = file.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String name = pathname.getName();
				if(Util.isArchive(name)) {
					if(projectnames != null) {
						IPath path = new Path(pathname.getAbsolutePath());
						return projectnames.contains(path.removeFileExtension().lastSegment());
					}
					else {
						return true;
					}
				}
				return false;
			}
		});
		IApiComponent[] components = new IApiComponent[jars.length];
		for (int i = 0; i < jars.length; i++) {
			components[i] = ApiModelFactory.newApiComponent(base, jars[i].getAbsolutePath());
		}
		base.addApiComponents(components);
		return base;
	}
	
	/**
	 * @return the default test {@link IApiBaseline} (used as a scope)
	 */
	protected IApiBaseline getTestScope(HashSet<String> projectnames) throws CoreException {
		return createBaseline(SCOPE_NAME, projectnames);
	}
	
	/**
	 * @param filename
	 * @return
	 */
	String getExcludeFilePath(String filename) {
		if(filename == null) {
			return null;
		}
		return TEST_SRC_ROOT.append("exclude").append(filename).toOSString();
	}
	
	/**
	 * Returns the populated exclude set or an empty list if the exclude file location is <code>null</code>
	 * @param baseline
	 * @param filename
	 * @return the listing of excluded items
	 */
	protected HashSet<String> getExcludeSet(IApiBaseline baseline, String filename) {
		if(filename == null) {
			return null;
		}
		return new HashSet<String>(Util.initializeRegexExcludeList(getExcludeFilePath(filename), baseline));
	}
	
	/**
	 * reports a failure in the search framework back to the parent test
	 * @param message
	 */
	protected void reportFailure(String message) {
		fail(message);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TEST_REQUESTOR = new TestRequestor(this); 
		TEST_REPORTER = new TestReporter(this);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		if(this.baseline != null) {
			this.baseline.dispose();
		}
		if(this.scope != null) {
			this.scope.dispose();
		}
		super.tearDown();
	}
}
