/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.CRCVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.junit.Test;

/**
 * Tests CRC codes for API descriptions
 */
public class CRCTests {

	/**
	 * Test that a CRC code for a type's API description is the same for equivalent descriptions.
	 */
	@Test
	public void testCRCEqual() {
		ApiDescription description = new ApiDescription("test.component"); //$NON-NLS-1$
		IReferenceTypeDescriptor type = Factory.typeDescriptor("org.eclipse.debug.core.SomeClass"); //$NON-NLS-1$
		description.setVisibility(type, VisibilityModifiers.API);
		description.setRestrictions(type, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND);
		IMethodDescriptor method = type.getMethod("someMethod", "(I)V"); //$NON-NLS-1$ //$NON-NLS-2$
		description.setRestrictions(method, RestrictionModifiers.NO_OVERRIDE);
		IFieldDescriptor field = type.getField("fField"); //$NON-NLS-1$
		description.setRestrictions(field, RestrictionModifiers.NO_REFERENCE);

		CRCVisitor visitor = new CRCVisitor();
		description.accept(visitor, type, null);
		long crc1 = visitor.getValue();

		// test the same in another description with different order

		ApiDescription description2 = new ApiDescription("test.component"); //$NON-NLS-1$
		IReferenceTypeDescriptor type2 = Factory.typeDescriptor("org.eclipse.debug.core.SomeClass"); //$NON-NLS-1$
		description2.setVisibility(type2, VisibilityModifiers.API);
		description2.setRestrictions(type2, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND);
		IFieldDescriptor field2 = type2.getField("fField"); //$NON-NLS-1$
		description2.setRestrictions(field2, RestrictionModifiers.NO_REFERENCE);
		IMethodDescriptor method2 = type2.getMethod("someMethod", "(I)V"); //$NON-NLS-1$ //$NON-NLS-2$
		description2.setRestrictions(method2, RestrictionModifiers.NO_OVERRIDE);

		CRCVisitor visitor2 = new CRCVisitor();
		description2.accept(visitor2, type2, null);
		long crc2 = visitor2.getValue();

		assertEquals("CRC codes should be the same", crc1, crc2); //$NON-NLS-1$
	}

	/**
	 * Test that a CRC changes when annotations for a method in a type have changed.
	 */
	@Test
	public void testCRCModified() {
		ApiDescription description = new ApiDescription("test.component"); //$NON-NLS-1$
		IReferenceTypeDescriptor type = Factory.typeDescriptor("org.eclipse.debug.core.SomeClass"); //$NON-NLS-1$
		description.setVisibility(type, VisibilityModifiers.API);
		description.setRestrictions(type, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND);
		IMethodDescriptor method = type.getMethod("someMethod", "(I)V"); //$NON-NLS-1$ //$NON-NLS-2$
		description.setRestrictions(method, RestrictionModifiers.NO_OVERRIDE);
		IFieldDescriptor field = type.getField("fField"); //$NON-NLS-1$
		description.setRestrictions(field, RestrictionModifiers.NO_REFERENCE);

		CRCVisitor visitor = new CRCVisitor();
		description.accept(visitor, type, null);
		long crc1 = visitor.getValue();

		// modify the annotations, check different
		description.setRestrictions(method, RestrictionModifiers.NO_REFERENCE);

		CRCVisitor visitor2 = new CRCVisitor();
		description.accept(visitor2, type, null);
		long crc2 = visitor2.getValue();

		assertFalse("CRC codes should be different", crc1 == crc2); //$NON-NLS-1$
	}

}
