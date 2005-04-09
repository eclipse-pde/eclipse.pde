/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * 
 */
public class FeatureReferencePage extends PDEFormPage {
	public static final String PAGE_ID = "reference"; //$NON-NLS-1$

	private PluginSection fPluginSection;

	private PluginDetailsSection fPluginDetailsSection;

	private PluginPortabilitySection fPluginPortabilitySection;

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
		layout.makeColumnsEqualWidth = true;
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

		fPluginSection = new PluginSection(this, left);
		gd = new GridData(GridData.FILL_BOTH);
		fPluginSection.getSection().setLayoutData(gd);

		fPluginDetailsSection = new PluginDetailsSection(this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		fPluginDetailsSection.getSection().setLayoutData(gd);

		fPluginPortabilitySection = new PluginPortabilitySection(this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		fPluginPortabilitySection.getSection().setLayoutData(gd);

		managedForm.addPart(fPluginSection);
		managedForm.addPart(fPluginDetailsSection);
		managedForm.addPart(fPluginPortabilitySection);

		form.setText(PDEUIMessages.FeatureEditor_ReferencePage_heading);
		// WorkbenchHelp.setHelp(form.getBody(),
		// IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		fPluginSection.fireSelection();
	}

	public void setFocus() {
		fPluginSection.setFocus();
	}
}
