/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.views.properties.IPropertySheetPage;
/**
 * 
 */
public class SchemaFormPage extends PDEFormPage implements IModelChangedListener {
	public static final String PAGE_ID = "form"; //$NON-NLS-1$
	public static final String PAGE_TITLE = "SchemaEditor.FormPage.title"; //$NON-NLS-1$
	private SchemaSpecSection schemaSpecSection;
	private ElementSection elementSection;
	private DescriptionSection descriptionSection;
	private GrammarSection grammarSection;
	private SchemaPropertySheet propertySheetPage;
	private IColorManager colorManager=new ColorManager();	

	public SchemaFormPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString(PAGE_TITLE));
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		//layout.marginWidth = 10;
		layout.horizontalSpacing=15;
		form.getBody().setLayout(layout);

		GridData gd;
		
		schemaSpecSection = new SchemaSpecSection(this, form.getBody());
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		schemaSpecSection.getSection().setLayoutData(gd);

		grammarSection = new GrammarSection(this, form.getBody());
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		gd.verticalSpan = 2;
		grammarSection.getSection().setLayoutData(gd);
		
		elementSection = new ElementSection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		elementSection.getSection().setLayoutData(gd);

		descriptionSection = new DescriptionSection(this, form.getBody(), colorManager);
		gd = new GridData(GridData.FILL_HORIZONTAL
		                            | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		gd.heightHint = 150;
		descriptionSection.getSection().setLayoutData(gd);

		managedForm.addPart(schemaSpecSection);
		managedForm.addPart(elementSection);
		managedForm.addPart(grammarSection);
		managedForm.addPart(descriptionSection);
		
		WorkbenchHelp.setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_MAIN);
		initialize();
	}
	public void initialize() {
		ISchema schema = (ISchema)getModel();
		getManagedForm().getForm().setText(schema.getName());
		schema.addModelChangedListener(this);
	}
	public void dispose() {
		ISchema schema = (ISchema) getModel();
		colorManager.dispose();
		if (schema!=null) schema.removeModelChangedListener(this);
		super.dispose();
	}

	public IPropertySheetPage getPropertySheetPage() {
		if (propertySheetPage==null)
			propertySheetPage = new SchemaPropertySheet();
		return propertySheetPage;
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			ISchema schema = (ISchema) getModel();
			getManagedForm().getForm().setText(schema.getName());
		}
	}
}
