/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.Vector;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaStringAttributeDetails extends SchemaAttributeDetails {
	private Button fTransTrue;
	private Button fTransFalse;
	private TableViewer fRestrictionsTable;
	private Button fAddRestriction;
	private Button fRemoveRestriction;

	public SchemaStringAttributeDetails(ElementSection section) {
		super(section);
	}

	protected void createTypeDetails(Composite parent, FormToolkit toolkit) {
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);

		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_translatable);
		label.setForeground(foreground);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 11;
		gd.verticalIndent = 2;
		label.setLayoutData(gd);
		Button[] buttons = createTrueFalseButtons(parent, toolkit, 2);
		fTransTrue = buttons[0];
		fTransFalse = buttons[1];

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_restrictions);
		label.setForeground(foreground);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 11;
		gd.verticalIndent = 2;
		label.setLayoutData(gd);

		Composite tableComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		tableComp.setLayout(layout);
		tableComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Table table = toolkit.createTable(tableComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		table.setLayoutData(gd);
		fRestrictionsTable = new TableViewer(table);
		fRestrictionsTable.setContentProvider(new SchemaAttributeContentProvider());
		fRestrictionsTable.setLabelProvider(new LabelProvider());

		Composite resButtonComp = toolkit.createComposite(parent);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		resButtonComp.setLayout(layout);
		resButtonComp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fAddRestriction = toolkit.createButton(resButtonComp, PDEUIMessages.SchemaAttributeDetails_addRestButton, SWT.NONE);
		fRemoveRestriction = toolkit.createButton(resButtonComp, PDEUIMessages.SchemaAttributeDetails_removeRestButton, SWT.NONE);
		fAddRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		super.updateFields(object);

		fTransTrue.setSelection(getAttribute().isTranslatable());
		fTransFalse.setSelection(!getAttribute().isTranslatable());
		fRestrictionsTable.setInput(new Object());

		boolean editable = isEditableElement();
		fTransTrue.setEnabled(editable);
		fTransFalse.setEnabled(editable);
		fRestrictionsTable.getControl().setEnabled(editable);
		fAddRestriction.setEnabled(editable);
		fRemoveRestriction.setEnabled(!fRestrictionsTable.getSelection().isEmpty() && editable);
	}

	public void hookListeners() {
		super.hookListeners();
		fTransTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				getAttribute().setTranslatableProperty(fTransTrue.getSelection());
			}
		});
		fAddRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				NewRestrictionDialog dialog = new NewRestrictionDialog(getPage().getSite().getShell());
				if (dialog.open() != Window.OK)
					return;
				String text = dialog.getNewRestriction();
				if (text != null && text.length() > 0) {
					ISchemaSimpleType type = getAttribute().getType();
					ChoiceRestriction res = (ChoiceRestriction) type.getRestriction();
					Vector vres = new Vector();
					if (res != null) {
						Object[] currRes = res.getChildren();
						for (int i = 0; i < currRes.length; i++) {
							vres.add(currRes[i]);
						}
					}
					vres.add(new SchemaEnumeration(getAttribute().getSchema(), text));
					if (res == null)
						res = new ChoiceRestriction(getAttribute().getSchema());
					res.setChildren(vres);
					if (type instanceof SchemaSimpleType)
						((SchemaSimpleType) type).setRestriction(res);
					fRestrictionsTable.refresh();
				}
			}
		});
		fRemoveRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				ISelection selection = fRestrictionsTable.getSelection();
				if (selection.isEmpty())
					return;
				if (!(selection instanceof IStructuredSelection))
					return;
				IStructuredSelection sselection = (IStructuredSelection) selection;
				Object[] aselection = sselection.toArray();
				ISchemaSimpleType type = getAttribute().getType();
				ChoiceRestriction res = (ChoiceRestriction) type.getRestriction();
				Vector vres = new Vector();
				if (res != null) {
					Object[] currRes = res.getChildren();
					for (int i = 0; i < currRes.length; i++) {
						boolean stays = true;
						for (int j = 0; j < aselection.length; j++) {
							if (currRes[i].equals(aselection[j]))
								stays = false;
						}
						if (stays)
							vres.add(currRes[i]);
					}
					res.setChildren(vres);
					if (type instanceof SchemaSimpleType) {
						if (vres.size() == 0)
							((SchemaSimpleType) type).setRestriction(null);
						else
							((SchemaSimpleType) type).setRestriction(res);
					}
					fRestrictionsTable.refresh();
				}
			}
		});
		fRestrictionsTable.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (blockListeners())
					return;
				fRemoveRestriction.setEnabled(getAttribute().getSchema().isEditable() && !event.getSelection().isEmpty());
			}
		});
	}
}
