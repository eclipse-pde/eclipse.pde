/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.plugin.MatchSection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 *
 */
public class FeatureDependenciesPage extends PDEFormPage {
	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$

	private RequiresSection fRequiresSection;

	private MatchSection fMatchSection;

	/**
	 *
	 * @param editor
	 * @param title
	 */
	public FeatureDependenciesPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_DEPENDENCIES;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ));

		GridData gd;

		Composite left = toolkit.createComposite(form.getBody());
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		left.setLayoutData(gd);

		Composite right = toolkit.createComposite(form.getBody());
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		right.setLayoutData(gd);

		fRequiresSection = new RequiresSection(this, left);
		fMatchSection = new MatchSection(this, right, false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fMatchSection.getSection().setLayoutData(gd);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fRequiresSection.getSection(), fMatchSection.getSection());

		managedForm.addPart(fRequiresSection);
		managedForm.addPart(fMatchSection);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_DEPENDENCIES);
		initialize();
		fRequiresSection.fireSelection();
		super.createFormContent(managedForm);
	}

	public void initialize() {
		getManagedForm().getForm().setText(PDEUIMessages.FeatureEditor_DependenciesPage_heading);
	}
}
