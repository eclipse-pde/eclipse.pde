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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


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
	 */
	@Test
	public void testEquals() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(scomp1, scomp2, "The components should be equal"); //$NON-NLS-1$
		Assertions.assertNotEquals(scomp1, tcomp, "The components should not be equal"); //$NON-NLS-1$
		Assertions.assertNotEquals(scomp2, tcomp, "The components should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#hashCode()} method
	 */
	@Test
	public void testHashCode() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(scomp1.hashCode(), scomp2.hashCode(), "The component hashcodes should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getComponentId()} method
	 */
	@Test
	public void testGetComponentId() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		SkippedComponent scomp2 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(scomp1.getComponentId(), scomp2.getComponentId(), "The component ids should be equal"); //$NON-NLS-1$
		assertEquals(scomp1.getComponentId(), tcomp.getSymbolicName(), "The component ids should be equal"); //$NON-NLS-1$
		assertEquals(scomp2.getComponentId(), tcomp.getSymbolicName(), "The component ids should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#wasExcluded()} method
	 */
	@Test
	public void testWasExcluded() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertFalse(scomp1.wasExcluded(), "The testing component was not excluded"); //$NON-NLS-1$
		scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
		assertTrue(scomp1.wasExcluded(), "The testing component was excluded"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#hasResolutionErrors()} method
	 */
	@Test
	public void testHasResolutionErrors() throws CoreException {
			IApiComponent tcomp = getTestingComponent();
			assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
			SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
			assertFalse(scomp1.hasResolutionErrors(), "The testing component did have resolution errors"); //$NON-NLS-1$
			scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
			assertTrue(scomp1.hasResolutionErrors(), "The testing component did not have resolution errors"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getAncestor(int)} method
	 */
	@Test
	public void testGetAncestor() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull(scomp1.getAncestor(IApiElement.COMPONENT), "there should be no ancestors for SkippedComponents"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getApiComponent()} method
	 */
	@Test
	public void testGetApiComponent() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull(scomp1.getApiComponent(), "there should be no IApiComponent object for SkippedComponents"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getName()} method
	 */
	@Test
	public void testGetName() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(tcomp.getSymbolicName(), scomp1.getName(), "The names should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getParent()} method
	 */
	@Test
	public void testGetParent() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNull(scomp1.getParent(), "there should be no parentt object for SkippedComponents"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getType()} method
	 */
	@Test
	public void testGetType() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(IApiElement.COMPONENT, scomp1.getType(), "The type should be IApiElement.COMPONENT"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getVersion()} method
	 */
	@Test
	public void testGetVersion() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertEquals(scomp1.getVersion(), DEFAULT_VERSION, "The version should be 1.0.0"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getErrors()} method
	 */
	@Test
	public void testGetErrors() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNotNull(scomp1.getErrors(), "There should be resolution errors for the testing component"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link SkippedComponent#getErrorDetails()} method
	 */
	@Test
	public void testGetErrorDetails() throws CoreException {
		IApiComponent tcomp = getTestingComponent();
		assertNotNull(tcomp, "The testing component should not be null"); //$NON-NLS-1$
		SkippedComponent scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), tcomp.getErrors());
		assertNotNull(scomp1.getErrors(), "There should be resolution errors for the testing component"); //$NON-NLS-1$
		String reason = scomp1.getErrorDetails();
		assertTrue(reason.startsWith("Require-Bundle:"), "The reason should be because of a unresolved constraint"); //$NON-NLS-1$ //$NON-NLS-2$
		scomp1 = new SkippedComponent(tcomp.getSymbolicName(), tcomp.getVersion(), null);
		assertNull(scomp1.getErrors(), "There should be no errors for the testing component"); //$NON-NLS-1$
		reason = scomp1.getErrorDetails();
		assertTrue(reason.startsWith("This component was excluded from the search by the search parameters."), "The reason should be because it was exclude"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
