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
public class FeatureIncludesPage extends PDEFormPage {
	public static final String PAGE_ID = "includes"; //$NON-NLS-1$

	private IncludedFeaturesSection fIncludedSection;

	private IncludedFeaturesDetailsSection fIncludedDetailsSection;

	private IncludedFeaturesPortabilitySection fIncludedPortabilitySection;

	/**
	 * 
	 * @param editor
	 * @param title
	 */
	public FeatureIncludesPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_ADVANCED;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ));

		GridData gd;

		Composite left = toolkit.createComposite(form.getBody());
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		left.setLayoutData(gd);

		Composite right = toolkit.createComposite(form.getBody());
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		right.setLayoutData(gd);

		fIncludedSection = new IncludedFeaturesSection(this, left);
		fIncludedDetailsSection = new IncludedFeaturesDetailsSection(this, right);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fIncludedSection.getSection(), fIncludedDetailsSection.getSection());

		fIncludedPortabilitySection = new IncludedFeaturesPortabilitySection(this, right);

		managedForm.addPart(fIncludedSection);
		managedForm.addPart(fIncludedDetailsSection);
		managedForm.addPart(fIncludedPortabilitySection);
		form.setText(PDEUIMessages.FeatureEditor_IncludesPage_heading);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_ADVANCED);
		// WorkbenchHelp.setHelp(form.getBody(),
		// IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		fIncludedSection.fireSelection();
		super.createFormContent(managedForm);
	}
}
