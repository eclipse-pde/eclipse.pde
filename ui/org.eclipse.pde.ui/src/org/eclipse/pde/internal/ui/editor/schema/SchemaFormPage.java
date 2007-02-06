/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class SchemaFormPage extends PDEFormPage implements IModelChangedListener {
	public static final String PAGE_ID = "form"; //$NON-NLS-1$
	private ElementSection fSection;
	private SchemaBlock fBlock;
	private IColorManager fColorManager;
	private DetailsPart fDetailsPart;
	
	public class SchemaBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider {
		
		public SchemaBlock() {
			super(SchemaFormPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fSection = new ElementSection(getPage(), parent);
			return fSection;
		}
		protected void registerPages(DetailsPart detailsPart) {
			fDetailsPart = detailsPart;
			detailsPart.setPageLimit(5);
			detailsPart.registerPage(ISchemaObjectReference.class, new SchemaElementReferenceDetails(fSection));
			detailsPart.registerPage(ISchemaRootElement.class, new SchemaRootElementDetails(fSection));
			detailsPart.registerPage(ISchemaElement.class, new SchemaElementDetails(fSection));
			detailsPart.registerPage(ISchemaCompositor.class, new SchemaCompositorDetails(fSection));
			detailsPart.registerPage(ISchemaAttribute.class, new SchemaAttributeDetails(fSection));
			detailsPart.setPageProvider(this);
		}
		public Object getPageKey(Object object) {
			if (object instanceof ISchemaObjectReference)
				return ISchemaObjectReference.class;
			else if (object instanceof ISchemaRootElement)
				return ISchemaRootElement.class;
			else if (object instanceof ISchemaElement)
				return ISchemaElement.class;
			else if (object instanceof ISchemaCompositor)
				return ISchemaCompositor.class;
			else if (object instanceof ISchemaAttribute)
				return ISchemaAttribute.class;
			else
				return null;
		}
		
		public IDetailsPage getPage(Object object) {
			return null;
		}
	}
	
	public SchemaFormPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SchemaEditor_FormPage_title);
		fBlock = new SchemaBlock();
		fColorManager = ColorManager.getDefault();
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		fBlock.createContent(managedForm);
		DescriptionSection descSection = new DescriptionSection(this, form.getBody(), fColorManager);
		managedForm.addPart(descSection);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_MAIN);
		initialize();
	}
	
	public void initialize() {
		ISchema schema = (ISchema)getModel();
		getManagedForm().getForm().setText(schema.getName());
		schema.addModelChangedListener(this);
	}
	public void dispose() {
		ISchema schema = (ISchema) getModel();
		if (schema != null) schema.removeModelChangedListener(this);
		fColorManager.dispose();
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			String changeProperty = event.getChangedProperty();
			if (changeProperty != null && changeProperty.equals(ISchemaObject.P_NAME)) {
				Object[] change = event.getChangedObjects();
				if (change.length > 0 && change[0] instanceof ISchema)
					getManagedForm().getForm().setText(((ISchema)change[0]).getName());
			}
		}
		if (fSection != null)
			fSection.handleModelChanged(event);
		IDetailsPage page = fDetailsPart.getCurrentPage();
		if (page instanceof IModelChangedListener)
			((IModelChangedListener)page).modelChanged(event);
	}
}
