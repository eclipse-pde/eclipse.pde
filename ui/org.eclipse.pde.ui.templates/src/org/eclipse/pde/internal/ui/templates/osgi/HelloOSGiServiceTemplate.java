/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.*;

public class HelloOSGiServiceTemplate extends PDETemplateSection {

	public static final String LANGUAGE = "language"; //$NON-NLS-1$
	public static final String WORD1 = "word1"; //$NON-NLS-1$
	public static final String WORD2 = "word2"; //$NON-NLS-1$
	public static final String WORD3 = "word3"; //$NON-NLS-1$
	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$

	public HelloOSGiServiceTemplate() {
		setPageCount(1);
		addOption(LANGUAGE, PDETemplateMessages.HelloOSGiServiceTemplate_greeting, PDETemplateMessages.HelloOSGiServiceTemplate_howdy, 0);
		addOption(WORD1, PDETemplateMessages.HelloOSGiServiceTemplate_word1, "osgi", 0); //$NON-NLS-1$
		addOption(WORD2, PDETemplateMessages.HelloOSGiServiceTemplate_word2, "eclipse", 0); //$NON-NLS-1$
		addOption(WORD3, PDETemplateMessages.HelloOSGiServiceTemplate_word3, "equinox", 0); //$NON-NLS-1$

	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.HelloOSGiServiceTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.HelloOSGiServiceTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "helloOSGiService"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) { // do nothing

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#isDependentOnParentWizard()
	 */
	public boolean isDependentOnParentWizard() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}
}
