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
package org.eclipse.pde.internal.ui.editor.feature;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
/**
 * Feature page.
 */
public class FeatureFormPage extends PDEFormPage {
	public static final String PAGE_ID = "feature";
	private URLSection urlSection;
	private FeatureSpecSection specSection;
	private PortabilitySection portabilitySection;
	
	public FeatureFormPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;
		
		GridData gd;

		specSection = new FeatureSpecSection(this, form.getBody());
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		specSection.getSection().setLayoutData(gd);

		urlSection = new URLSection(this, form.getBody());
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		urlSection.getSection().setLayoutData(gd);

		portabilitySection = new PortabilitySection(this, form.getBody());
		gd = new GridData(GridData.FILL_BOTH);
		portabilitySection.getSection().setLayoutData(gd);

		managedForm.addPart(specSection);
		managedForm.addPart(urlSection);
		managedForm.addPart(portabilitySection);
		
		WorkbenchHelp.setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_OVERVIEW);
		initialize();
	}
	public void initialize() {
		IFeatureModel model = (IFeatureModel)getModel();
		IFeature feature = model.getFeature();
		getManagedForm().getForm().setText(model.getResourceString(feature.getLabel()));
	}
}