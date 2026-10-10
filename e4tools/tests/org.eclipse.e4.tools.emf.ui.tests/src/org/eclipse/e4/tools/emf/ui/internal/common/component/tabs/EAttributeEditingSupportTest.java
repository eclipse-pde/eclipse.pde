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

package org.eclipse.e4.tools.emf.ui.internal.common.component.tabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EAttributeEditingSupportTest {

	private Shell shell;
	private TableViewer viewer;

	@BeforeEach
	public void setUp() {
		shell = new Shell(Display.getDefault());
		viewer = new TableViewer(shell, SWT.NONE);
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void testTextEditorIsReused() {
		final EAttributeEditingSupport support = new EAttributeEditingSupport(viewer, "label", null); //$NON-NLS-1$
		final MPart part = MBasicFactory.INSTANCE.createPart();
		final int children = viewer.getTable().getChildren().length;

		assertSame(support.getCellEditor(part), support.getCellEditor(part));
		assertEquals(children + 1, viewer.getTable().getChildren().length);
	}

	@Test
	public void testCheckboxEditorIsReused() {
		final EAttributeEditingSupport support = new EAttributeEditingSupport(viewer, "toBeRendered", null); //$NON-NLS-1$
		final MPart part = MBasicFactory.INSTANCE.createPart();

		assertSame(support.getCellEditor(part), support.getCellEditor(part));
	}
}
