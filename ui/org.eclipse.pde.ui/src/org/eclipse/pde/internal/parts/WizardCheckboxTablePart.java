/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.parts;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.wizards.ListUtil;
/**
 * @version 	1.0
 * @author
 */
public class WizardCheckboxTablePart extends CheckboxTablePart {
	public static final String KEY_SELECT_ALL = "WizardCheckboxTablePart.selectAll";
	public static final String KEY_DESELECT_ALL = "WizardCheckboxTablePart.deselectAll";
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
		this(mainLabel, new String [] {
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				PDEPlugin.getResourceString(KEY_DESELECT_ALL) });
		setSelectAllIndex(0);
		setDeselectAllIndex(1);
	}
	
	public void setSelectAllIndex(int index) {
		this.selectAllIndex = index;
	}
	public void setDeselectAllIndex(int index) {
		this.deselectAllIndex = index;
	}
	
	public void buttonSelected(int index) {
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
	
	public void setSelection(Object [] selected) {
		CheckboxTableViewer viewer = getTableViewer();
		viewer.setCheckedElements(selected);
		counter = selected.length;
		updateCounterLabel();
	}
	
	public void createControl(Composite parent) {
		createControl(parent, 2, null);
		counterLabel = new Label(parent, SWT.NULL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		updateCounterLabel();
	}
	
	protected StructuredViewer createStructuredViewer(
		Composite parent,
		FormWidgetFactory factory) {
		StructuredViewer viewer = super.createStructuredViewer(parent, factory);
		viewer.setSorter(ListUtil.NAME_SORTER);
		return viewer;
	}	
	
	protected void createMainLabel(Composite parent, int span, FormWidgetFactory factory) {
		if (tableName==null) return;
		Label label = new Label(parent, SWT.NULL);
		label.setText(tableName);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}
	
	private void updateCounter() {
		Object [] selection = getSelection();
		counter = selection.length;
		updateCounterLabel();
	}
	
	private void updateCounter(int delta) {
		counter += delta;
		updateCounterLabel();
	}
	
	protected void updateCounterLabel() {
		String number = ""+counter;
		String message = PDEPlugin.getFormattedMessage(KEY_COUNTER, number);
		counterLabel.setText(message);
	}
	
	public int getSelectionCount() {
		return counter;
	}
	
	protected void handleSelectAll(boolean select) {
		CheckboxTableViewer viewer = getTableViewer();
		viewer.setAllChecked(select);
		if (!select) {
			counter = 0;
		}
		else {
			counter = viewer.getTable().getItemCount();
		}
		updateCounterLabel();
	}
	protected void elementChecked(Object element, boolean checked) {
		updateCounter(checked ? 1 : -1);
	}
}