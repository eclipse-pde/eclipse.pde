/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * vogella GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.common.component.virtual.VirtualListEditor.Spec;
import org.eclipse.e4.tools.emf.ui.internal.common.component.virtual.VirtualListSpecs;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.advanced.MAdvancedFactory;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MCompositePart;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.fragment.MFragmentFactory;
import org.eclipse.e4.ui.model.fragment.MModelFragments;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

/**
 * Characterizes the {@link Spec}s of the virtual list nodes of the model editor
 * tree, since the editor behind them is configured by nothing else.
 */
public class VirtualListSpecsTest {

	private static List<Spec> allSpecs() throws IllegalAccessException {
		List<Spec> specs = new ArrayList<>();
		for (Field field : VirtualListSpecs.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == Spec.class) {
				specs.add((Spec) field.get(null));
			}
		}
		return specs;
	}

	private static Messages messagesNamedAfterKeys() throws IllegalAccessException {
		Messages messages = new Messages();
		for (Field field : Messages.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
				field.set(messages, field.getName());
			}
		}
		return messages;
	}

	private static Object get(Set<Spec> checked, Spec spec, Object container) {
		checked.add(spec);
		return ((EObject) container).eGet(spec.feature());
	}

	private static List<String> names(List<EClass> eClasses) {
		return eClasses.stream().map(EClass::getName).toList();
	}

	@Test
	public void testAllSpecsAreCovered() throws IllegalAccessException {
		assertEquals(24, allSpecs().size());
	}

	@Test
	public void testCreatablesFitTheirFeature() throws IllegalAccessException {
		for (Spec spec : allSpecs()) {
			assertTrue(spec.feature().isContainment(), spec.feature().getName());
			assertTrue(spec.feature().isMany(), spec.feature().getName());
			assertFalse(spec.creatables().isEmpty(), spec.feature().getName());
			for (EClass eClass : spec.eClasses()) {
				assertTrue(spec.feature().getEReferenceType().isSuperTypeOf(eClass),
						eClass.getName() + " in " + spec.feature().getName());
				assertFalse(eClass.isAbstract(), eClass.getName());
			}
			assertTrue(spec.eClasses().contains(spec.initialType()), spec.feature().getName());
		}
	}

	@Test
	public void testSingleTypeSpec() throws IllegalAccessException {
		Spec spec = VirtualListSpecs.ADDONS;
		Messages messages = messagesNamedAfterKeys();
		assertEquals("addons", spec.feature().getName());
		assertEquals(List.of("Addon"), names(spec.eClasses()));
		assertEquals("VApplicationAddons_TreeLabel", spec.label().apply(messages));
		assertEquals("VApplicationAddons_TreeLabelDescription", spec.description().apply(messages));
		assertEquals("VApplicationAddons_AddAddon", spec.creatables().get(0).label().apply(messages));
		assertFalse(spec.noGroup());
	}

	@Test
	public void testWindowSpecs() {
		for (Spec spec : List.of(VirtualListSpecs.APPLICATION_WINDOWS, VirtualListSpecs.WINDOW_WINDOWS,
				VirtualListSpecs.PERSPECTIVE_WINDOWS)) {
			assertEquals(List.of("TrimmedWindow", "Window"), names(spec.eClasses()));
			assertEquals("TrimmedWindow", spec.initialType().getName());
			assertTrue(spec.noGroup());
		}
		assertEquals("children", VirtualListSpecs.APPLICATION_WINDOWS.feature().getName());
		assertEquals("windows", VirtualListSpecs.WINDOW_WINDOWS.feature().getName());
		assertEquals("windows", VirtualListSpecs.PERSPECTIVE_WINDOWS.feature().getName());
	}

	@Test
	public void testTrimSpecs() {
		for (Spec spec : List.of(VirtualListSpecs.PART_TRIMS, VirtualListSpecs.PART_DESCRIPTOR_TRIMS,
				VirtualListSpecs.PERSPECTIVE_TRIMS, VirtualListSpecs.WINDOW_TRIMS)) {
			assertEquals(List.of("TrimBar"), names(spec.eClasses()));
			assertEquals("trimBars", spec.feature().getName());
			assertTrue(spec.noGroup());
		}
	}

	@Test
	public void testControlsSpec() {
		Spec spec = VirtualListSpecs.CONTROLS;
		assertEquals(List.of("PartSashContainer", "PartStack", "Part", "Area", "Placeholder"),
				names(spec.eClasses()));
		assertEquals("Part", spec.initialType().getName());
		assertEquals("PartStack", spec.contributionsFor().getName());
		assertEquals(MPart.class, spec.importType());
		assertEquals(List.of("Views", "Editors"),
				spec.imports().stream().map(i -> i.label().apply(null)).toList());
	}

	@Test
	public void testSharedElementsSpec() {
		Spec spec = VirtualListSpecs.SHARED_ELEMENTS;
		assertEquals("sharedElements", spec.feature().getName());
		assertEquals(List.of("PartSashContainer", "Part", "PartStack", "Area"), names(spec.eClasses()));
		assertEquals("Part", spec.initialType().getName());
		assertEquals(3, spec.imports().size());
	}

	@Test
	public void testWindowControlSpecsSortTheirActions() {
		assertTrue(VirtualListSpecs.WINDOW_CONTROLS.sortActions());
		assertEquals("PerspectiveStack", VirtualListSpecs.WINDOW_CONTROLS.initialType().getName());
		assertFalse(VirtualListSpecs.PERSPECTIVE_CONTROLS.sortActions());
		assertEquals("PartSashContainer", VirtualListSpecs.PERSPECTIVE_CONTROLS.initialType().getName());
	}

	@Test
	public void testSnippetsSpec() {
		Spec spec = VirtualListSpecs.SNIPPETS;
		assertEquals("snippets", spec.feature().getName());
		assertEquals(List.of("Area", "Part", "PartSashContainer", "PartStack", "CompositePart", "Perspective",
				"PerspectiveStack", "TrimContribution", "TrimmedWindow", "Window", "TrimBar"),
				names(spec.eClasses()));
		assertEquals("Perspective", spec.initialType().getName());
		assertTrue(spec.sortActions());
	}

	@Test
	public void testPartDescriptorsSpecImportsDescriptors() {
		Spec spec = VirtualListSpecs.PART_DESCRIPTORS;
		assertEquals("descriptors", spec.feature().getName());
		assertEquals(List.of("PartDescriptor"), names(spec.eClasses()));
		assertEquals(3, spec.imports().size());
	}

	@Test
	public void testFeaturesReadTheChildrenOfTheirContainer() throws IllegalAccessException {
		Set<Spec> checked = Collections.newSetFromMap(new IdentityHashMap<>());
		MApplication application = MApplicationFactory.INSTANCE.createApplication();
		assertSame(application.getAddons(), get(checked, VirtualListSpecs.ADDONS, application));
		assertSame(application.getCommands(), get(checked, VirtualListSpecs.COMMANDS, application));
		assertSame(application.getCategories(), get(checked, VirtualListSpecs.CATEGORIES, application));
		assertSame(application.getDescriptors(), get(checked, VirtualListSpecs.PART_DESCRIPTORS, application));
		assertSame(application.getBindingTables(), get(checked, VirtualListSpecs.BINDING_TABLES, application));
		assertSame(application.getRootContext(), get(checked, VirtualListSpecs.ROOT_CONTEXTS, application));
		assertSame(application.getHandlers(), get(checked, VirtualListSpecs.HANDLERS, application));
		assertSame(application.getMenuContributions(), get(checked, VirtualListSpecs.MENU_CONTRIBUTIONS, application));
		assertSame(application.getToolBarContributions(), get(checked, VirtualListSpecs.TOOLBAR_CONTRIBUTIONS, application));
		assertSame(application.getTrimContributions(), get(checked, VirtualListSpecs.TRIM_CONTRIBUTIONS, application));
		assertSame(application.getSnippets(), get(checked, VirtualListSpecs.SNIPPETS, application));
		assertSame(application.getChildren(), get(checked, VirtualListSpecs.APPLICATION_WINDOWS, application));

		MWindow window = MBasicFactory.INSTANCE.createWindow();
		assertSame(window.getSharedElements(), get(checked, VirtualListSpecs.SHARED_ELEMENTS, window));
		assertSame(window.getWindows(), get(checked, VirtualListSpecs.WINDOW_WINDOWS, window));
		assertSame(window.getChildren(), get(checked, VirtualListSpecs.WINDOW_CONTROLS, window));

		MPart part = MBasicFactory.INSTANCE.createPart();
		assertSame(part.getTrimBars(), get(checked, VirtualListSpecs.PART_TRIMS, part));

		MHandledItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
		assertSame(item.getParameters(), get(checked, VirtualListSpecs.PARAMETERS, item));

		MModelFragments fragments = MFragmentFactory.INSTANCE.createModelFragments();
		assertSame(fragments.getFragments(), get(checked, VirtualListSpecs.MODEL_FRAGMENTS, fragments));

		MTrimmedWindow trimmedWindow = MBasicFactory.INSTANCE.createTrimmedWindow();
		assertSame(trimmedWindow.getTrimBars(), get(checked, VirtualListSpecs.WINDOW_TRIMS, trimmedWindow));

		MPerspective perspective = MAdvancedFactory.INSTANCE.createPerspective();
		assertSame(perspective.getTrimBars(), get(checked, VirtualListSpecs.PERSPECTIVE_TRIMS, perspective));
		assertSame(perspective.getWindows(), get(checked, VirtualListSpecs.PERSPECTIVE_WINDOWS, perspective));
		assertSame(perspective.getChildren(), get(checked, VirtualListSpecs.PERSPECTIVE_CONTROLS, perspective));

		MPartDescriptor descriptor = org.eclipse.e4.ui.model.application.descriptor.basic.MBasicFactory.INSTANCE
				.createPartDescriptor();
		assertSame(descriptor.getTrimBars(), get(checked, VirtualListSpecs.PART_DESCRIPTOR_TRIMS, descriptor));

		MCompositePart compositePart = MBasicFactory.INSTANCE.createCompositePart();
		assertSame(compositePart.getChildren(), get(checked, VirtualListSpecs.CONTROLS, compositePart));

		assertEquals(allSpecs().size(), checked.size());
	}
}
