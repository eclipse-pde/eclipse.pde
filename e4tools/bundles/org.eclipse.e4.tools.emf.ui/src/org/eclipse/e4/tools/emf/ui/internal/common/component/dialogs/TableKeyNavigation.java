/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * Moves the focus between a search field and the result table with the arrow keys.
 */
public final class TableKeyNavigation {

	private TableKeyNavigation() {
	}

	/** Arrow down in the field selects the first row of the table. The supplier copes with rebuilt tables. */
	public static void installOnText(Text text, Supplier<Table> table) {
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN && table.get().getItemCount() > 0) {
					table.get().setFocus();
					table.get().select(0);
				}
			}
		});
	}

	/** Arrow up on the first row returns the focus to the field. */
	public static void installOnTable(Table table, Text text) {
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP && table.getSelectionIndex() == 0) {
					text.setFocus();
				}
			}
		});
	}
}
