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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.After;
import org.junit.Before;

/**
 * General Search test, which contains utility methods, etc for search tests
 *
 * @since 1.0.1
 */
public abstract class SearchTest {

	static final String TEST_SOURCE = "test-search"; //$NON-NLS-1$
	static final String BASELINE_DIR_NAME = "baseline"; //$NON-NLS-1$
	static final String BASELINE_NAME = "default_baseline"; //$NON-NLS-1$
	static final String SCOPE_NAME = "default_scope"; //$NON-NLS-1$
	static final IPath TEST_SRC_ROOT = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE);
	static final HashSet<String> DEFAULT_SCOPE_PROJECTS = new HashSet<>();
	static final String P1_NAME = "a.b.c.P1"; //$NON-NLS-1$
	static final String P2_NAME = "x.y.z.P2"; //$NON-NLS-1$
	static final String P3_NAME = "l.m.n.P3"; //$NON-NLS-1$
	static final String DEFAULT_VERSION = "1.0.0"; //$NON-NLS-1$

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
		if (this.baseline == null) {
			this.baseline = createBaseline(BASELINE_NAME, null);
		}
		return this.baseline;
	}

	/**
	 * Creates an {@link IApiBaseline} from all jar'd projects listed in the
	 * '/test-search/baseline' directory
	 *
	 * @return a new {@link IApiBaseline}
	 */
	IApiBaseline createBaseline(final String name, final HashSet<String> projectnames) throws CoreException {
		File file = TEST_SRC_ROOT.append(BASELINE_DIR_NAME).toFile();
		if (!file.exists()) {
			fail("The baseline test directory must exist"); //$NON-NLS-1$
		}
		IApiBaseline base = ApiModelFactory.newApiBaseline(name, Util.getEEDescriptionFile());
		File[] jars = file.listFiles((FileFilter) pathname -> {
			String name1 = pathname.getName();
			if (Util.isArchive(name1)) {
				if (projectnames != null) {
					IPath path = new Path(pathname.getAbsolutePath());
					return projectnames.contains(path.removeFileExtension().lastSegment());
				} else {
					return true;
				}
			}
			return false;
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
		if (filename == null) {
			return null;
		}
		return TEST_SRC_ROOT.append("exclude").append(filename).toOSString(); //$NON-NLS-1$
	}

	/**
	 * Returns the populated exclude set or an empty list if the exclude file
	 * location is <code>null</code>
	 *
	 * @param baseline
	 * @param filename
	 * @return the listing of excluded items
	 */
	protected HashSet<String> getExcludeSet(IApiBaseline baseline, String filename) throws CoreException {
		if (filename == null) {
			return null;
		}
		// MY fix it
		final FilteredElements excludedElements = Util.initializeRegexFilterList(getExcludeFilePath(filename), baseline, false);
		final HashSet<String> result = new HashSet<>(excludedElements.getExactMatches());
		result.addAll(excludedElements.getPartialMatches());
		return result;
	}

	/**
	 * reports a failure in the search framework back to the parent test
	 *
	 * @param message
	 */
	protected void reportFailure(String message) {
		fail(message);
	}

	@Before
	public void setUp() throws Exception {
		TEST_REQUESTOR = new TestRequestor(this);
		TEST_REPORTER = new TestReporter(this);
	}

	@After
	public void tearDown() throws Exception {
		if (this.baseline != null) {
			this.baseline.dispose();
		}
		if (this.scope != null) {
			this.scope.dispose();
		}
	}
}
