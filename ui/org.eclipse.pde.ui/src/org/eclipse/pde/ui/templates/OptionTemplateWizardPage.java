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
package org.eclipse.pde.ui.templates;

import java.util.ArrayList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * An implementation of the standard wizard page that creates its contents from
 * the list of template options. The options will be created in the order they
 * are added to the list. When the page is made visible, options that require
 * late initialization will be given a chance to initialize.
 * 
 * @since 2.0
 */

public class OptionTemplateWizardPage extends WizardPage {
	private BaseOptionTemplateSection section;
	private ArrayList options;
	private String helpContextId;

	/**
	 * The constructor.
	 * 
	 * @param section
	 *            the section that is contributing this page
	 * @param option
	 *            a list of options that should be shown in this page.
	 */
	public OptionTemplateWizardPage(BaseOptionTemplateSection section,
			ArrayList options, String helpContextId) {
		super("");
		this.section = section;
		this.options = options;
		this.helpContextId = helpContextId;
	}
	/**
	 * Creates the page control by creating individual options in the order
	 * subject to their position in the list.'
	 * 
	 * @param composite
	 */
	public void createControl(Composite composite) {
		Composite container = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		for (int i = 0; i < options.size(); i++) {
			TemplateOption option = (TemplateOption) options.get(i);
			option.createControl(container, 2);
		}
		if (helpContextId != null)
			WorkbenchHelp.setHelp(container, helpContextId);
		setControl(container);
		Dialog.applyDialogFont(container);
	}
	/**
	 * Initializes the options that require late initialization when the page is
	 * made visible.
	 * 
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		if (visible && section.isDependentOnParentWizard()) {
			IWizard wizard = getWizard();
			if (wizard instanceof AbstractNewPluginTemplateWizard) {
				AbstractNewPluginTemplateWizard templateWizard = (AbstractNewPluginTemplateWizard) wizard;
				section.initializeFields((IFieldData) templateWizard.getData());
			}
		}
		super.setVisible(visible);
	}
}