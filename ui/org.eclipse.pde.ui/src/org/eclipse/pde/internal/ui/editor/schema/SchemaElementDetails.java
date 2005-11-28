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
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
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

public class SchemaElementDetails extends AbstractSchemaDetails {

	private SchemaElement fElement;
	private FormEntry fIcon;
	private FormEntry fLabelProperty;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private ComboPart fTranslatable;
	private Label fTransLabel;
	private Label fDepLabel;
	
	public SchemaElementDetails(ISchemaElement element, ElementSection section) {
		super(section, true);
		if (element instanceof SchemaElementReference)
			element = (SchemaElement)((SchemaElementReference)element).getReferencedObject();
		fElement = (SchemaElement)element;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);
		fName.setDimLabel(true);
		fLabelProperty = new FormEntry(parent, toolkit, PDEUIMessages.SchemaElementDetails_labelProperty, SWT.NONE);
		fLabelProperty.setDimLabel(true);
		fIcon = new FormEntry(parent, toolkit, PDEUIMessages.SchemaElementDetails_icon, SWT.NONE);
		fIcon.setDimLabel(true);
		
		fDepLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		fDepLabel.setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);

		fTransLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_translatable);
		fTransLabel.setForeground(foreground);
		fTranslatable = createComboPart(parent, toolkit, BOOLS, 2);
		
		setText(PDEUIMessages.SchemaElementDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
	}

	public void updateFields() {
		if (fElement == null)
			return;
		fName.setValue(fElement.getName(), true);
		fLabelProperty.setValue(fElement.getLabelProperty(), true);
		fIcon.setValue(fElement.getIconProperty(), true);
		
		fDeprecated.select(fElement.isDeprecated() ? 0 : 1);
		fTranslatable.select(fElement.hasTranslatableContent() ? 0 : 1);
		
		boolean editable = fElement.getSchema().isEditable();
		fIcon.setEditable(editable);
		fLabelProperty.setEditable(editable);
		fName.setEditable(editable);
		fDeprecated.setEnabled(editable);
		fTranslatable.setEnabled(editable);
		fDepLabel.setEnabled(editable);
		fTransLabel.setEnabled(editable);
	}

	public void hookListeners() {
		fIcon.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setIconProperty(fIcon.getValue());
			}
		});
		fLabelProperty.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setLabelProperty(fLabelProperty.getValue());
			}
		});
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setName(fName.getValue());
				setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setDeprecatedProperty(fDeprecated.getSelectionIndex() == 0);
			}
		});
		fTranslatable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setTranslatableProperty(fTranslatable.getSelectionIndex() == 0);
			}
		});
	}

	public ISchemaObject getDetailsObject() {
		return fElement;
	}
}
