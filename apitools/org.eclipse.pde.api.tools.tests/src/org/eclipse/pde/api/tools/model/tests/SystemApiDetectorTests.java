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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.ApiAnnotations;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.ReferenceModifiers;

/**
 * Tests API manifest implementation.
 * 
 * @since 1.0.0
 */
public class SystemApiDetectorTests extends TestCase {
	/**
	 * Tests API description: java.lang.Object
	 */
	public void test1() throws CoreException {
		Reference reference = Reference.typeReference(null, "java.lang.Object", ReferenceModifiers.REF_EXTENDS);
		assertTrue(reference.resolve(ProfileModifiers.JRE_1_1));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
	}
	/**
	 * Tests API description: java.text.BreakIterator#getInt([BI)I
	 */
	public void test2() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.text.BreakIterator", "getInt", "([BI)I", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertFalse(reference.resolve(ProfileModifiers.JAVASE_1_6));
	}
	/**
	 * Tests API description: java.text.BreakIterator#getInt([BI)I
	 */
	public void test3() throws CoreException {
		Reference reference = Reference.typeReference(null, "javax.swing.table.TableRowSorter", ReferenceModifiers.REF_EXTENDS);
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
	}
	public void test4() {
		int[] profileValues = {
			ProfileModifiers.JRE_1_1,
			ProfileModifiers.J2SE_1_2,
			ProfileModifiers.J2SE_1_3,
			ProfileModifiers.J2SE_1_4,
			ProfileModifiers.J2SE_1_5,
			ProfileModifiers.JAVASE_1_6,
			ProfileModifiers.OSGI_MINIMUM_1_0,
			ProfileModifiers.OSGI_MINIMUM_1_1,
			ProfileModifiers.OSGI_MINIMUM_1_2,
		};
		int[] visibilities = {
			VisibilityModifiers.API,
			VisibilityModifiers.SPI,
			VisibilityModifiers.PRIVATE,
			VisibilityModifiers.PRIVATE_PERMISSIBLE,
		};
		int[] restrictions = {
			RestrictionModifiers.NO_EXTEND,
			RestrictionModifiers.NO_IMPLEMENT,
			RestrictionModifiers.NO_INSTANTIATE,
			RestrictionModifiers.NO_OVERRIDE,
			RestrictionModifiers.NO_REFERENCE,
			RestrictionModifiers.NO_RESTRICTIONS,
		};
		for (int i = 0, max = restrictions.length; i < max; i++) {
			int restriction = restrictions[i];
			for (int j = 0, max2 = visibilities.length; j < max2; j++) {
				int visibility = visibilities[j];
				for (int n = 0, max3 = profileValues.length; n < max3; n++) {
					int addedProfileValue = profileValues[n];
					for (int m = 0, max4 = profileValues.length; m < max4; m++) {
						int removedProfileValue = profileValues[m];
						ApiAnnotations annotations = new ApiAnnotations(visibility, restriction, addedProfileValue, removedProfileValue);
						if (visibility != annotations.getVisibility()) {
							assertTrue("should be equals for visibility = " + visibility, false);
						}
						if (restriction != annotations.getRestrictions()) {
							assertTrue("should be equals for restriction = " + restriction, false);
						}
						if (addedProfileValue != annotations.getAddedProfile()) {
							assertTrue("should be equals for " + ProfileModifiers.getName(addedProfileValue), false);
						}
						if (removedProfileValue != annotations.getRemovedProfile()) {
							assertTrue("should be equals for " + ProfileModifiers.getName(removedProfileValue), false);
						}
					}
				}
			}
		}
	}
	/**
	 * Tests API description: java.lang.Integer.MAX_VALUE
	 */
	public void test5() throws CoreException {
		Reference reference = Reference.fieldReference(null, "java.lang.Integer", "MAX_VALUE", ReferenceModifiers.REF_GETSTATIC);
		assertTrue(reference.resolve(ProfileModifiers.JRE_1_1));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
	}
	/**
	 * Tests API description: java.lang.Integer.compareTo(Ljava.lang.Integer;)I
	 */
	public void test6() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.lang.Integer", "compareTo", "(Ljava/lang/Integer;)I", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.lang.Integer.compareTo(Ljava.lang.Object;)I
	 */
	public void test7() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.lang.Integer", "compareTo", "(Ljava/lang/Object;)I", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertFalse(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.lang.String#java.lang.String.replace(CharSequence, CharSequence)
	 */
	public void test8() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.lang.String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.text.resources.BreakIteratorRules
	 */
	public void test9() throws CoreException {
		Reference reference = Reference.typeReference(null, "java.text.resources.BreakIteratorRules", ReferenceModifiers.REF_EXTENDS);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertFalse(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.text.resources.BreakIteratorRules#getContents()[[Ljava/lang/Object;
	 */
	public void test10() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.text.resources.BreakIteratorRules", "getContents", "()[[Ljava/lang/Object;", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertFalse(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.lang.StringBuffer#insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;
	 */
	public void test11() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.lang.StringBuffer", "insert", "(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;", ReferenceModifiers.REF_VIRTUALMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}
	/**
	 * Tests API description: java.lang.RuntimeException(Throwable)
	 */
	public void test12() throws CoreException {
		Reference reference = Reference.methodReference(null, "java.lang.RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", ReferenceModifiers.REF_CONSTRUCTORMETHOD);
		assertFalse(reference.resolve(ProfileModifiers.JRE_1_1));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_2));
		assertFalse(reference.resolve(ProfileModifiers.J2SE_1_3));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_4));
		assertTrue(reference.resolve(ProfileModifiers.J2SE_1_5));
		assertTrue(reference.resolve(ProfileModifiers.JAVASE_1_6));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_0));
		assertFalse(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_1));
		assertTrue(reference.resolve(ProfileModifiers.OSGI_MINIMUM_1_2));
		assertFalse(reference.resolve(ProfileModifiers.CDC_1_0_FOUNDATION_1_0));
		assertTrue(reference.resolve(ProfileModifiers.CDC_1_1_FOUNDATION_1_1));
	}	
}
