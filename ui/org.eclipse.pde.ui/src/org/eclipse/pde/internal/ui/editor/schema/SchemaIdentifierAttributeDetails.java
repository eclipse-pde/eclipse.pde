/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Vector;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.util.PDESchemaHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaIdentifierAttributeDetails extends SchemaAttributeDetails {

	private FormEntry fReferenceEntry;
	private TableViewer fRestrictionsTable;
	private Button fAddRestriction;
	private Button fRemoveRestriction;

	public SchemaIdentifierAttributeDetails(ElementSection section) {
		super(section);
	}

	// TODO we should reuse our attribute tables when possible
	@Override
	protected void createTypeDetails(Composite parent, FormToolkit toolkit) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fReferenceEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaStringAttributeDetails_reference, PDEUIMessages.SchemaAttributeDetails_browseButton, false, 11);

		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);
		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaIdentifierAttributeDetails_additionalRestrictions);
		label.setForeground(foreground);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 11;
		gd.verticalIndent = 2;
		label.setLayoutData(gd);

		// create restrictions
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

	@Override
	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		super.updateFields(object);

		String basedOn = getAttribute().getBasedOn();
		if ((basedOn != null) && (basedOn.length() > 0)) {
			fReferenceEntry.setValue(basedOn, true);
		} else {
			fReferenceEntry.setValue("", true); //$NON-NLS-1$
		}

		boolean editable = isEditableElement();
		fReferenceEntry.setEditable(editable);

		fRestrictionsTable.setInput(new Object());
		fRestrictionsTable.getControl().setEnabled(editable);
		fAddRestriction.setEnabled(editable);
		fRemoveRestriction.setEnabled(!fRestrictionsTable.getStructuredSelection().isEmpty() && editable);
	}

	@Override
	public void hookListeners() {
		super.hookListeners();
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fReferenceEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				getAttribute().setBasedOn(fReferenceEntry.getValue());
			}

			@Override
			public void browseButtonSelected(FormEntry entry) {
				if (blockListeners())
					return;
				doOpenSelectionDialog(fReferenceEntry);
			}
		});
		fAddRestriction.addSelectionListener(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			NewRestrictionDialog dialog = new NewRestrictionDialog(getPage().getSite().getShell());
			if (dialog.open() != Window.OK)
				return;
			String text = dialog.getNewRestriction();
			if (text != null && text.length() > 0) {
				ISchemaSimpleType type = getAttribute().getType();
				ChoiceRestriction res = (ChoiceRestriction) type.getRestriction();
				java.util.List<ISchemaEnumeration> vres = new Vector<>();
				if (res != null) {
					ISchemaEnumeration[] currRes = res.getChildren();
					for (ISchemaEnumeration currRe : currRes) {
						vres.add(currRe);
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
		}));
		fRemoveRestriction.addSelectionListener(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			IStructuredSelection selection = fRestrictionsTable.getStructuredSelection();
			if (selection.isEmpty())
				return;
			Object[] aselection = selection.toArray();
			ISchemaSimpleType type = getAttribute().getType();
			ChoiceRestriction res = (ChoiceRestriction) type.getRestriction();
			java.util.List<ISchemaEnumeration> vres = new Vector<>();
			if (res != null) {
				ISchemaEnumeration[] currRes = res.getChildren();
				for (ISchemaEnumeration currRe : currRes) {
					boolean stays = true;
					for (Object element : aselection) {
						if (currRe.equals(element))
							stays = false;
					}
					if (stays)
						vres.add(currRe);
				}
				res.setChildren(vres);
				if (type instanceof SchemaSimpleType) {
					if (vres.isEmpty())
						((SchemaSimpleType) type).setRestriction(null);
					else
						((SchemaSimpleType) type).setRestriction(res);
				}
				fRestrictionsTable.refresh();
			}
		}));
		fRestrictionsTable.addSelectionChangedListener(event -> {
			if (blockListeners())
				return;
			fRemoveRestriction.setEnabled(getAttribute().getSchema().isEditable() && !event.getSelection().isEmpty());
		});
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		FilteredSchemaAttributeSelectionDialog dialog = new FilteredSchemaAttributeSelectionDialog(PDEPlugin.getActiveWorkbenchShell());
		int status = dialog.open();
		if (status == Window.OK) {
                        Object[] selectedAttributes = dialog.getResult();
                        StringBuilder result = new StringBuilder();
                        for (Object object : selectedAttributes) {
                                if (object instanceof ISchemaAttribute) {
                                        ISchemaAttribute attribute = (ISchemaAttribute) object;
                                        String id = PDESchemaHelper.getReferenceIdentifier(attribute);
                                        if (result.length() > 0) {
                                                result.append(","); //$NON-NLS-1$
                                        }
                                        result.append(id);
                                }
			}
                        entry.setValue(result.toString());
                        entry.commit();
		}
	}

}
