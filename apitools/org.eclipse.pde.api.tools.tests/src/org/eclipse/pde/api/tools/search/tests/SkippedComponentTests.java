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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.search.SkippedComponent} class
 *
 * @since 1.0.1
 */
public class SkippedComponentTests extends SearchTest {

	static final String SC_NAME = "l.m.n.P"; //$NON-NLS-1$

	IApiComponent TESTING_COMPONENT = null;

	IApiComponent getTestingComponent() throws CoreException {
		if(TESTING_COMPONENT == null) {
				TESTING_COMPONENT = getTestBaseline().getApiComponent(SC_NAME);
		}
		return TESTING_COMPONENT;
	}

	/**
	 * Tests the {@link SkippedComponent#equals(Object)} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testEquals() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The components should be equal", scomp1, scomp2); //$NON-NLS-1$
		Assert.assertNotEquals("The components should not be equal", scomp1, tcomp); //$NON-NLS-1$
		Assert.assertNotEquals("The components should not be equal", scomp2, tcomp); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#hashCode()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testHashCode() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The component hashcodes should be equal", scomp1.hashCode(), scomp2.hashCode()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getComponentId()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetComponentId() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The component ids should be equal", scomp1.getComponentId(), scomp2.getComponentId()); //$NON-NLS-1$
		assertEquals("The component ids should be equal", scomp1.getComponentId(), tcomp.getSymbolicName()); //$NON-NLS-1$
		assertEquals("The component ids should be equal", scomp2.getComponentId(), tcomp.getSymbolicName()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#wasExcluded()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testWasExcluded() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertFalse("The testing component was not excluded", scomp1.wasExcluded()); //$NON-NLS-1$
		scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
		assertTrue("The testing component was excluded", scomp1.wasExcluded()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#hasResolutionErrors()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testHasResolutionErrors() throws CoreException {
			IApiComponent tcomp = getTestingComponent();
			assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
			SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
			assertFalse("The testing component did have resolution errors", scomp1.hasResolutionErrors()); //$NON-NLS-1$
			scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
			assertTrue("The testing component did not have resolution errors", scomp1.hasResolutionErrors()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getAncestor(int)} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetAncestor() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull("there should be no ancestors for SkippedComponents", scomp1.getAncestor(IApiElement.COMPONENT)); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getApiComponent()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetApiComponent() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull("there should be no IApiComponent object for SkippedComponents", scomp1.getApiComponent()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getName()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetName() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The names should be equal", tcomp.getSymbolicName(), scomp1.getName()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getParent()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetParent() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull("there should be no parentt object for SkippedComponents", scomp1.getParent()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getType()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetType() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The type should be IApiElement.COMPONENT", IApiElement.COMPONENT, scomp1.getType()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getVersion()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetVersion() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals("The version should be 1.0.0", scomp1.getVersion(), DEFAULT_VERSION); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getErrors()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetErrors() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNotNull("There should be resolution errors for the testing component", scomp1.getErrors()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getErrorDetails()} method
	 *
	 * @throws CoreException
	 */
	@Test
	public void testGetErrorDetails() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull("The testing component should not be null", tcomp); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNotNull("There should be resolution errors for the testing component", scomp1.getErrors()); //$NON-NLS-1$
		String reason = scomp1.getErrorDetails();
		assertTrue("The reason should be because of a unresolved constraint", reason.startsWith("Require-Bundle:")); //$NON-NLS-1$ //$NON-NLS-2$
		scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
		assertNull("There should be no errors for the testing component", scomp1.getErrors()); //$NON-NLS-1$
		reason = scomp1.getErrorDetails();
		assertTrue("The reason should be because it was exclude", //$NON-NLS-1$
				reason.startsWith("This component was excluded from the search by the search parameters.")); //$NON-NLS-1$
	}
}
