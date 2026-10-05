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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.e4.tools.emf.ui.internal.common.PasteTarget;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.CommandParameter;
import org.junit.jupiter.api.Test;

public class PasteTargetTest {

	private final MApplication application = MApplicationFactory.INSTANCE.createApplication();

	private MHandler handler() {
		final MHandler handler = MCommandsFactory.INSTANCE.createHandler();
		application.getHandlers().add(handler);
		return handler;
	}

	@Test
	public void testSiblingSelectedPastesAfterIt() {
		final MHandler first = handler();
		handler();

		final PasteTarget target = PasteTarget.find((EObject) first, (EObject) MCommandsFactory.INSTANCE.createHandler());

		assertSame(application, target.container());
		assertEquals("handlers", target.feature().getName());
		assertEquals(1, target.index());
	}

	@Test
	public void testOtherTypeSelectedWalksUpToAcceptingAncestor() {
		final MHandler handler = handler();
		final MCommand command = MCommandsFactory.INSTANCE.createCommand();

		final PasteTarget target = PasteTarget.find((EObject) handler, (EObject) command);

		assertSame(application, target.container());
		assertEquals("commands", target.feature().getName());
		assertEquals(CommandParameter.NO_INDEX, target.index());
	}

	@Test
	public void testContainerSelectedAppendsToChildren() {
		final MPartStack stack = MBasicFactory.INSTANCE.createPartStack();
		stack.getChildren().add(MBasicFactory.INSTANCE.createPart());
		final MPart part = MBasicFactory.INSTANCE.createPart();

		final PasteTarget target = PasteTarget.find((EObject) stack, (EObject) part);

		assertSame(stack, target.container());
		assertEquals("children", target.feature().getName());
		assertEquals(CommandParameter.NO_INDEX, target.index());
	}

	@Test
	public void testNoAcceptingAncestor() {
		final MPart part = MBasicFactory.INSTANCE.createPart();

		assertNull(PasteTarget.find((EObject) part, (EObject) MApplicationFactory.INSTANCE.createApplication()));
	}
}
