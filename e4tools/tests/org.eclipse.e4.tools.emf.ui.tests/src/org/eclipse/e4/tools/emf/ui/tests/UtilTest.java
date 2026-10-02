/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.tools.emf.ui.common.IEditorFeature.FeatureClass;
import org.eclipse.e4.tools.emf.ui.common.Util;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.fragment.MFragmentFactory;
import org.eclipse.e4.ui.model.fragment.MModelFragments;
import org.eclipse.e4.ui.model.fragment.MStringModelFragment;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.junit.jupiter.api.Test;

public class UtilTest {

	private static MPart part(String id) {
		final MPart part = MBasicFactory.INSTANCE.createPart();
		part.setElementId(id);
		return part;
	}

	private static MPartStack stack(MPart... parts) {
		final MPartStack stack = MBasicFactory.INSTANCE.createPartStack();
		for (final MPart part : parts) {
			stack.getChildren().add(part);
		}
		return stack;
	}

	private static EditingDomain editingDomain() {
		return new AdapterFactoryEditingDomain(
				new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE),
				new BasicCommandStack());
	}

	@Test
	public void testIsNullOrEmpty() {
		assertTrue(Util.isNullOrEmpty(null));
		assertTrue(Util.isNullOrEmpty("")); //$NON-NLS-1$
		assertTrue(Util.isNullOrEmpty("  ")); //$NON-NLS-1$
		assertFalse(Util.isNullOrEmpty(" a ")); //$NON-NLS-1$
	}

	@Test
	public void testIsImport() {
		final MModelFragments fragments = MFragmentFactory.INSTANCE.createModelFragments();
		final MPart imported = part("imported"); //$NON-NLS-1$
		fragments.getImports().add(imported);
		final MStringModelFragment fragment = MFragmentFactory.INSTANCE.createStringModelFragment();
		final MPart contributed = part("contributed"); //$NON-NLS-1$
		fragment.getElements().add(contributed);
		fragments.getFragments().add(fragment);

		assertTrue(Util.isImport((EObject) imported));
		assertFalse(Util.isImport((EObject) contributed));
		assertFalse(Util.isImport((EObject) fragment));
	}

	@Test
	public void testAddClassesListsConcreteApplicationElements() {
		// EClasses come from model instances, the generated package literals are not API
		final EClass addon = ((EObject) MApplicationFactory.INSTANCE.createAddon()).eClass();
		final EClass part = ((EObject) MBasicFactory.INSTANCE.createPart()).eClass();
		final EClass application = ((EObject) MApplicationFactory.INSTANCE.createApplication()).eClass();
		final EPackage applicationPackage = addon.getEPackage();
		final List<FeatureClass> classes = new ArrayList<>();
		Util.addClasses(applicationPackage, classes);

		final List<Object> eClasses = classes.stream().map(c -> (Object) c.eClass).toList();
		assertTrue(eClasses.contains(part), "sub packages are searched"); //$NON-NLS-1$
		assertTrue(eClasses.contains(addon));
		assertFalse(eClasses.contains(application));
		assertFalse(eClasses.contains(applicationPackage.getEClassifier("ApplicationElement"))); //$NON-NLS-1$
		for (final FeatureClass c : classes) {
			assertFalse(c.eClass.isAbstract(), c.label);
			assertFalse(c.eClass.isInterface(), c.label);
			assertEquals(c.eClass.getName(), c.label);
		}
	}

	@Test
	public void testDefaultElementIdFillsFirstGap() {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("Demo"); //$NON-NLS-1$
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final Resource resource = new XMIResourceImpl(URI.createURI("test.e4xmi")); //$NON-NLS-1$
		resource.getContents().add((EObject) application);

		assertEquals("demo.part.0", Util.getDefaultElementId(resource, part(null), project)); //$NON-NLS-1$

		final MPartStack stack = stack(part("demo.part.0"), part("demo.part.1"), part("demo.part.3"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				part("demo.part.x"), part("other.part.2")); //$NON-NLS-1$ //$NON-NLS-2$
		application.getChildren().add(MBasicFactory.INSTANCE.createTrimmedWindow());
		application.getChildren().get(0).getChildren().add(stack);

		assertEquals("demo.part.2", Util.getDefaultElementId(resource, part(null), project)); //$NON-NLS-1$
		assertEquals("demo.partstack.0", //$NON-NLS-1$
				Util.getDefaultElementId(resource, MBasicFactory.INSTANCE.createPartStack(), project));
	}

	@Test
	public void testMoveElementByIndexWithCommand() {
		final MPart a = part("a"); //$NON-NLS-1$
		final MPart b = part("b"); //$NON-NLS-1$
		final MPart c = part("c"); //$NON-NLS-1$
		final MPartStack stack = stack(a, b, c);
		final EditingDomain domain = editingDomain();

		assertTrue(Util.moveElementByIndex(domain, c, false, 0));
		assertEquals(List.of(c, a, b), stack.getChildren());
		assertNull(stack.getSelectedElement());

		domain.getCommandStack().undo();
		assertEquals(List.of(a, b, c), stack.getChildren());
	}

	@Test
	public void testMoveElementByIndexInLiveModel() {
		final MPart a = part("a"); //$NON-NLS-1$
		final MPart b = part("b"); //$NON-NLS-1$
		final MPart c = part("c"); //$NON-NLS-1$
		final MPartStack stack = stack(a, b, c);
		final EditingDomain domain = editingDomain();

		assertTrue(Util.moveElementByIndex(domain, a, true, 1));
		assertEquals(List.of(b, a, c), stack.getChildren());
		assertSame(a, stack.getSelectedElement());
		assertFalse(domain.getCommandStack().canUndo(), "live changes bypass the command stack"); //$NON-NLS-1$

		assertTrue(Util.moveElementByIndex(domain, b, true, -1));
		assertEquals(List.of(a, c, b), stack.getChildren());
	}
}
