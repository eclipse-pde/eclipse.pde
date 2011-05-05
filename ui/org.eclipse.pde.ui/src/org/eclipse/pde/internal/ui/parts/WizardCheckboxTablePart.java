/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class WizardCheckboxTablePart extends CheckboxTablePart {
	private int selectAllIndex = -1;
	private int deselectAllIndex = -1;
	private int selectIndex = -1;
	private int deselectIndex = -1;
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
		this(mainLabel, new String[] {PDEUIMessages.WizardCheckboxTablePart_selectAll, PDEUIMessages.WizardCheckboxTablePart_deselectAll, PDEUIMessages.WizardCheckboxTablePart_select, PDEUIMessages.WizardCheckboxTablePart_deselect});
		setSelectAllIndex(0);
		setDeselectAllIndex(1);
		setSelectIndex(2);
		setDeselectIndex(3);
	}

	public void setSelectAllIndex(int index) {
		this.selectAllIndex = index;
	}

	public void setDeselectAllIndex(int index) {
		this.deselectAllIndex = index;
	}

	public void setSelectIndex(int index) {
		this.selectIndex = index;
	}

	public void setDeselectIndex(int index) {
		this.deselectIndex = index;
	}

	protected void buttonSelected(Button button, int index) {
		if (index == selectAllIndex) {
			handleSelectAll(true);
		}
		if (index == deselectAllIndex) {
			handleSelectAll(false);
		}
		if (index == selectIndex) {
			handleSelect(true);
		}
		if (index == deselectIndex) {
			handleSelect(false);
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
		createControl(parent, 2);
	}

	public void createControl(Composite parent, int span) {
		createControl(parent, SWT.NULL, span, null);
		counterLabel = new Label(parent, SWT.NULL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		counterLabel.setLayoutData(gd);
		updateCounter(0);
	}

	public void createControl(Composite parent, int span, boolean multiselect) {
		if (multiselect) {
			createControl(parent, SWT.MULTI, span, null);
		} else {
			createControl(parent, SWT.NULL, span, null);
		}
		counterLabel = new Label(parent, SWT.NULL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		counterLabel.setLayoutData(gd);
		updateCounter(0);
	}

	protected Button createButton(Composite parent, String label, int index, FormToolkit toolkit) {
		Button button = super.createButton(parent, label, index, toolkit);
		SWTUtil.setButtonDimensionHint(button);
		return button;
	}

	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
		viewer.setComparator(ListUtil.NAME_COMPARATOR);
		return viewer;
	}

	protected void createMainLabel(Composite parent, int span, FormToolkit toolkit) {
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

	public void updateCount(int amount) {
		updateCounter(amount);
	}

	protected void updateCounterLabel() {
		String number = "" + getSelectionCount(); //$NON-NLS-1$
		String totalNumber = "" + getTotalCount(); //$NON-NLS-1$
		String message = NLS.bind(PDEUIMessages.WizardCheckboxTablePart_counter, (new String[] {number, totalNumber}));
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

	protected void handleSelect(boolean select) {
		CheckboxTableViewer viewer = getTableViewer();
		if (viewer.getTable().getSelection().length > 0) {
			TableItem[] selected = viewer.getTable().getSelection();
			for (int i = 0; i < selected.length; i++) {
				TableItem item = selected[i];
				item.setChecked(select);
			}
			updateCounter(viewer.getCheckedElements().length);
		}
	}

	protected void elementChecked(Object element, boolean checked) {
		int count = getSelectionCount();
		updateCounter(checked ? count + 1 : count - 1);
	}

	public Label getCounterLabel() {
		return counterLabel;
	}
}
