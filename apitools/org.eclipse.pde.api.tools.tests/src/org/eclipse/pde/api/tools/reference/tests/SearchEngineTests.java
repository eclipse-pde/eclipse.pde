/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.reference.tests;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.search.XMLFactory;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.w3c.dom.Document;

/**
 * Tests search engine.
 * 
 * @since 1.0.0
 */
public class SearchEngineTests extends TestCase {
	
	/**
	 * Searches for subclasses of A.
	 * 
	 * @throws CoreException
	 */
	public void testSearchForExtenders() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IPackageDescriptor pkg = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor typeA = pkg.getType("A");
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_EXTENDS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.addElementRestriction(componentA.getId(), new IElementDescriptor[]{typeA});
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of extenders", 1, refs.length);
		assertEquals("Wrong extender", Factory.packageDescriptor("component.b").getType("B"), refs[0].getSourceLocation().getMember());
	}
	
	/**
	 * Searches for extensions of A by name matching, rather than element matching.
	 * 
	 * @throws CoreException
	 */
	public void testSearchForExtendersByName() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_EXTENDS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.addPatternRestriction("component.a.A", IElementDescriptor.T_REFERENCE_TYPE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of extenders", 1, refs.length);
		assertEquals("Wrong extender", Factory.packageDescriptor("component.b").getType("B"), refs[0].getSourceLocation().getMember());
	}
	
	/**
	 * Searches for callers of the "no*" methods in MethodNoExtendClass
	 * 
	 * @throws CoreException
	 */
	public void testSearchForCallersByRegEx() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(
				ReferenceModifiers.REF_VIRTUALMETHOD | ReferenceModifiers.REF_SPECIALMETHOD,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.addPatternRestriction("no.*", IElementDescriptor.T_METHOD);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of callers", 3, refs.length);
	}	
	
	/**
	 * Searches for extensions of A by name matching, rather than element matching.
	 * 
	 * @throws CoreException
	 */
	public void testSearchForExtendersInPackageByName() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_EXTENDS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.addPatternRestriction("component.a", IElementDescriptor.T_PACKAGE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of extenders", 4, refs.length);
		assertEquals("Wrong extender", Factory.packageDescriptor("component.b").getType("B"), refs[0].getSourceLocation().getMember());
	}	
	
	/**
	 * Tests that references to illegal method overrides are found.
	 * 
	 * @throws CoreException
	 */
	public void testSearchForMethodExtenders() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IPackageDescriptor pkga = Factory.packageDescriptor("component.a");
		IReferenceTypeDescriptor base = pkga.getType("MethodNoExtendClass");
		IMethodDescriptor m1 = base.getMethod("noOverride", "()V");
		IMethodDescriptor m2 = base.getMethod("noOverridePrimitiveArg", "(I)V");
		IMethodDescriptor m3 = base.getMethod("noOverrideStringArg", "(Ljava/lang/String;)V");
		componentA.getApiDescription().setRestrictions(null, m1, RestrictionModifiers.NO_EXTEND);
		componentA.getApiDescription().setRestrictions(null, m2, RestrictionModifiers.NO_EXTEND);
		componentA.getApiDescription().setRestrictions(null, m3, RestrictionModifiers.NO_EXTEND);
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_OVERRIDE,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_EXTEND);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of overrides", 3, refs.length);
		Set<String> set = new HashSet<String>();
		set.add(m1.getName());
		set.add(m1.getSignature());
		set.add(m2.getName());
		set.add(m2.getSignature());
		set.add(m3.getName());
		set.add(m3.getSignature());
		for (int i = 0; i < refs.length; i++) {
			IReference reference = refs[i];
			IMethodDescriptor method = (IMethodDescriptor) reference.getTargetLocation().getMember();
			assertTrue("Method not present", set.remove(method.getName()));
			assertTrue("Signature not present", set.remove(method.getSignature()));
		}
	}	
	
	/**
	 * Searches for an illegal instantiation
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalInstantiate() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("NoInstantiateClass");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_INSTANTIATE);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_INSTANTIATE,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_INSTANTIATE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal instantiations", 1, refs.length);
		assertEquals("Wrong source", Factory.packageDescriptor("e.f.g").getType("TestInstantiate"), refs[0].getSourceLocation().getType());
	}	
	
	/**
	 * Searches for an illegal subclass
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalExtendsClass() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("NoExtendClass");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_EXTEND);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_EXTENDS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_EXTEND);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal extensions", 1, refs.length);
		assertEquals("Wrong source", Factory.packageDescriptor("e.f.g").getType("TestSubclass"), refs[0].getSourceLocation().getType());
	}		
	
	/**
	 * Searches for an illegal interface implementation
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalImplementInterface() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("INoImplementInterface");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_IMPLEMENT);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentA, componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_IMPLEMENTS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_IMPLEMENT);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal implements", 1, refs.length);
		assertEquals("Wrong source", Factory.packageDescriptor("e.f.g").getType("TestImplement"), refs[0].getSourceLocation().getType());
	}
	
	/**
	 * Searches for an legal interface implementation within a component.
	 * Since the restrictions do not apply to the owning component, the implementation
	 * within the same component is OK.
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalImplementWithinComponent() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("INoImplementInterface");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_IMPLEMENT);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentA});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(true);
		criteria.setReferenceKinds(ReferenceModifiers.REF_IMPLEMENTS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_IMPLEMENT);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal implements", 0, refs.length);
	}	

	/**
	 * Searches for an illegal interface implementation
	 * 
	 * @throws CoreException
	 */
	public void testIgnoreIllegalImplementWithinComponent() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("INoImplementInterface");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_IMPLEMENT);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentA});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_IMPLEMENTS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_IMPLEMENT);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal implements", 0, refs.length);
	}
	
	/**
	 * Tests that search results can be written to XML file.
	 * @throws CoreException 
	 */
	public void testXMLReferences() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noInst = Factory.packageDescriptor("component.a").getType("INoImplementInterface");
		componentA.getApiDescription().setRestrictions(null, noInst, RestrictionModifiers.NO_IMPLEMENT);
		IReferenceTypeDescriptor noExt = Factory.packageDescriptor("component.a").getType("NoExtendClass");
		componentA.getApiDescription().setRestrictions(null, noExt, RestrictionModifiers.NO_EXTEND);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria c1 = Factory.newSearchCriteria();
		IApiSearchCriteria c2 = Factory.newSearchCriteria();
		c1.setConsiderComponentLocalReferences(false);
		c1.setReferenceKinds(ReferenceModifiers.REF_EXTENDS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_EXTEND);
		c1.addComponentRestriction(componentA.getId());
		c2.setConsiderComponentLocalReferences(false);
		c2.setReferenceKinds(ReferenceModifiers.REF_IMPLEMENTS,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_IMPLEMENT);
		c2.addComponentRestriction(componentA.getId());		
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{c1, c2}, null);
		assertTrue("Wrong number of references", refs.length > 0);
		
		Document document = XMLFactory.serializeReferences(refs);
		Util.serializeDocument(document);
		// System.out.println(xml);
		// TODO: how to validate XML?
		
	}

	/**
	 * Searches for an illegal method references
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalMethodReference() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noCall = Factory.packageDescriptor("component.a").getType("MethodNoReference");
		IMethodDescriptor instanceMethod = noCall.getMethod("doNotCallInstance", "()V");
		IMethodDescriptor staticMethod = noCall.getMethod("doNotCallStatic", "()V");
		IMethodDescriptor superMethod = noCall.getMethod("doNotCallSuper", "()V");
		componentA.getApiDescription().setRestrictions(null, instanceMethod, RestrictionModifiers.NO_REFERENCE);
		componentA.getApiDescription().setRestrictions(null, staticMethod, RestrictionModifiers.NO_REFERENCE);
		componentA.getApiDescription().setRestrictions(null, superMethod, RestrictionModifiers.NO_REFERENCE);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_SPECIALMETHOD | ReferenceModifiers.REF_STATICMETHOD | ReferenceModifiers.REF_VIRTUALMETHOD,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_REFERENCE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal references", 3, refs.length);
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < refs.length; i++) {
			set.add(refs[i].getSourceLocation().getMember().getName());
		}
		assertTrue("missing callInstance", set.contains("callInstance"));
		assertTrue("missing callInstance", set.contains("callStatic"));
		assertTrue("missing callInstance", set.contains("callSuper"));
		assertFalse("should not flag doNotCallSuper", set.contains("doNotCallSuper"));
	}	
	
	/**
	 * Searches for an illegal field references
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalFieldReference() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noCall = Factory.packageDescriptor("component.a").getType("FieldNoReference");
		IFieldDescriptor instanceField = noCall.getField("INSTANCE_NO_REF");
		IFieldDescriptor staticField = noCall.getField("STATIC_NO_REF");
		componentA.getApiDescription().setRestrictions(null, instanceField, RestrictionModifiers.NO_REFERENCE);
		componentA.getApiDescription().setRestrictions(null, staticField, RestrictionModifiers.NO_REFERENCE);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_GETFIELD | ReferenceModifiers.REF_GETSTATIC |
				ReferenceModifiers.REF_PUTFIELD | ReferenceModifiers.REF_PUTSTATIC,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_REFERENCE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		assertEquals("Wrong number of illegal references", 4, refs.length);
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < refs.length; i++) {
			set.add(refs[i].getSourceLocation().getMember().getName());
		}
		assertTrue("missing callInstance", set.contains("readInstance"));
		assertTrue("missing callInstance", set.contains("readStatic"));
		assertTrue("missing callInstance", set.contains("writeInstance"));
		assertTrue("missing callInstance", set.contains("writeStatic"));		
	}
	
	/**
	 * Searches for an illegal method references on interfaces
	 * 
	 * @throws CoreException
	 */
	public void testSearchIllegalInterfaceMethodReference() throws CoreException {
		IApiProfile profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		IApiComponent componentB = profile.getApiComponent("component.b");
		IReferenceTypeDescriptor noCall = Factory.packageDescriptor("component.a").getType("MethodNoReferenceInterface");
		IMethodDescriptor method = noCall.getMethod("getName", "()Ljava/lang/String;");
		componentA.getApiDescription().setRestrictions(null, method, RestrictionModifiers.NO_REFERENCE);
		IApiSearchScope sourceScope = Factory.newScope(new IApiComponent[]{componentB});
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.REF_INTERFACEMETHOD,
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.NO_REFERENCE);
		IReference[] refs = engine.search(sourceScope, new IApiSearchCriteria[]{criteria}, null);
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < refs.length; i++) {
			set.add(refs[i].getSourceLocation().getMember().getName());
		}
		assertTrue("missing callBaseInterface", set.contains("callBaseInterface"));
		assertTrue("missing callLeafInterface", set.contains("callLeafInterface"));
		assertEquals("Wrong number of illegal references", 2, refs.length);
	}		
}

