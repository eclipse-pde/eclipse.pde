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

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;

/**
 * TODO add class javadoc
 */

public abstract class AbstractNewPluginTemplateWizard
	extends Wizard implements IPluginContentWizard {
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title";
	boolean fragment;
	private String id;

	/**
	 * Creates a new template wizard.
	 */

	public AbstractNewPluginTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setNeedsProgressMonitor(true);
	}
/**
 * TODO add javadoc.
 */
	public void init(String id) {
		this.id = id;
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	/**
	 * This wizard adds a mandatory first page. Subclasses implement
	 * this method to add additional pages to the wizard.
	 */
	protected abstract void addAdditionalPages();

	/**
	 * Implements wizard method. Subclasses cannot override it.
	 */
	public final void addPages() {
		addAdditionalPages();
	}

	/**
	 * @see org.eclipse.pde.ui.IPluginContentWizard#getPluginData()
	 */
	public String getPluginId() {
		return id;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		return true;
	}
}