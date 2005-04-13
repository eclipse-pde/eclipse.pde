/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * 
 * 
 */
public class InfoFormPage extends PDEFormPage {
	public static final String PAGE_ID = "info";	 //$NON-NLS-1$
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
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.marginWidth = 10;
		GridData gd;
		
		infoSection = new InfoSection(this, form.getBody(), colorManager);
		gd = new GridData(GridData.FILL_BOTH);
		infoSection.getSection().setLayoutData(gd);
		managedForm.addPart(infoSection);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_INFO);
		initialize();
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	public void initialize() {
		getManagedForm().getForm().setText(PDEUIMessages.FeatureEditor_InfoPage_heading);
	}
}
