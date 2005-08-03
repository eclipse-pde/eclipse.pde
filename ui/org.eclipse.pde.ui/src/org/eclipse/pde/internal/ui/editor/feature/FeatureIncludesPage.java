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

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

		fIncludedSection = new IncludedFeaturesSection(this, left);
		gd = new GridData(GridData.FILL_BOTH);
		fIncludedSection.getSection().setLayoutData(gd);

		fIncludedDetailsSection = new IncludedFeaturesDetailsSection(this,
				right);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		fIncludedDetailsSection.getSection().setLayoutData(gd);

		fIncludedPortabilitySection = new IncludedFeaturesPortabilitySection(
				this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		fIncludedPortabilitySection.getSection().setLayoutData(gd);

		managedForm.addPart(fIncludedSection);
		managedForm.addPart(fIncludedDetailsSection);
		managedForm.addPart(fIncludedPortabilitySection);
		form.setText(PDEUIMessages.FeatureEditor_IncludesPage_heading); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_ADVANCED);
		// WorkbenchHelp.setHelp(form.getBody(),
		// IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		fIncludedSection.fireSelection();
	}
}
