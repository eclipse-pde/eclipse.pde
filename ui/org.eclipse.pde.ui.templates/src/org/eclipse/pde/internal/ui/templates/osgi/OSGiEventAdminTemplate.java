/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;

public class OSGiEventAdminTemplate extends PDETemplateSection {

	public static final String EVENT_TOPIC = "eventTopic"; //$NON-NLS-1$
	private String packageName = null;

	public OSGiEventAdminTemplate() {
		setPageCount(1);
		addOption(EVENT_TOPIC, PDETemplateMessages.OSGiEventAdminTemplate_eventTopicTitle, "org/osgi/framework/BundleEvent/STARTED", 0); //$NON-NLS-1$
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_RCP_MAIL);
		page.setTitle(PDETemplateMessages.OSGiEventAdminTemplate_pageTitle);
		page.setDescription(PDETemplateMessages.OSGiEventAdminTemplate_pageDescription);
		wizard.addPage(page);
		markPagesAdded();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "OSGiEventAdmin"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) { // do nothing
		setManifestHeader("Service-Component", "OSGI-INF/*.xml"); //$NON-NLS-1$ //$NON-NLS-2$]
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

	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String packageName = getFormattedPackageName(data.getId());
		initializeOption(KEY_PACKAGE_NAME, packageName);
		this.packageName = getFormattedPackageName(data.getId());
	}

	public void initializeFields(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		String packageName = getFormattedPackageName(id);
		initializeOption(KEY_PACKAGE_NAME, packageName);
		this.packageName = getFormattedPackageName(id);
	}

	public String getStringOption(String name) {
		if (name.equals(KEY_PACKAGE_NAME)) {
			return packageName;
		}
		return super.getStringOption(name);
	}
}
