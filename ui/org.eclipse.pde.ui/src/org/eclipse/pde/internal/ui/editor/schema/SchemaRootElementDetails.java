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

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaRootElementDetails extends AbstractSchemaDetails {

	private SchemaRootElement fElement;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private FormEntry fSuggestion;
	private Label fDepLabel;
	
	public SchemaRootElementDetails(ISchemaElement element, ElementSection section) {
		super(section, true);
		if (element instanceof SchemaElementReference)
			element = (SchemaRootElement)((SchemaElementReference)element).getReferencedObject();
		fElement = (SchemaRootElement)element;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);
		fName.setDimLabel(true);
		
		fDepLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		fDepLabel.setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);

		fSuggestion = new FormEntry(parent, toolkit, PDEUIMessages.SchemaRootElementDetails_replacement, null, false, 6);
		fSuggestion.setDimLabel(true);
		
		setText(PDEUIMessages.SchemaElementDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
	}

	public void updateFields() {
		if (fElement == null)
			return;
		fName.setValue(fElement.getName(), true);		
		fDeprecated.select(fElement.isDeprecated() ? 0 : 1);
		fSuggestion.setValue(fElement.getDeprecatedSuggestion(), true);
		
		boolean editable = fElement.getSchema().isEditable();
		fSuggestion.setEditable(fElement.isDeprecated() && editable);
		fName.setEditable(editable);
		fDeprecated.setEnabled(editable);
		fDepLabel.setEnabled(editable);
	}

	public void hookListeners() {
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setName(fName.getValue());
				setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setDeprecatedProperty(fDeprecated.getSelectionIndex() == 0);
				fSuggestion.setEditable(fDeprecated.getSelectionIndex() == 0
						&& fElement.getSchema().isEditable());
			}
		});
		fSuggestion.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setDeprecatedSuggestion(fSuggestion.getValue());
			}
		});
	}

	public ISchemaObject getDetailsObject() {
		return fElement;
	}
}
