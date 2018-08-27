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

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 *
 *
 */
public class InfoFormPage extends PDEFormPage {
	public static final String PAGE_ID = "info"; //$NON-NLS-1$
	private IColorManager colorManager = ColorManager.getDefault();
	private InfoSection infoSection;

	/**
	 *
	 * @param editor
	 * @param title
	 */
	public InfoFormPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_INFO;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ));

		infoSection = new InfoSection(this, form.getBody(), colorManager);
		managedForm.addPart(infoSection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_INFO);
		initialize();
	}

	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public void initialize() {
		getManagedForm().getForm().setText(PDEUIMessages.FeatureEditor_InfoPage_heading);
	}
}
