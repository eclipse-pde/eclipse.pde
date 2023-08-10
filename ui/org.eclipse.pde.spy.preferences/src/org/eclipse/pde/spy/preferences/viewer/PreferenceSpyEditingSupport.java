/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.viewer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry.Fields;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class PreferenceSpyEditingSupport extends EditingSupport {

	private Fields field;

	public PreferenceSpyEditingSupport(ColumnViewer viewer, Fields field) {
		super(viewer);
		this.field = field;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return new TextCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY);
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		String value = null;
		if (element instanceof PreferenceEntry preferenceEntry) {
			switch (field) {
			case nodePath:
				value = preferenceEntry.getNodePath();
				break;
			case key:
				value = preferenceEntry.getKey();
				break;
			case oldValue:
				value = preferenceEntry.getOldValue();
				break;
			case newValue:
				value = preferenceEntry.getNewValue();
				break;
			default:
				break;
			}
		}

		return value;
	}

	@Override
	protected void setValue(Object element, Object value) {
	}

}
