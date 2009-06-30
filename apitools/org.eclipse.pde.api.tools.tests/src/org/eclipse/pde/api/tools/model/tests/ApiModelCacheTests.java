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
package org.eclipse.pde.api.tools.model.tests;

import junit.framework.TestCase;

import org.eclipse.pde.api.tools.internal.model.ApiModelCache;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * Tests the {@link ApiModelCache}
 * 
 * @since 1.0.2
 */
public class ApiModelCacheTests extends TestCase {

	static final String TEST_COMP_ID = "testcomp-id";
	static final String TEST_BASELINE_ID = "testbaseline-id";
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		ApiModelCache.getCache().flushCaches();
		super.tearDown();
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
				"()V",
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
	public void testAddRemoveElementHandleInfo() throws Exception {
		String typename = "testtype1";
		cacheType(typename);
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNotNull("The type "+typename+" should have been retrieved", element);
		assertTrue("The test element "+typename+" should have been removed", ApiModelCache.getCache().removeElementInfo(element));
		element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.COMPONENT);
		assertNull("the test API component "+TEST_COMP_ID+" should not have been retrieved", element);
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty());
	}
	
	/**
	 * Tests aching / removing {@link IApiElement}s using String identifiers instead 
	 * of {@link IApiElement} handles
	 *  
	 * @throws Exception
	 */
	public void testAddRemoveElementStringInfo() throws Exception {
		String typename = "testtype2";
		cacheType(typename);
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNotNull("The type "+typename+" should have been retrieved", element);
		assertTrue("The test element "+typename+" should have been removed", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, element.getName(), element.getType()));
		element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.COMPONENT);
		assertNull("the test API component "+TEST_COMP_ID+" should not have been retrieved", element);
		assertTrue("The cache should be empty", ApiModelCache.getCache().isEmpty());
	}
	
	/**
	 * Tests trying to remove a non-existent type from the cache with cached types
	 * 
	 * @throws Exception
	 */
	public void testRemoveNonExistentType() throws Exception {
		cacheType("testtype2");
		cacheType("testtype1");
		String typename = "testtype3";
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNull("The element 'testtype3' should not exist in the cache", element);
		assertFalse("The element 'testtype3' should not havde been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "testtype3", IApiElement.TYPE));
	}
	
	/**
	 * Tests trying to remove a non-existent type from the cache when nothing has been cached yet
	 * 
	 * @throws Exception
	 */
	public void testRemoveNonExistentTypeEmptyCache() throws Exception {
		String typename = "testtype3";
		IApiElement element = ApiModelCache.getCache().getElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, typename, IApiElement.TYPE);
		assertNull("The element 'testtype3' should not exist in the cache", element);
		assertFalse("The element 'testtype3' should not havde been removed from the cache", ApiModelCache.getCache().removeElementInfo(TEST_BASELINE_ID, TEST_COMP_ID, "testtype3", IApiElement.TYPE));
	}
}
