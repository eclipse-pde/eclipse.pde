/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * 
 * 
 */
public class InfoFormPage extends PDEFormPage {
	public static final String PAGE_ID = "info";	
	private static final String KEY_TITLE = "FeatureEditor.InfoPage.heading";
	private IColorManager colorManager = new ColorManager();
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
		
		WorkbenchHelp.setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_INFO);
		initialize();
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	public void initialize() {
		getManagedForm().getForm().setText(PDEPlugin.getResourceString(KEY_TITLE));
	}
}