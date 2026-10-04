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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.TableViewerUtil;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableViewerUtilTest {

	private Shell shell;
	private TableViewer viewer;
	private TableColumn col0;
	private TableColumn col1;
	private TableColumn col2;

	@BeforeEach
	public void setUp() {
		shell = new Shell(Display.getDefault());
		viewer = new TableViewer(shell, SWT.NONE);
		col0 = new TableColumn(viewer.getTable(), SWT.NONE);
		col1 = new TableColumn(viewer.getTable(), SWT.NONE);
		col2 = new TableColumn(viewer.getTable(), SWT.NONE);
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void testColumnsInDisplayOrder() {
		assertEquals(List.of(col0, col1, col2), TableViewerUtil.getColumnsInDisplayOrder(viewer));

		viewer.getTable().setColumnOrder(new int[] { 2, 0, 1 });
		assertEquals(List.of(col2, col0, col1), TableViewerUtil.getColumnsInDisplayOrder(viewer));
	}

	@Test
	public void testMoveColumnToEnd() {
		final Table table = viewer.getTable();
		table.setColumnOrder(new int[] { 2, 0, 1 });

		TableViewerUtil.moveColumnToEnd(viewer, col2);
		assertArrayEquals(new int[] { 0, 1, 2 }, table.getColumnOrder());

		TableViewerUtil.moveColumnToEnd(viewer, col0);
		assertArrayEquals(new int[] { 1, 2, 0 }, table.getColumnOrder());

		TableViewerUtil.moveColumnToEnd(viewer, col0);
		assertArrayEquals(new int[] { 1, 2, 0 }, table.getColumnOrder());
	}
}
