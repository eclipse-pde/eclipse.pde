/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.product.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.build.*;
import org.eclipse.pde.internal.ui.wizards.product.*;
import org.eclipse.ui.progress.*;


public class ProductExportWizard extends BaseExportWizard {
	
	private static final String STORE_SECTION = "ProductExportWizard"; //$NON-NLS-1$
	private IFile fFile;
	private WorkspaceProductModel fProductModel;

	public ProductExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PRODUCT_EXPORT_WIZ);
	}
	
	public ProductExportWizard(IFile file) {
		this();
		fFile = file;
	}

	protected BaseExportWizardPage createPage1() {
		return new ProductExportWizardPage(fFile == null ? getSelection() : new StructuredSelection(fFile));
	}

	protected AdvancedPluginExportPage createPage2() {
		return new AdvancedFeatureExportPage();
	}
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}

	protected void generateAntTask(PrintWriter writer) {
	}

	protected void scheduleExportJob() {
		ProductExportWizardPage page = (ProductExportWizardPage)fPage1;
		ProductExportJob job = new ProductExportJob(
										fProductModel, 
										page.getRootDirectory(), 
										page.doExportToDirectory(), 
										page.doExportSource(), 
										page.getDestination(), 
										page.getFileName());
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
	}
	
	protected boolean performPreliminaryChecks() {
		ProductExportWizardPage page = (ProductExportWizardPage)fPage1;
		fProductModel = new WorkspaceProductModel(page.getProductFile(), false);
		try {
			fProductModel.load();
			if (!fProductModel.isLoaded()) {
				MessageDialog.openError(getContainer().getShell(), "Error", "The specified product configuration is corrupt.");
				return false;
			}
		} catch (CoreException e) {
			MessageDialog.openError(getContainer().getShell(), "Error", "The specified product configuration is corrupt.");
			return false;
		}

		if (((ProductExportWizardPage)fPage1).doSync()) {
			try {
				getContainer().run(false, false, new SynchronizationOperation(fProductModel.getProduct(), getContainer().getShell()));
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getContainer().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}		
		return true;
	}

}
