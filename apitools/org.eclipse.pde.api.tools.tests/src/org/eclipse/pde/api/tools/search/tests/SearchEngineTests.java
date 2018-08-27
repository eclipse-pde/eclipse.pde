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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.junit.Test;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine}
 *
 * @since 1.0.1
 */
public class SearchEngineTests extends SearchTest {

	/**
	 * Tests the the engine properly aborts with invalid <code>null</code>
	 * arguments
	 *
	 * @throws CoreException
	 */
	@Test
	public void testNullArguments() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		engine.search(null, TEST_REQUESTOR, TEST_REPORTER, null);
		engine.search(null, null, TEST_REPORTER, null);
		engine.search(null, null, null, null);
		engine.search(getTestBaseline(), null, null, null);
		engine.search(null, TEST_REQUESTOR, null, null);
	}

	/**
	 * Tests that the search engine properly reports matches when the scope and
	 * baseline are one-in-the-same
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchNoSeparateScope() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		TEST_REQUESTOR.setScopeBaseline(getTestBaseline());
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL },
						{ IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that the search engine properly reports matches when the scope and
	 * baseline are not the same
	 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline}
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchSeparateScope() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL },
						{ IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that an entry in the exclude file is honored
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchExcludeOne() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excludeone.txt")); //$NON-NLS-1$
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME },
				new int[][] { { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(new String[] { P3_NAME });
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that all elements that appear in the exclude file are left out
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchExcludeAll() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excludeall.txt")); //$NON-NLS-1$
		// expecting no reported references
		TEST_REPORTER.setExpectedReferences(null, null);
		TEST_REPORTER.setExpectedNotSearched(new String[] { P1_NAME, P2_NAME, P3_NAME });
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that a RegEx entry in the exclude file is honored (R:a.b.c.*)
	 *
	 * @throws CoreException
	 */
	@Test
	public void testExcludeRegexOne() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excluderegex.txt")); //$NON-NLS-1$
		TEST_REPORTER.setExpectedReferences(new String[] { P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(new String[] { P2_NAME });
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that a RegEx entry will cover all the proper matches (R:*.P*)
	 *
	 * @throws CoreException
	 */
	@Test
	public void testExcludeRegexAll() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excluderegexall.txt")); //$NON-NLS-1$
		// expecting no reported references
		TEST_REPORTER.setExpectedReferences(null, null);
		TEST_REPORTER.setExpectedNotSearched(new String[] { P1_NAME, P2_NAME, P3_NAME });
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests searching for API only
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchApiOnly() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL }, { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests searching for internal references only
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchInternalOnly() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL }, { IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Search with tracing on (causing the console to have content)
	 *
	 * @throws CoreException
	 */
	@Test
	public void testSearchWithDebugOn() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL }, { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		// ApiPlugin.DEBUG_SEARCH_ENGINE = true;
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}

	/**
	 * Tests that the search engine properly reports matches when the scope and
	 * baseline are not the same
	 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline}
	 *
	 * @throws CoreException
	 */
	@Test
	public void testBadRegex() throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		this.scope = getTestScope(DEFAULT_SCOPE_PROJECTS);
		TEST_REQUESTOR.setScopeBaseline(this.scope);
		TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
		TEST_REPORTER.setExpectedReferences(new String[] { P2_NAME, P3_NAME },
				new int[][] { { IReference.REF_FIELDDECL, IReference.REF_FIELDDECL },
						{ IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL } });
		TEST_REPORTER.setExpectedNotSearched(null);
		engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
	}
}
