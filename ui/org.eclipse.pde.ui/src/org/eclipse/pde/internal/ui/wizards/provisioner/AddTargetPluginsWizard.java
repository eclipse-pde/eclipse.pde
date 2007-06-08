/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.ui.IProvisionerWizard;
import org.eclipse.swt.graphics.Image;

public class AddTargetPluginsWizard extends NewWizard {
	
	private static final String PROVISIONER_POINT = "targetProvisioners"; //$NON-NLS-1$
	private ProvisionerListSelectionPage fSelectionPage = null;
	private File[] fDirs = null;
	private IProvisionerWizard fWizard = null;
	
	public AddTargetPluginsWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.AddTargetPluginsWizard_windowTitle); 
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		setForcePreviousAndNextButtons(true);
		ElementList list = getAvailableProvisioners();
		if (list.size() == 1) {
			try {
				fWizard = (IProvisionerWizard)((WizardElement)list.getChildren()[0]).createExecutableExtension();
			} catch (CoreException e) {
				MessageDialog.openError(
						getContainer().getShell(), 
						PDEUIMessages.Errors_CreationError, 
						PDEUIMessages.Errors_CreationError_NoWizard); 
			}
			fWizard.addPages();
			IWizardPage[] pages = fWizard.getPages();
			for (int i = 0; i < pages.length; i++)
				addPage(pages[i]);
		} else {
			fSelectionPage = new ProvisionerListSelectionPage(getAvailableProvisioners());
			addPage(fSelectionPage);
		}
		super.addPages();
	}
	
	private ElementList getAvailableProvisioners() {
		ElementList list = new ElementList(PROVISIONER_POINT);
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), PROVISIONER_POINT);
		if (point == null)
			return list;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				WizardElement element = createWizardElement(elements[j]);
				if (element != null) {
					list.add(element);
				}
			}
		}
		return list;
	}
	
	protected WizardElement createWizardElement(IConfigurationElement config) {
		String name = config.getAttribute(WizardElement.ATT_NAME);
		String id = config.getAttribute(WizardElement.ATT_ID);
		if (name == null || id == null)
			return null;
		WizardElement element = new WizardElement(config);
		
		String imageName = config.getAttribute(WizardElement.ATT_ICON);
		Image image = null;
		if (imageName != null) {
			String pluginID = config.getNamespaceIdentifier();
			image =
				PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pluginID, imageName);
		}
		element.setImage(image);
		return element;
	}

	public boolean canFinish() {
		return ((fSelectionPage != null && getPageCount() > 1) || fSelectionPage == null) && super.canFinish();
	}

	public boolean performFinish() {
		IProvisionerWizard wizard = (fSelectionPage != null) ? (IProvisionerWizard)fSelectionPage.getSelectedWizard() :
			fWizard;
		if (wizard == null)
			return true;
		fDirs = wizard.getLocations();
		return super.performFinish();
	}
	
	public File[] getDirectories() {
		return (fDirs == null) ? new File[0] : fDirs;
	}

}
