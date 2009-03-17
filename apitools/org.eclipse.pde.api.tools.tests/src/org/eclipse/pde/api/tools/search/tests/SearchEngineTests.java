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

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine}
 * 
 * @since 1.0.1
 */
public class SearchEngineTests extends SearchTest {
	
	/**
	 * Tests the the engine properly aborts with invalid <code>null</code> arguments
	 */
	public void testNullArguments() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			engine.search(null, TEST_REQUESTOR, TEST_REPORTER, null);
			engine.search(null, null, TEST_REPORTER, null);
			engine.search(null, null, null, null);
			engine.search(getTestBaseline(), null, null, null);
			engine.search(null, TEST_REQUESTOR, null, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that the search engine properly reports matches
	 * when the scope and baseline are one-in-the-same
	 */
	public void testSearchNoSeparateScope() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestBaseline());
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that the search engine properly reports matches when the scope and baseline
	 * are not the same {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline}
	 */
	public void testSearchSeparateScope() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that an entry in the exclude file is honored
	 */
	public void testSearchExcludeOne() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excludeone.txt"));
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(new String[] {P3_NAME});
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that all elements that appear in the exclude file are left out
	 */
	public void testSearchExcludeAll() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excludeall.txt"));
			//expecting no reported references
			TEST_REPORTER.setExpectedReferences(null, null);
			TEST_REPORTER.setExpectedNotSearched(new String[] {P1_NAME, P2_NAME, P3_NAME});
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that a RegEx entry in the exclude file is honored (R:a.b.c.*)
	 */
	public void testExcludeRegexOne() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excluderegex.txt"));
			TEST_REPORTER.setExpectedReferences(
					new String[] {P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(new String[] {P2_NAME});
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that a RegEx entry will cover all the proper matches (R:*.P*)
	 */
	public void testExcludeRegexAll() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REQUESTOR.setExcludedElements(getExcludeSet(getTestBaseline(), "excluderegexall.txt"));
			//expecting no reported references
			TEST_REPORTER.setExpectedReferences(null, null);
			TEST_REPORTER.setExpectedNotSearched(new String[] {P1_NAME, P2_NAME, P3_NAME});
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests searching for API only
	 */
	public void testSearchApiOnly() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests searching for internal references only
	 */
	public void testSearchInternalOnly() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Search with tracing on (causing the console to have content)
	 */
	public void testSearchWithDebugOn() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			ApiSearchEngine.setDebug(true);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
	
	/**
	 * Tests that the search engine properly reports matches when the scope and baseline
	 * are not the same {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline}
	 */
	public void testBadRegex() {
		ApiSearchEngine engine = new ApiSearchEngine();
		try {
			TEST_REQUESTOR.setScopeBaseline(getTestScope(DEFAULT_SCOPE_PROJECTS));
			TEST_REQUESTOR.setSearchMask(IApiSearchRequestor.INCLUDE_API | IApiSearchRequestor.INCLUDE_INTERNAL);
			TEST_REPORTER.setExpectedReferences(
					new String[] {P2_NAME, P3_NAME}, 
					new int[][] {{IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}, {IReference.REF_FIELDDECL, IReference.REF_FIELDDECL, IReference.REF_FIELDDECL}});
			TEST_REPORTER.setExpectedNotSearched(null);
			engine.search(getTestBaseline(), TEST_REQUESTOR, TEST_REPORTER, null);
		}
		catch(Exception e) {
			fail("The search engine should not throw an exception: "+e.getMessage());
		}
	}
}
