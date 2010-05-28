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
package org.eclipse.pde.ui.templates;

import java.util.ArrayList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

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
	 * @param options
	 *            a list of options that should be shown in this page.
	 * @param helpContextId
	 * 			  the help context id
	 */
	public OptionTemplateWizardPage(BaseOptionTemplateSection section, ArrayList options, String helpContextId) {
		super(""); //$NON-NLS-1$
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
			PlatformUI.getWorkbench().getHelpSystem().setHelp(container, helpContextId);
		setControl(container);
		Dialog.applyDialogFont(container);
		container.forceFocus();
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
				section.initializeFields(templateWizard.getData());
			}
		}
		super.setVisible(visible);
	}
}
