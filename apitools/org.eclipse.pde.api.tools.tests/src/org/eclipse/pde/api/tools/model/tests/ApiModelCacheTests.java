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
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.pde.api.tools.internal.model.ApiModelCache;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.junit.After;
import org.junit.Test;

/**
 * Tests the {@link ApiModelCache}
 *
 * @since 1.0.2
 */
public class ApiModelCacheTests {

	static final String TEST_COMP_ID = "testcomp-id"; //$NON-NLS-1$
	static final String TEST_BASELINE_ID = "testbaseline-id"; //$NON-NLS-1$

	@After
	public void tearDown() throws Exception {
		ApiModelCache.getCache().flushCaches();
	}

	/**
	 * Creates a testing {@link IApiType} with the given type name ad adds it to the cache
	 * using the default test component - created using {@link #TEST_COMP_ID}
	 *
	 * @param typename
	 * @throws Exception
	 */
	private void cacheType(String typename) throws Exception {
		IApiType type = TestSuiteHelper.createTestingApiType(
				TEST_BASELINE_ID,
				TEST_COMP_ID,
				typename,
				"()V", //$NON-NLS-1$
				null,
				0,
				null);
		ApiModelCache.getCache().cacheElementInfo(type);
	}

	/**
	 * Tests caching / removing {@link IApiElement} infos
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddRemoveElementHandleInfo() throws Exception {
		String typename = "testtype1"; //$NON-NLS-1$
		cacheType(typename);
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNotNull("The type "+typename+" should have been retrieved", element); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The test element "+typename+" should have been removed", ApiModelCache.getCache().removeElementInfo(element)); //$NON-NLS-1$ //$NON-NLS-2$
		element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.COMPONENT);
		assertNull("the test API component "+TEST_COMP_ID+" should not have been retrieved", element); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty()); //$NON-NLS-1$
	}

	/**
	 * Tests aching / removing {@link IApiElement}s using String identifiers
	 * instead of {@link IApiElement} handles
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddRemoveElementStringInfo() throws Exception {
		String typename = "testtype2"; //$NON-NLS-1$
		cacheType(typename);
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNotNull("The type "+typename+" should have been retrieved", element); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The test element "+typename+" should have been removed", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, element.getName(), element.getType())); //$NON-NLS-1$ //$NON-NLS-2$
		element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.COMPONENT);
		assertNull("the test API component "+TEST_COMP_ID+" should not have been retrieved", element); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty()); //$NON-NLS-1$
	}

	/**
	 * Tests trying to remove a non-existent type from the cache with cached
	 * types
	 *
	 * @throws Exception
	 */
	@Test
	public void testRemoveNonExistentType() throws Exception {
		cacheType("testtype2"); //$NON-NLS-1$
		cacheType("testtype1"); //$NON-NLS-1$
		String typename = "testtype3"; //$NON-NLS-1$
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNull("The element 'testtype3' should not exist in the cache", element); //$NON-NLS-1$
		assertFalse("The element 'testtype3' should not have been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "testtype3", IApiElement.TYPE)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests trying to remove a non-existent type from the cache when nothing
	 * has been cached yet
	 *
	 * @throws Exception
	 */
	@Test
	public void testRemoveNonExistentTypeEmptyCache() throws Exception {
		String typename = "testtype3"; //$NON-NLS-1$
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNull("The element 'testtype3' should not exist in the cache", element); //$NON-NLS-1$
		assertFalse("The element 'testtype3' should not have been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "testtype3", IApiElement.TYPE)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding some member types to the cache and removing them via the
	 * root type
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddRemoveMemerTypeStringInfo() throws Exception {
		String roottypename = "a.b.c.testee1"; //$NON-NLS-1$
		cacheType("a.b.c.testee1"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner1"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner2"); //$NON-NLS-1$
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, roottypename, IApiElement.TYPE);
		assertNotNull("The element 'a.b.c.testee1' should exist in the cache", element); //$NON-NLS-1$
		assertNotNull("The element 'a.b.c.testee1$inner' should exist in the cache",  //$NON-NLS-1$
				ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner", IApiElement.TYPE)); //$NON-NLS-1$
		assertNotNull("The element 'a.b.c.testee1$inner1' should exist in the cache",  //$NON-NLS-1$
				ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner1", IApiElement.TYPE)); //$NON-NLS-1$
		assertNotNull("The element 'a.b.c.testee1$inner2' should exist in the cache",  //$NON-NLS-1$
				ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner2", IApiElement.TYPE)); //$NON-NLS-1$
		assertTrue("the type 'a.b.c.testee1$inner1' should have been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner1", IApiElement.TYPE)); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The type 'a.b.c.testee1' should have been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, roottypename, IApiElement.TYPE)); //$NON-NLS-1$
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty()); //$NON-NLS-1$
	}

	/**
	 * Tests adding some member types to the cache and removing them via the
	 * root type
	 *
	 * @throws Exception
	 */
	@Test
	public void testAddRemoveMemerTypeElementInfo() throws Exception {
		String roottypename = "a.b.c.testee1"; //$NON-NLS-1$
		cacheType("a.b.c.testee1"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner1"); //$NON-NLS-1$
		cacheType("a.b.c.testee1$inner2"); //$NON-NLS-1$
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner1", IApiElement.TYPE); //$NON-NLS-1$
		assertNotNull("The element 'a.b.c.testee1$inner1' should exist in the cache", element); //$NON-NLS-1$
		assertTrue("the type 'a.b.c.testee1$inner1' should have been removed from the cache", ApiModelCache.getCache().removeElementInfo(element)); //$NON-NLS-1$

		assertNotNull("The element 'a.b.c.testee1$inner' should exist in the cache",  //$NON-NLS-1$
				ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner", IApiElement.TYPE)); //$NON-NLS-1$
		assertNotNull("The element 'a.b.c.testee1$inner2' should exist in the cache",  //$NON-NLS-1$
				ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "a.b.c.testee1$inner2", IApiElement.TYPE)); //$NON-NLS-1$
		element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, roottypename, IApiElement.TYPE);
		assertNotNull("The element 'a.b.c.testee1' should exist in the cache", element); //$NON-NLS-1$
		assertTrue("The type 'a.b.c.testee1' should have been removed from the cache", ApiModelCache.getCache().removeElementInfo(element)); //$NON-NLS-1$
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty()); //$NON-NLS-1$
	}
}
