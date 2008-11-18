/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.internal.ApiAnnotations;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Tests API manifest implementation.
 * 
 * @since 1.0.0
 */
public class SystemApiDescriptionTests extends TestCase {
	
	private IApiDescription fJREManifest = buildManifest(ProfileModifiers.J2SE_1_5_NAME);
	private IApiDescription fOSGiManifest = buildManifest(ProfileModifiers.OSGI_MINIMUM_1_0_NAME);
	private IApiDescription fCDCManifest = buildManifest(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME);
	protected IApiDescription buildManifest(String eeID) {
		IApiDescription manifest = SystemLibraryApiDescription.newSystemLibraryApiDescription(eeID);
		return manifest;
	}
	/**
	 * Resolves field descriptor for a field with the given name.
	 *  
	 * @param typeName fully qualified name of referenced type
	 * @param fieldName field name of referenced type
	 */
	protected IFieldDescriptor resolveField(
			String typeName,
			String fieldName) {
		String packageName = Util.getPackageName(typeName);
		String tName = Util.getTypeName(typeName);
		return Factory.packageDescriptor(packageName).getType(tName).getField(fieldName);
	}
	/**
	 * Resolves method descriptor for a method with the given method name and method signature for a given name.
	 *  
	 * @param typeName fully qualified name of referenced type
	 * @param methodName the given method name
	 * @param methodSignature the given method signature
	 */
	protected IMethodDescriptor resolveMethod(
			String typeName,
			String methodName,
			String methodSignature) {
		String packageName = Util.getPackageName(typeName);
		String tName = Util.getTypeName(typeName);
		return
			Factory.packageDescriptor(packageName).getType(tName).getMethod(methodName, methodSignature);
	}
	/**
	 * Resolves reference type descriptor for a type with the given name.
	 *  
	 * @param typeName fully qualified name of referenced type
	 * @param addedProfile expected added profile modifiers
	 * @param removedProfile expected removed profile modifiers
	 */
	protected IReferenceTypeDescriptor resolveType(String typeName) {
		String packageName = Util.getPackageName(typeName);
		String tName = Util.getTypeName(typeName);
		return Factory.packageDescriptor(packageName).getType(tName);
	}
	/**
	 * Tests API description: java.lang.Object
	 */
	public void test1() {
		IReferenceTypeDescriptor elementDescriptor = resolveType("java.lang.Object");
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
	}
	/**
	 * Tests API description: java.text.BreakIterator#getInt([BI)I
	 */
	public void test2() {
		IElementDescriptor elementDescriptor = resolveMethod("java.text.BreakIterator", "getInt", "([BI)I");
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
	}
	/**
	 * Tests API description: java.text.BreakIterator#getInt([BI)I
	 */
	public void test3() {
		IElementDescriptor elementDescriptor = resolveType("javax.swing.table.TableRowSorter");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
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
	public void test5() {
		IElementDescriptor elementDescriptor = resolveField("java.lang.Integer", "MAX_VALUE");
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
	}
	/**
	 * Tests API description: java.lang.Integer.compareTo(Ljava.lang.Integer;)I
	 */
	public void test6() {
		IElementDescriptor elementDescriptor = resolveMethod("java.lang.Integer", "compareTo", "(Ljava/lang/Integer;)I");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
	/**
	 * Tests API description: java.lang.Integer.compareTo(Ljava.lang.Object;)I
	 */
	public void test7() {
		IElementDescriptor elementDescriptor = resolveMethod("java.lang.Integer", "compareTo", "(Ljava/lang/Object;)I");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
	/**
	 * Tests API description: java.lang.String#java.lang.String.replace(CharSequence, CharSequence)
	 */
	public void test8() {
		IElementDescriptor elementDescriptor = resolveMethod("java.lang.String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
	/**
	 * Tests API description: java.text.resources.BreakIteratorRules
	 */
	public void test9() {
		IElementDescriptor elementDescriptor = resolveType("java.text.resources.BreakIteratorRules");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
	/**
	 * Tests API description: java.text.resources.BreakIteratorRules#getContents()[[Ljava/lang/Object;
	 */
	public void test10() {
		IElementDescriptor elementDescriptor = resolveMethod("java.text.resources.BreakIteratorRules", "getContents", "()[[Ljava/lang/Object;");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
	/**
	 * Tests API description: java.lang.StringBuffer#insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;
	 */
	public void test11() {
		IElementDescriptor elementDescriptor = resolveMethod("java.lang.StringBuffer", "insert", "(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;");
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JRE_1_1_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_2_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_3_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_4_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.J2SE_1_5_NAME), elementDescriptor, fJREManifest));
		assertTrue(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.JAVASE_1_6_NAME), elementDescriptor, fJREManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_0_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_1_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.OSGI_MINIMUM_1_2_NAME), elementDescriptor, fOSGiManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME), elementDescriptor, fCDCManifest));
		assertFalse(Util.isAPI(ProfileModifiers.getValue(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME), elementDescriptor, fCDCManifest));
	}
}
