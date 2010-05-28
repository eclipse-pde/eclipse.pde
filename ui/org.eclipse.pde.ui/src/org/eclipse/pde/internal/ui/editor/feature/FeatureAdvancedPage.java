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

public class FeatureAdvancedPage extends PDEFormPage {
	public static final String PAGE_ID = "advanced"; //$NON-NLS-1$

	private InstallSection fInstallSection;

	private HandlerSection fHandlerSection;

	private DataSection fDataSection;

	private DataDetailsSection fDataDetailsSection;

	private DataPortabilitySection fDataPortabilitySection;

	public FeatureAdvancedPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_INSTALLATION;
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));

		Composite body = form.getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		Composite left = toolkit.createComposite(body);
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		left.setLayoutData(new GridData(GridData.FILL_BOTH));

		fInstallSection = new InstallSection(this, left);
		fDataSection = new DataSection(this, left);
		fDataPortabilitySection = new DataPortabilitySection(this, left);

		Composite right = toolkit.createComposite(body);
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		right.setLayoutData(new GridData(GridData.FILL_BOTH));

		fHandlerSection = new HandlerSection(this, right);
		fDataDetailsSection = new DataDetailsSection(this, right);

		managedForm.addPart(fInstallSection);
		managedForm.addPart(fHandlerSection);
		managedForm.addPart(fDataSection);
		managedForm.addPart(fDataDetailsSection);
		managedForm.addPart(fDataPortabilitySection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_INSTALLATION);

		form.setText(PDEUIMessages.FeatureEditor_AdvancedPage_heading);
		fDataSection.fireSelection();
	}
}
