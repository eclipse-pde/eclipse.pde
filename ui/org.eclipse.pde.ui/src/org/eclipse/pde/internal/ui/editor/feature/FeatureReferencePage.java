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
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.help.WorkbenchHelp;
/**
 *  
 */
public class FeatureReferencePage extends PDEFormPage {
	public static final String PAGE_ID = "reference";
	private static final String KEY_HEADING = "FeatureEditor.ReferencePage.heading";
	private PluginSection pluginSection;
	private RequiresSection requiresSection;
	private FeatureMatchSection matchSection;
/**
 * 
 * @param editor
 * @param title
 */
	public FeatureReferencePage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		//layout.makeColumnsEqualWidth=true;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;
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
		pluginSection = new PluginSection(this, left);
		gd = new GridData(GridData.FILL_BOTH);
		pluginSection.getSection().setLayoutData(gd);
		requiresSection = new RequiresSection(this, right);
		gd = new GridData(GridData.FILL_BOTH);
		requiresSection.getSection().setLayoutData(gd);
		matchSection = new FeatureMatchSection(this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		matchSection.getSection().setLayoutData(gd);
		/*
		 * String title = PDEPlugin.getResourceString(KEY_P_TITLE); String desc =
		 * PDEPlugin.getResourceString(KEY_P_DESC);
		 * 
		 * portabilitySection = new PortabilitySection(page, title, desc, true);
		 * control = portabilitySection.createControl(right, factory); gd = new
		 * GridData(GridData.FILL_HORIZONTAL); control.setLayoutData(gd);
		 */
		managedForm.addPart(pluginSection);
		managedForm.addPart(requiresSection);
		managedForm.addPart(matchSection);
		//		registerSection(portabilitySection);
		WorkbenchHelp.setHelp(form.getBody(),
				IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		initialize();
	}
	public void initialize() {
		IFeatureModel model = (IFeatureModel) getModel();
		getManagedForm().getForm().setText(
				PDEPlugin.getResourceString(KEY_HEADING));
	}
}