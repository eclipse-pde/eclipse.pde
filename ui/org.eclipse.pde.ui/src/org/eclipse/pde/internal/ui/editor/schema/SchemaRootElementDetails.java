/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaRootElementDetails extends AbstractSchemaDetails {

	private SchemaRootElement fElement;
	private FormEntry fName;
	private Button fDepTrue;
	private Button fDepFalse;

	private Button fInternalTrue;
	private Button fInternalFalse;

	private FormEntry fSuggestion;

	public SchemaRootElementDetails(ElementSection section) {
		super(section, true, true);
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);

		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.READ_ONLY);
		// Ensures label columns on every detail page are same width
		((GridData) fName.getLabel().getLayoutData()).widthHint = minLabelWeight;

		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_internal);
		label.setForeground(foreground);
		Button[] internalButtons = createTrueFalseButtons(parent, toolkit, 2);
		fInternalTrue = internalButtons[0];
		fInternalFalse = internalButtons[1];

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		Button[] deprecatedButtons = createTrueFalseButtons(parent, toolkit, 2);
		fDepTrue = deprecatedButtons[0];
		fDepFalse = deprecatedButtons[1];

		fSuggestion = new FormEntry(parent, toolkit, PDEUIMessages.SchemaRootElementDetails_replacement, null, false, 6);

		setText(PDEUIMessages.SchemaElementDetails_rootTitle);
	}

	public void updateFields(ISchemaObject element) {
		if (!(element instanceof ISchemaElement))
			return;
		if (element instanceof SchemaElementReference)
			element = ((SchemaElementReference) element).getReferencedObject();
		fElement = (SchemaRootElement) element;
		if (fElement == null)
			return;

		setDecription(NLS.bind(PDEUIMessages.SchemaElementDetails_description, fElement.getName()));
		fName.setValue(fElement.getName(), true);
		fDepTrue.setSelection(fElement.isDeprecated());
		fDepFalse.setSelection(!fElement.isDeprecated());
		fSuggestion.setValue(fElement.getDeprecatedSuggestion(), true);

		fInternalTrue.setSelection(fElement.isInternal());
		fInternalFalse.setSelection(!fElement.isInternal());

		fDepTrue.setEnabled(isEditable());
		fDepFalse.setEnabled(isEditable());

		fInternalTrue.setEnabled(isEditable());
		fInternalFalse.setEnabled(isEditable());

		if (!fElement.isDeprecated()) {
			fSuggestion.getLabel().setEnabled(false);
			fSuggestion.getText().setEditable(false);
		} else {
			fSuggestion.setEditable(isEditable());
		}
	}

	public void hookListeners() {
		fDepTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				boolean deprecated = fDepTrue.getSelection();
				fElement.setDeprecatedProperty(deprecated);
				fSuggestion.getLabel().setEnabled(deprecated);
				fSuggestion.getText().setEditable(deprecated);
			}
		});

		fInternalTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				boolean internal = fInternalTrue.getSelection();
				fElement.setInternal(internal);
			}
		});

		fSuggestion.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				fElement.setDeprecatedSuggestion(fSuggestion.getValue());
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fSuggestion.commit();
	}

}
