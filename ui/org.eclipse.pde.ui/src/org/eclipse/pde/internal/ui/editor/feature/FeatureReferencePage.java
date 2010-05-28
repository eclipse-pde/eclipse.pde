/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_CONTENT;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGINS_FRAGMENTS));

		GridData gd;

		Composite left = toolkit.createComposite(form.getBody());
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		left.setLayoutData(gd);

		Composite right = toolkit.createComposite(form.getBody());
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		right.setLayoutData(gd);

		fPluginSection = new PluginSection(this, left);

		fPluginDetailsSection = new PluginDetailsSection(this, right);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fPluginSection.getSection(), fPluginDetailsSection.getSection());

		fPluginPortabilitySection = new PluginPortabilitySection(this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fPluginPortabilitySection.getSection().setLayoutData(gd);

		managedForm.addPart(fPluginSection);
		managedForm.addPart(fPluginDetailsSection);
		managedForm.addPart(fPluginPortabilitySection);

		form.setText(PDEUIMessages.FeatureEditor_ReferencePage_heading);
		// WorkbenchHelp.setHelp(form.getBody(),
		// IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		fPluginSection.fireSelection();
		super.createFormContent(managedForm);
	}

	public void setFocus() {
		fPluginSection.setFocus();
	}
}
