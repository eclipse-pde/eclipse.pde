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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class FeatureAdvancedPage extends PDEFormPage {
	public static final String PAGE_ID = "advanced"; //$NON-NLS-1$
	private static final String KEY_HEADING = "FeatureEditor.AdvancedPage.heading"; //$NON-NLS-1$
	private IncludedFeaturesSection includedSection;
	private DataSection dataSection;
	private HandlerSection handlerSection;

	public FeatureAdvancedPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();

		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth=true;
		layout.marginWidth = 10;
		layout.horizontalSpacing=15;
		layout.verticalSpacing=15;
		GridData gd;
		
		Composite left = toolkit.createComposite(form.getBody());
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		left.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		left.setLayoutData(gd);
		
		Composite right = toolkit.createComposite(form.getBody());
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		right.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		right.setLayoutData(gd);

		includedSection = new IncludedFeaturesSection(this, left);
		gd = new GridData(GridData.FILL_BOTH);
		includedSection.getSection().setLayoutData(gd);
		
		dataSection = new DataSection(this, right);
		gd = new GridData(GridData.FILL_BOTH);
		dataSection.getSection().setLayoutData(gd);
		
		handlerSection = new HandlerSection(this, right);
		gd = new GridData(GridData.FILL_BOTH);
		handlerSection.getSection().setLayoutData(gd);
		
		managedForm.addPart(includedSection);
		managedForm.addPart(dataSection);
		managedForm.addPart(handlerSection);
		
		WorkbenchHelp.setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_ADVANCED);
		initialize();
	}
	
	public void initialize() {
		getManagedForm().getForm().setText(PDEPlugin.getResourceString(KEY_HEADING));
	}
}
