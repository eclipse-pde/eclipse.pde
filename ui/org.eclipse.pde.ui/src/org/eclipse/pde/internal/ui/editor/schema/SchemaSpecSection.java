/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SchemaSpecSection extends PDESection {

	private FormEntry fPluginText;
	private FormEntry fPointText;
	private FormEntry fNameText;

	public SchemaSpecSection(SchemaOverviewPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.SchemaEditor_SpecSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_SpecSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fPluginText.commit();
		fPointText.commit();
		fNameText.commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fPluginText.cancelEdit();
		fPointText.cancelEdit();
		fNameText.cancelEdit();
		super.cancelEdit();
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		final Schema schema = (Schema) getPage().getModel();
		fPluginText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_plugin, null, false);
		fPluginText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPluginId(text.getValue());
			}
		});
		fPointText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_point, null, false);
		fPointText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPointId(text.getValue());
			}
		});
		fNameText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_name, null, false);
		fNameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setName(text.getValue());
				getPage().getManagedForm().getForm().setText(schema.getName());
			}
		});

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	public void dispose() {
		ISchema schema = (ISchema) getPage().getModel();
		if (schema != null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		ISchema schema = (ISchema) getPage().getModel();
		refresh();
		fPluginText.setEditable(isEditable());
		fPointText.setEditable(isEditable());
		fNameText.setEditable(isEditable());
		schema.addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.SectionPart#setFocus()
	 */
	public void setFocus() {
		if (fPluginText != null) {
			fPluginText.getText().setFocus();
		}
	}

	public void refresh() {
		ISchema schema = (ISchema) getPage().getModel();
		fPluginText.setValue(schema.getPluginId(), true);
		fPointText.setValue(schema.getPointId(), true);
		fNameText.setValue(schema.getName(), true);
		getPage().getManagedForm().getForm().setText(schema.getName());
		super.refresh();
	}

	public boolean canPaste(Clipboard clipboard) {
		return isEditable();
	}
}
