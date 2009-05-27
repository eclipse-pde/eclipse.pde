/*******************************************************************************
 *  Copyright (c) 2006, 2007 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.*;

public class OSGiSimpleLogServiceTemplate extends PDETemplateSection {

	public static final String START_LOG_MESSAGE = "startLogMessage"; //$NON-NLS-1$
	public static final String STOP_LOG_MESSAGE = "stopLogMessage"; //$NON-NLS-1$

	public OSGiSimpleLogServiceTemplate() {
		setPageCount(1);
		addOption(START_LOG_MESSAGE, PDETemplateMessages.OSGiSimpleLogServiceTemplate_startLogMessage, PDETemplateMessages.OSGiSimpleLogServiceTemplate_logMessage, 0);
		addOption(STOP_LOG_MESSAGE, PDETemplateMessages.OSGiSimpleLogServiceTemplate_stopLogMessage, PDETemplateMessages.OSGiSimpleLogServiceTemplate_logMessage, 0);
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.OSGiSimpleLogServiceTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.OSGiSimpleLogServiceTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "OSGiSimpleLogService"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) {
		// do nothing
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
