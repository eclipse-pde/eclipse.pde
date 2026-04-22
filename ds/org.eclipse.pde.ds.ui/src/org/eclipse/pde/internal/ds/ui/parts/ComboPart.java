/*******************************************************************************
 * Copyright (c) 2008, 2026 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.parts;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ComboPart {

	protected Combo combo;

	public ComboPart() {
	}

	public void addSelectionListener(SelectionListener listener) {
		combo.addSelectionListener(listener);
	}

	public int indexOf(String item) {
		return combo.indexOf(item);
	}

	public void addModifyListener(ModifyListener listener) {
		combo.addModifyListener(listener);
	}

	public void createControl(Composite parent, FormToolkit toolkit, int style) {
		combo = new Combo(parent, style | toolkit.getBorderStyle());
		toolkit.adapt(combo, true, false);
	}

	public Control getControl() {
		return combo;
	}

	public int getSelectionIndex() {
		return combo.getSelectionIndex();
	}

	public void add(String item, int index) {
		combo.add(item, index);
	}

	public void add(String item) {
		combo.add(item);
	}

	public void remove(int index) {
		if ((index < 0) || (index >= getItemCount())) {
			return;
		}
		combo.remove(index);
	}

	public void select(int index) {
		combo.select(index);
	}

	public String getSelection() {
		return combo.getText().trim();
	}

	public void setText(String text) {
		combo.setText(text);
	}

	public void setItems(String[] items) {
		combo.setItems(items);
	}

	public void setEnabled(boolean enabled) {
		combo.setEnabled(enabled);
	}

	public int getItemCount() {
		return combo.getItemCount();
	}

	public String[] getItems() {
		return combo.getItems();
	}
}
