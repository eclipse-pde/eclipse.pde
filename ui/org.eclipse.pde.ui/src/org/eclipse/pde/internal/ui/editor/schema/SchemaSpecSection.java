/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;
/**
 *
 */
public class SchemaSpecSection extends PDESection {
	public static final String SECTION_TITLE = "SchemaEditor.SpecSection.title";
	public static final String SECTION_DESC = "SchemaEditor.SpecSection.desc";
	public static final String SECTION_PLUGIN = "SchemaEditor.SpecSection.plugin";
	public static final String SECTION_POINT = "SchemaEditor.SpecSection.point";
	public static final String SECTION_NAME = "SchemaEditor.SpecSection.name";
	private FormEntry pluginText;
	private FormEntry pointText;
	private FormEntry nameText;
	public SchemaSpecSection(SchemaFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | Section.TWISTIE);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	public void commit(boolean onSave) {
		pluginText.commit();
		pointText.commit();
		nameText.commit();
		super.commit(onSave);
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);
		final Schema schema = (Schema) getPage().getModel();
		pluginText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_PLUGIN), null, false);
		pluginText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPluginId(text.getValue());
			}
		});
		pointText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_POINT), null, false);
		pointText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPointId(text.getValue());
			}
		});
		nameText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_NAME), null, false);
		nameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setName(text.getValue());
				getPage().getManagedForm().getForm().setText(schema.getName());
			}
		});
		GridData gd = (GridData) pointText.getText().getLayoutData();
		gd.widthHint = 150;
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}
	public void dispose() {
		ISchema schema = (ISchema) getPage().getModel();
		if (schema!=null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}
	public void initialize() {
		ISchema schema = (ISchema) getPage().getModel();
		refresh();
		if (!(schema instanceof IEditable)) {
			pluginText.getText().setEnabled(false);
			pointText.getText().setEnabled(false);
			nameText.getText().setEnabled(false);
		}
		schema.addModelChangedListener(this);
	}

	public void setFocus() {
		if (pointText != null)
			pointText.getText().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		ISchema schema = (ISchema)getPage().getModel();
		setIfDefined(pluginText, schema.getPluginId());
		setIfDefined(pointText, schema.getPointId());
		setIfDefined(nameText, schema.getName());
		getPage().getManagedForm().getForm().setText(schema.getName());
		super.refresh();
	}
}