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

import java.util.ArrayList;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
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
	private FormEntry fName;
	private ComboPart fLabelProperty;
	private ComboPart fIcon;
	private ComboPart fDeprecated;
	private ComboPart fTranslatable;
	
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
		
		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaElementDetails_labelProperty);
		label.setForeground(foreground);
		fLabelProperty = createComboPart(parent, toolkit, getLabelItems(), 2);
		
		label = toolkit.createLabel(parent, PDEUIMessages.SchemaElementDetails_icon);
		label.setForeground(foreground);
		fIcon = createComboPart(parent, toolkit, getIconItems(), 2);
		
		label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_translatable);
		label.setForeground(foreground);
		fTranslatable = createComboPart(parent, toolkit, BOOLS, 2);
		
		setText(PDEUIMessages.SchemaElementDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
	}

	public void updateFields() {
		if (fElement == null)
			return;
		fName.setValue(fElement.getName(), true);
		String labProp = fElement.getLabelProperty();
		fLabelProperty.setText(labProp != null ? labProp : "");
		String icProp = fElement.getIconProperty();
		fIcon.setText(icProp != null ? icProp : "");
		
		fDeprecated.select(fElement.isDeprecated() ? 0 : 1);
		fTranslatable.select(fElement.hasTranslatableContent() ? 0 : 1);
		
		boolean editable = isEditableElement();
		fIcon.setEnabled(editable);
		fLabelProperty.setEnabled(editable);
		fName.setEditable(editable);
	}

	public void hookListeners() {
		fIcon.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String icon = fIcon.getSelection();
				if (icon == null || icon.equals(""))
					fElement.setIconProperty(null);
				else
					fElement.setIconProperty(icon);
			}
		});
		fLabelProperty.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String label = fLabelProperty.getSelection();
				if (label == null || label.equals(""))
					fElement.setLabelProperty(null);
				else
					fElement.setLabelProperty(label);
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
	
	private String[] getIconItems() {
		ISchemaAttribute[] attribs = fElement.getAttributes();
		ArrayList list = new ArrayList();
		list.add("");
		for (int i = 0; i < attribs.length; i++) {
			if (attribs[i].getKind() == IMetaAttribute.RESOURCE) {
				list.add(attribs[i].getName());
			}
		}
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	private String[] getLabelItems() {
		ISchemaAttribute[] attribs = fElement.getAttributes();
		String[] labels = new String[attribs.length + 1];
		labels[0] = "";
		for (int i = 0; i < attribs.length; i++) {
			labels[i + 1] = attribs[i].getName();
		}
		return labels;
	}
}
