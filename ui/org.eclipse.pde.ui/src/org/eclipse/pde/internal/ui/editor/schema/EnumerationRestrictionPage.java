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

import java.util.Vector;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;

public class EnumerationRestrictionPage implements IRestrictionPage {
	public static final String KEY_CHOICES =
		"RestrictionDialog.choices"; //$NON-NLS-1$
	public static final String KEY_NEW_CHOICE =
		"RestrictionDialog.newChoice"; //$NON-NLS-1$
	public static final String KEY_ADD =
		"RestrictionDialog.add"; //$NON-NLS-1$
	public static final String KEY_REMOVE =
		"RestrictionDialog.remove"; //$NON-NLS-1$
	private List choiceList;
	private Button addButton;
	private Button deleteButton;
	private Text text;
	private Control control;

	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());

		Composite top = new Composite(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		Label label = new Label(top, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_NEW_CHOICE));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		text = new Text(top, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		addButton = new Button(top, SWT.PUSH);
		addButton.setText(PDEPlugin.getResourceString(KEY_ADD));
		addButton.setEnabled(false);
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(addButton);
		

		Composite bottom = new Composite(container, SWT.NULL);
		bottom.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		bottom.setLayout(layout);

		label = new Label(bottom, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		label.setText(PDEPlugin.getResourceString(KEY_CHOICES));
		
		choiceList = new List(bottom, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		choiceList.setLayoutData(new GridData(GridData.FILL_BOTH));

		deleteButton = new Button(bottom, SWT.PUSH);
		deleteButton.setText(PDEPlugin.getResourceString(KEY_REMOVE));
		deleteButton.setEnabled(false);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});
		deleteButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(deleteButton);

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String item = text.getText();
				boolean canAdd = true;
				if (item.length() == 0 || choiceList.indexOf(item) != -1)
					canAdd = false;
				addButton.setEnabled(canAdd);
			}
		});
		text.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				handleAdd();
				e.doit = false;
			}
		});

		choiceList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deleteButton.setEnabled(choiceList.getSelectionCount() > 0);
				if (choiceList.getSelectionCount() == 1) {
					text.setText(choiceList.getSelection()[0]);
				}
			}
		});
		this.control = container;
		return container;
	}
	public Class getCompatibleRestrictionClass() {
		return ChoiceRestriction.class;
	}
	public org.eclipse.swt.widgets.Control getControl() {
		return control;
	}
	public ISchemaRestriction getRestriction() {
		ChoiceRestriction restriction = new ChoiceRestriction((ISchema) null);
		String[] items = choiceList.getItems();
		if (items.length > 0) {
			Vector enums = new Vector();
			for (int i = 0; i < items.length; i++) {
				enums.addElement(new SchemaEnumeration(restriction, items[i]));
			}
			restriction.setChildren(enums);
		}
		return restriction;
	}
	private void handleAdd() {
		String item = text.getText().trim();
		if (item.length()==0) return;
		choiceList.add(item);
		choiceList.setSelection(new String[] { item });
		text.setText(""); //$NON-NLS-1$
		deleteButton.setEnabled(true);
	}

	private void handleDelete() {
		String[] selection = choiceList.getSelection();
		choiceList.setRedraw(false);
		for (int i = 0; i < selection.length; i++) {
			choiceList.remove(selection[i]);
		}
		choiceList.setRedraw(true);
		deleteButton.setEnabled(false);
	}
	
	public void initialize(ISchemaRestriction restriction) {
		if (restriction != null) {
			Object[] children = restriction.getChildren();
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				if (child instanceof ISchemaEnumeration) {
					choiceList.add(child.toString());
				}
			}
		}
	}
}
