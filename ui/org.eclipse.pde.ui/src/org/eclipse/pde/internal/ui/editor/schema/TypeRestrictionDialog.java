/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.swt.events.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import java.util.Hashtable;

public class TypeRestrictionDialog extends Dialog {
	public static final String KEY_RESTRICTION_TYPE ="RestrictionDialog.type"; //$NON-NLS-1$
	private static final String T_ENUMERATION = "enumeration"; //$NON-NLS-1$
	private static final String T_NONE = "none"; //$NON-NLS-1$
	
	private String [] typeChoices = { T_NONE, T_ENUMERATION };
	private Combo typeCombo;
	private Hashtable pages = new Hashtable();
	private ISchemaRestriction restriction;
	private PageBook pageBook;

public TypeRestrictionDialog(Shell shell, ISchemaRestriction restriction) {
	super(shell);
	if (restriction!=null && restriction.getChildren().length>0)
		this.restriction = restriction;
}
protected void createButtonsForButtonBar(Composite parent) {
	// create OK and Cancel buttons by default
	createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
}
protected Control createDialogArea(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	container.setLayout(layout);
	GridData gd = new GridData(GridData.FILL_BOTH);
	container.setLayoutData(gd);

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_RESTRICTION_TYPE));
	gd = new GridData();
	label.setLayoutData(gd);
	typeCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
	initializeTypeCombo();
	typeCombo.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleTypeSelection();
		}
	});
	gd = new GridData(GridData.FILL_HORIZONTAL);
	typeCombo.setLayoutData(gd);

	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.SHADOW_OUT);
	label.setLayoutData(gd);
	
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;

	pageBook = new PageBook(container, SWT.NULL);
	pageBook.setLayoutData(gd);
	initializePages();
	WorkbenchHelp.setHelp(container, IHelpContextIds.SCHEMA_TYPE_RESTRICTION);
	return container;
}
public Object getValue() {
	return restriction;
}
private void handleTypeSelection() {
	String selection = typeCombo.getItem(typeCombo.getSelectionIndex());
	IRestrictionPage page = (IRestrictionPage)pages.get(selection);
	pageBook.showPage(page.getControl());
}
protected void initializePages() {
	IRestrictionPage page;
	IRestrictionPage pageToShow = null;
	String typeToShow = null;

	page = new NoRestrictionPage();
	page.createControl(pageBook);
	pages.put(T_NONE, page);
	if (restriction == null) {
		pageToShow = page;
		typeToShow = T_NONE;
	}

	page = new EnumerationRestrictionPage();
	page.createControl(pageBook);
	pages.put(T_ENUMERATION, page);
	if (restriction != null
		&& page.getCompatibleRestrictionClass().isInstance(restriction)) {
		pageToShow = page;
		typeToShow = T_ENUMERATION;
	}
	pageToShow.initialize(restriction);
	typeCombo.setText(typeToShow);
	pageBook.showPage(pageToShow.getControl());
}
protected void initializeTypeCombo() {
	typeCombo.setItems(typeChoices);
}
protected void okPressed() {
	String selectedRestriction = typeChoices[typeCombo.getSelectionIndex()];
	IRestrictionPage page = (IRestrictionPage)pages.get(selectedRestriction);
	restriction = page.getRestriction();
	super.okPressed();
}
}
