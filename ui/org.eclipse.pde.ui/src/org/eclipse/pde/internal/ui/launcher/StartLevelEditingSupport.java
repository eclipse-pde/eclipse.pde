/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;

class StartLevelEditingSupport extends EditingSupport {

	private final CachedCheckboxTreeViewer fViewer;
	private final CellEditor fCellEditor;

	public StartLevelEditingSupport(CachedCheckboxTreeViewer viewer) {
		super(viewer);
		fViewer = viewer;
		fCellEditor = new StartLevelCellEditor(viewer.getTree());
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return fCellEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return fViewer.isCheckedLeafElement(element) && element instanceof IHasStartLevel;
	}

	@Override
	protected Object getValue(Object element) {
		return ((IHasStartLevel) element).getStartLevel();
	}

	@Override
	protected void setValue(Object element, Object value) {
		((IHasStartLevel) element).setStartLevel((String) value);
		fViewer.update(element, null);
	}

	public interface IHasStartLevel {
		String getStartLevel();

		void setStartLevel(String level);
	}

	private static class StartLevelCellEditor extends CellEditor {

		private Spinner fSpinner;

		public StartLevelCellEditor(Composite parent) {
			create(parent);
		}

		@Override
		protected Control createControl(Composite parent) {
			fSpinner = new Spinner(parent, SWT.BORDER);
			fSpinner.setMinimum(0);

			fSpinner.addTraverseListener(e -> {
				if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			});
			fSpinner.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(e -> {
				fireApplyEditorValue();
				deactivate();
			}));
			fSpinner.addKeyListener(KeyListener.keyReleasedAdapter(e -> {
				if (e.character == SWT.ESC) {
					fireCancelEditor();
				}
			}));
			return fSpinner;
		}

		@Override
		protected Object doGetValue() {
			int startLevel = fSpinner.getSelection();
			return (startLevel > 0) ? String.valueOf(startLevel) : "default"; //$NON-NLS-1$
		}

		@Override
		protected void doSetFocus() {
			fSpinner.setFocus();
		}

		@Override
		protected void doSetValue(Object value) {
			if (value instanceof String stringValue) {
				int level = BundleLauncherHelper.parseAutoStartLevel(stringValue);
				fSpinner.setSelection(level);
			}
		}

	}

}
