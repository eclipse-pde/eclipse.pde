/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.util.SWTUtil;
/**
 * @version 	1.0
 * @author
 */
public class WizardCheckboxTablePart extends CheckboxTablePart {
	public static final String KEY_SELECT_ALL = "WizardCheckboxTablePart.selectAll";
	public static final String KEY_DESELECT_ALL =
		"WizardCheckboxTablePart.deselectAll";
	public static final String KEY_COUNTER = "WizardCheckboxTablePart.counter";

	private int selectAllIndex = -1;
	private int deselectAllIndex = -1;
	private String tableName;
	private int counter;
	private Label counterLabel;

	/**
	 * Constructor for WizardCheckboxTablePart.
	 * @param buttonLabels
	 */
	public WizardCheckboxTablePart(String tableName, String[] buttonLabels) {
		super(buttonLabels);
		this.tableName = tableName;
	}

	public WizardCheckboxTablePart(String mainLabel) {
		this(
			mainLabel,
			new String[] {
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				PDEPlugin.getResourceString(KEY_DESELECT_ALL)});
		setSelectAllIndex(0);
		setDeselectAllIndex(1);
	}

	public void setSelectAllIndex(int index) {
		this.selectAllIndex = index;
	}
	public void setDeselectAllIndex(int index) {
		this.deselectAllIndex = index;
	}

	protected void buttonSelected(Button button, int index) {
		if (index == selectAllIndex) {
			handleSelectAll(true);
		}
		if (index == deselectAllIndex) {
			handleSelectAll(false);
		}
	}

	public Object[] getSelection() {
		CheckboxTableViewer viewer = getTableViewer();
		return viewer.getCheckedElements();
	}

	public void setSelection(Object[] selected) {
		CheckboxTableViewer viewer = getTableViewer();
		viewer.setCheckedElements(selected);
		updateCounter(viewer.getCheckedElements().length);
	}

	public void createControl(Composite parent) {
		createControl(parent, SWT.NULL, 2, null);
		counterLabel = new Label(parent, SWT.NULL);
		GridData gd =
			new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		updateCounter(0);
	}

	protected Button createButton(
		Composite parent,
		String label,
		int index,
		FormWidgetFactory factory) {
		Button button = super.createButton(parent, label, index, factory);
		SWTUtil.setButtonDimensionHint(button);
		return button;
	}

	protected StructuredViewer createStructuredViewer(
		Composite parent,
		int style,
		FormWidgetFactory factory) {
		StructuredViewer viewer = super.createStructuredViewer(parent, style, factory);
		viewer.setSorter(ListUtil.NAME_SORTER);
		return viewer;
	}

	protected void createMainLabel(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		if (tableName == null)
			return;
		Label label = new Label(parent, SWT.NULL);
		label.setText(tableName);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}

	protected void updateCounter(int amount) {
		counter = amount;
		updateCounterLabel();
	}

	protected void updateCounterLabel() {
		String number = "" + getSelectionCount();
		String totalNumber = "" + getTotalCount();
		String message =
			PDEPlugin.getFormattedMessage(
				KEY_COUNTER,
				new String[] { number, totalNumber });
		counterLabel.setText(message);
	}

	public int getSelectionCount() {
		return counter;
	}

	public void selectAll(boolean select) {
		handleSelectAll(select);
	}

	private int getTotalCount() {
		CheckboxTableViewer viewer = getTableViewer();
		return viewer.getTable().getItemCount();
	}

	protected void handleSelectAll(boolean select) {
		CheckboxTableViewer viewer = getTableViewer();
		viewer.setAllChecked(select);
		int selected;
		if (!select) {
			selected = 0;
		} else {
			selected = getTotalCount();
		}
		updateCounter(selected);
	}
	protected void elementChecked(Object element, boolean checked) {
		int count = getSelectionCount();
		updateCounter(checked ? count + 1 : count - 1);
	}
}
