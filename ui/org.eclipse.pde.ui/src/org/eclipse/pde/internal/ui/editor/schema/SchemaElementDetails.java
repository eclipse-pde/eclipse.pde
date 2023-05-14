/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaElementDetails extends AbstractSchemaDetails {

	private SchemaElement fElement;
	private FormEntry fName;
	private Button fDepTrue;
	private Button fDepFalse;
	private Button fTransTrue;
	private Button fTransFalse;

	public SchemaElementDetails(ElementSection section) {
		super(section, true, true);
	}

	@Override
	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);

		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);
		// Ensures label columns on every detail page are same width
		((GridData) fName.getLabel().getLayoutData()).widthHint = minLabelWeight;

		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		Button[] buttons = createTrueFalseButtons(parent, toolkit, 2);
		fDepTrue = buttons[0];
		fDepFalse = buttons[1];

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_translatable);
		label.setForeground(foreground);
		buttons = createTrueFalseButtons(parent, toolkit, 2);
		fTransTrue = buttons[0];
		fTransFalse = buttons[1];

		setText(PDEUIMessages.SchemaElementDetails_title);
	}

	@Override
	public void updateFields(ISchemaObject object) {
		if (object instanceof SchemaElementReference)
			object = ((SchemaElementReference) object).getReferencedObject();
		fElement = (SchemaElement) object;
		if (fElement == null)
			return;
		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
		fName.setValue(fElement.getName(), true);

		fDepTrue.setSelection(fElement.isDeprecated());
		fDepFalse.setSelection(!fElement.isDeprecated());

		boolean isTranslatable = true;
		if ((fElement.getType() instanceof ISchemaComplexType && ((ISchemaComplexType) fElement.getType()).getCompositor() != null) || fElement.getAttributeCount() != 0)
			isTranslatable = false;

		fTransTrue.setSelection(fElement.hasTranslatableContent());
		fTransFalse.setSelection(!fElement.hasTranslatableContent());

		boolean editable = isEditableElement();
		fName.setEditable(editable);

		fDepTrue.setEnabled(editable);
		fDepFalse.setEnabled(editable);
		fTransTrue.setEnabled(editable && isTranslatable);
		fTransFalse.setEnabled(editable && isTranslatable);
	}

	@Override
	public void hookListeners() {
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				boolean revert = false;
				if (fName.getValue().length() == 0)
					revert = true;
				else {
					ISchemaElement[] elements = fElement.getSchema().getElements();
					for (ISchemaElement element : elements) {
						if (element != fElement && element.getName().equalsIgnoreCase(fName.getValue())) {
							revert = true;
							break;
						}
					}
				}
				if (revert)
					fName.setValue(fElement.getName(), true);
				else {
					fElement.setName(fName.getValue());
					((Schema) fElement.getSchema()).updateReferencesFor(fElement, ISchema.REFRESH_RENAME);
					setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
				}
			}
		});
		fDepTrue.addSelectionListener(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fElement.setDeprecatedProperty(fDepTrue.getSelection());
		}));
		fTransTrue.addSelectionListener(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fElement.setTranslatableProperty(fTransTrue.getSelection());
		}));
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		Object[] changedObjs = event.getChangedObjects();
		if (event.getChangeType() == IModelChangedEvent.INSERT && changedObjs.length > 0) {
			if (changedObjs[0] instanceof SchemaElement) {
				fName.getText().setFocus();
			}
		}
		super.modelChanged(event);
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fName.commit();
	}
}
