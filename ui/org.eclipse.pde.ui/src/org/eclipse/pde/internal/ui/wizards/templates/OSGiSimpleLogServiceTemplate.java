/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OSGiSimpleLogServiceTemplate extends PDETemplateSection {
	
	public static final String START_LOG_MESSAGE = "startLogMessage"; //$NON-NLS-1$
	public static final String STOP_LOG_MESSAGE = "stopLogMessage"; //$NON-NLS-1$
	public static final String KEY_APPLICATION_CLASS = "applicationClass"; //$NON-NLS-1$
	
	private OSGiSimpleLogServiceNewWizard wizard;
	
	public OSGiSimpleLogServiceTemplate(OSGiSimpleLogServiceNewWizard wizard) {
		setPageCount(1);
		this.wizard = wizard;
		addOption(START_LOG_MESSAGE, 
				PDEUIMessages.OSGiSimpleLogServiceTemplate_startLogMessage, 
				PDEUIMessages.OSGiSimpleLogServiceTemplate_logMessage, 
				0);
		addOption(STOP_LOG_MESSAGE, 
				PDEUIMessages.OSGiSimpleLogServiceTemplate_stopLogMessage, 
				PDEUIMessages.OSGiSimpleLogServiceTemplate_logMessage, 
				0);
	}
	
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDEUIMessages.OSGiSimpleLogServiceTemplate_pageTitle); 
		page.setDescription(PDEUIMessages.OSGiSimpleLogServiceTemplate_pageDescription);  
		wizard.addPage(page);
		markPagesAdded();
	}
	
	public String getReplacementString(String fileName, String key) {
		if (KEY_APPLICATION_CLASS.equals(key)) {
			String className = wizard.fData.getClassname();
			return className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
		} else if (KEY_PACKAGE_NAME.equals(key))
			return getFormattedPackageName(wizard.fData.getId());
		return super.getReplacementString(fileName, key);
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
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		
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
