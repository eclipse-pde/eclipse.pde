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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.plugin.MatchSection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * 
 */
public class FeatureDependenciesPage extends PDEFormPage {
	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$

	private static final String KEY_HEADING = "FeatureEditor.DependenciesPage.heading"; //$NON-NLS-1$

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

		fRequiresSection = new RequiresSection(this, left);
		gd = new GridData(GridData.FILL_BOTH);
		fRequiresSection.getSection().setLayoutData(gd);

		fMatchSection = new MatchSection(this, right, false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fMatchSection.getSection().setLayoutData(gd);
		managedForm.addPart(fRequiresSection);
		managedForm.addPart(fMatchSection);
		WorkbenchHelp.setHelp(form.getBody(),
				IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		initialize();
		fRequiresSection.fireSelection();
	}

	public void initialize() {
		getManagedForm().getForm().setText(
				PDEPlugin.getResourceString(KEY_HEADING));
	}
}
