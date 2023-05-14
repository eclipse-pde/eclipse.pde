/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DependenciesPage extends PDEFormPage {

	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$

	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.DependenciesPage_tabName);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		boolean isBundle = isBundle();
		ScrolledForm form = managedForm.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ));
		form.setText(PDEUIMessages.DependenciesPage_title);
		Composite body = form.getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(isBundle, 2));
		Composite left, right;
		FormToolkit toolkit = managedForm.getToolkit();
		left = toolkit.createComposite(body, SWT.NONE);
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		left.setLayoutData(new GridData(GridData.FILL_BOTH));
		right = toolkit.createComposite(body, SWT.NONE);
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		right.setLayoutData(new GridData(GridData.FILL_BOTH));

		RequiresSection requiresSection = new RequiresSection(this, left, getRequiredSectionLabels());
		managedForm.addPart(requiresSection);

		DependencyAnalysisSection section;
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 150;
		if (isBundle) {
			ImportPackageSection importPackageSection = new ImportPackageSection(this, right);
			importPackageSection.getSection().descriptionVerticalSpacing = requiresSection.getSection().getTextClientHeightDifference();
			managedForm.addPart(importPackageSection);
			IBaseModel model = getModel();
			if (model != null && model.isEditable())
				managedForm.addPart(new DependencyManagementSection(this, left));
			else
				gd.horizontalSpan = 2;
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.COMPACT);
		} else {
			// No MANIFEST.MF (not a Bundle), 3.0 timeframe
			MatchSection matchSection = new MatchSection(this, right, true);
			matchSection.getSection().descriptionVerticalSpacing = requiresSection.getSection().getTextClientHeightDifference();
			managedForm.addPart(matchSection);
			section = new DependencyAnalysisSection(this, right, ExpandableComposite.EXPANDED);
		}
		section.getSection().setLayoutData(gd);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES);
	}

	private boolean isBundle() {
		return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	private String[] getRequiredSectionLabels() {
		ArrayList<String> labels = new ArrayList<>();
		labels.add(PDEUIMessages.RequiresSection_add);
		labels.add(PDEUIMessages.RequiresSection_delete);
		labels.add(PDEUIMessages.RequiresSection_up);
		labels.add(PDEUIMessages.RequiresSection_down);
		if (isBundle())
			labels.add(PDEUIMessages.DependenciesPage_properties);
		return labels.toArray(new String[labels.size()]);
	}

}
