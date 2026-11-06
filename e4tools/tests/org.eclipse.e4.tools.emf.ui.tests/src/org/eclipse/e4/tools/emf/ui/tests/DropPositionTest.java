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

import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_AFTER;
import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_BEFORE;
import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_ON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.tools.emf.ui.internal.common.DropPosition;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.fragment.MFragmentFactory;
import org.eclipse.e4.ui.model.fragment.MModelFragments;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.Test;

public class DropPositionTest {

	private static final int ROW = 24;

	@Test
	public void siblingOnlyRowIsSplitInHalves() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(0, ROW, false, false));
		assertEquals(LOCATION_BEFORE, DropPosition.location(11, ROW, false, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(12, ROW, false, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(23, ROW, false, false));
	}

	@Test
	public void containerRowIsSplitInThirds() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(7, ROW, true, false));
		assertEquals(LOCATION_ON, DropPosition.location(8, ROW, true, false));
		assertEquals(LOCATION_ON, DropPosition.location(15, ROW, true, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(16, ROW, true, false));
	}

	@Test
	public void expandedRowHasNoAfterZone() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(7, ROW, true, true));
		assertEquals(LOCATION_ON, DropPosition.location(23, ROW, true, true));
		assertEquals(LOCATION_BEFORE, DropPosition.location(23, ROW, false, true));
	}

	@Test
	public void indexMatchesTheIndicatedSlotWhenMovingWithinTheList() {
		for (int from = 0; from < 4; from++) {
			for (int target = 0; target < 4; target++) {
				assertMove(from, target, false);
				assertMove(from, target, true);
			}
		}
	}

	private static void assertMove(int from, int target, boolean after) {
		final List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
		final String moved = list.get(from);
		final String targetElement = list.get(target);
		final List<String> expected = new ArrayList<>(list);
		if (!moved.equals(targetElement)) {
			expected.remove(moved);
			expected.add(expected.indexOf(targetElement) + (after ? 1 : 0), moved);
		}
		list.remove(from);
		list.add(DropPosition.index(target, after, from), moved);
		assertEquals(expected, list, () -> moved + (after ? " after " : " before ") + targetElement);
	}

	@Test
	public void indexFromAnotherListIsTheInsertionPoint() {
		assertEquals(2, DropPosition.index(2, false, -1));
		assertEquals(3, DropPosition.index(2, true, -1));
	}

	@Test
	public void unknownTargetStaysNegative() {
		assertEquals(-1, DropPosition.index(-1, true, 2));
	}

	private static EStructuralFeature feature(Object owner, String name) {
		return ((EObject) owner).eClass().getEStructuralFeature(name);
	}

	private static MCommand command(MApplication application) {
		final MCommand command = MCommandsFactory.INSTANCE.createCommand();
		command.setElementId("copy");
		command.setCommandName("Copy");
		application.getCommands().add(command);
		return command;
	}

	@Test
	public void elementOfSameModelIsMoved() {
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final MCommand command = command(application);

		assertSame(command, DropPosition.element((EObject) command, (EObject) application,
				feature(application, "commands"), false));
	}

	@Test
	public void elementMovedWithinModelIsNotCopied() {
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final MHandler handler = MCommandsFactory.INSTANCE.createHandler();
		application.getHandlers().add(handler);
		final MPart part = MBasicFactory.INSTANCE.createPart();
		application.getChildren().add(MBasicFactory.INSTANCE.createTrimmedWindow());
		application.getChildren().get(0).getChildren().add(part);

		assertSame(handler, DropPosition.element((EObject) handler, (EObject) part, feature(part, "handlers"), false));
	}

	@Test
	public void copyToOtherModelDropsReferences() {
		final MApplication source = MApplicationFactory.INSTANCE.createApplication();
		final MHandler handler = MCommandsFactory.INSTANCE.createHandler();
		handler.setCommand(command(source));
		source.getHandlers().add(handler);
		final MApplication target = MApplicationFactory.INSTANCE.createApplication();

		final EObject dropped = DropPosition.element((EObject) handler, (EObject) target,
				feature(target, "handlers"), false);

		assertNotSame(handler, dropped);
		assertNull(((MHandler) dropped).getCommand());
	}

	@Test
	public void elementOfOtherModelIsCopied() {
		final MApplication source = MApplicationFactory.INSTANCE.createApplication();
		final MCommand command = command(source);
		final MApplication target = MApplicationFactory.INSTANCE.createApplication();

		final EObject dropped = DropPosition.element((EObject) command, (EObject) target,
				feature(target, "commands"), false);

		assertNotSame(command, dropped);
		assertEquals("Copy", ((MCommand) dropped).getCommandName());
		assertSame(source, ((EObject) command).eContainer());
	}

	@Test
	public void elementDroppedOnImportsBecomesPlaceholder() {
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final MCommand command = command(application);
		final MModelFragments fragments = MFragmentFactory.INSTANCE.createModelFragments();

		final EObject dropped = DropPosition.element((EObject) command, (EObject) fragments,
				feature(fragments, "imports"), false);

		assertEquals("copy", ((MCommand) dropped).getElementId());
		assertNull(((MCommand) dropped).getCommandName());
		assertSame(application, ((EObject) command).eContainer());
	}

	@Test
	public void importDroppedOnImportsIsMoved() {
		final MModelFragments fragments = MFragmentFactory.INSTANCE.createModelFragments();
		final MCommand placeholder = MCommandsFactory.INSTANCE.createCommand();
		fragments.getImports().add(placeholder);

		assertSame(placeholder, DropPosition.element((EObject) placeholder, (EObject) fragments,
				feature(fragments, "imports"), false));
	}

	@Test
	public void copyWithinModelKeepsOriginalAndReferences() {
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final MHandler handler = MCommandsFactory.INSTANCE.createHandler();
		handler.setCommand(command(application));
		application.getHandlers().add(handler);

		final EObject dropped = DropPosition.element((EObject) handler, (EObject) application,
				feature(application, "handlers"), true);

		assertNotSame(handler, dropped);
		assertSame(handler.getCommand(), ((MHandler) dropped).getCommand());
		assertSame(application, ((EObject) handler).eContainer());
	}

	@Test
	public void copyDroppedOnImportsBecomesPlaceholder() {
		final MApplication application = MApplicationFactory.INSTANCE.createApplication();
		final MCommand command = command(application);
		final MModelFragments fragments = MFragmentFactory.INSTANCE.createModelFragments();

		final EObject dropped = DropPosition.element((EObject) command, (EObject) fragments,
				feature(fragments, "imports"), true);

		assertEquals("copy", ((MCommand) dropped).getElementId());
		assertNull(((MCommand) dropped).getCommandName());
	}
}
