/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP - ongoing enhancements
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.trace.internal.datamodel.TracingCollections;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.eclipse.ui.trace.internal.utils.TracingUtils;

/**
 * An editing support object for the tracing UI viewer.
 */
public class TracingComponentColumnEditingSupport extends EditingSupport {

	/**
	 * Construct a new {@link TracingComponentColumnEditingSupport} for the specified index.
	 *
	 * @param viewer
	 *            The viewer to add this editing support
	 * @param index
	 *            The column index. One of either {@link TracingConstants#LABEL_COLUMN_INDEX} for the label column or
	 *            {@link TracingConstants#VALUE_COLUMN_INDEX} for the value column.
	 */
	public TracingComponentColumnEditingSupport(final ColumnViewer viewer, final int index) {

		super(viewer);
		this.columnIndex = index;
		switch (this.columnIndex) {
			case TracingConstants.VALUE_COLUMN_INDEX :
				this.textEditor = new TextCellEditor((Composite) viewer.getControl(), SWT.NONE);
				this.comboEditor = new ComboBoxCellEditor((Composite) viewer.getControl(), new String[] {Messages.TracingComponentColumnEditingSupport_true, Messages.TracingComponentColumnEditingSupport_false}, SWT.READ_ONLY | SWT.SIMPLE);
				this.comboEditor.setActivationStyle(ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION | ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
				break;
			default :
				// do nothing - no other columns provide editing support
				this.textEditor = null;
				this.comboEditor = null;
		}
	}

	@Override
	protected boolean canEdit(final Object element) {

		boolean canEdit = false;
		switch (this.columnIndex) {
			case TracingConstants.VALUE_COLUMN_INDEX :
				return true;
			default :
				// do nothing - no other columns provide editing support
				canEdit = false;
		}
		return canEdit;
	}

	@Override
	protected CellEditor getCellEditor(final Object element) {

		if (element instanceof TracingComponentDebugOption) {
			TracingComponentDebugOption option = (TracingComponentDebugOption) element;
			if (TracingUtils.isValueBoolean(option.getOptionPathValue())) {
				return this.comboEditor;
			}
			return this.textEditor;
		}
		return null;
	}

	@Override
	protected Object getValue(final Object element) {

		Object value = null;
		switch (this.columnIndex) {
			case 1 :
				if (element instanceof TracingComponentDebugOption) {
					String optionPathValue = ((TracingComponentDebugOption) element).getOptionPathValue();
					if (TracingUtils.isValueBoolean(optionPathValue)) {
						value = Boolean.parseBoolean(optionPathValue) ? 0 : 1;
					} else {
						value = optionPathValue;
					}
				} else if (element instanceof String) {
					value = element;
				}
				break;
			default :
				// do nothing - no other columns provide editing support
		}
		return value;
	}

	@Override
	protected void setValue(final Object element, final Object value) {

		switch (this.columnIndex) {
			case 1 :
				if (element instanceof TracingComponentDebugOption) {
					TracingComponentDebugOption option = (TracingComponentDebugOption) element;
					String updatedValue = String.valueOf(value);
					if (value instanceof Integer) {
						updatedValue = String.valueOf((Integer) value == 0);
					}
					if (option.getOptionPathValue().equals(updatedValue)) {
						return; // nothing changed nothing to do
					}
					// find identical debug options and update them (this will include 'this' debug option that was
					// modified)
					TracingComponentDebugOption[] identicalOptions = TracingCollections.getInstance().getTracingDebugOptions(option.getOptionPath());
					for (TracingComponentDebugOption identicalOption : identicalOptions) {
						TracingCollections.getInstance().getModifiedDebugOptions().removeDebugOption(identicalOption.clone());
						identicalOption.setOptionPathValue(updatedValue);
						TracingCollections.getInstance().getModifiedDebugOptions().addDebugOption(identicalOption);
						this.getViewer().update(identicalOption, null);
					}
				}
				break;
			default :
				// do nothing - no other columns provide editing support
		}
	}

	/**
	 * The column index of the editors. One of {@link TracingConstants#LABEL_COLUMN_INDEX} or
	 * {@link TracingConstants#VALUE_COLUMN_INDEX}
	 */
	private int columnIndex;

	/**
	 * The {@link CellEditor} for the value column
	 */
	private CellEditor textEditor;
	private ComboBoxCellEditor comboEditor;

}