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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.build.FeatureExportInfo;
import org.eclipse.pde.internal.ui.build.ProductExportJob;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.ui.progress.IProgressConstants;
import org.w3c.dom.Document;


public class ProductExportWizard extends BaseExportWizard {
	
	private static final String STORE_SECTION = "ProductExportWizard"; //$NON-NLS-1$
	private IFile fFile;
	private WorkspaceProductModel fProductModel;
	private CrossPlatformExportPage fPage2;

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

	public void addPages() {
		super.addPages();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel("org.eclipse.platform.launchers"); //$NON-NLS-1$
		if (model != null) {
			fPage2 = new CrossPlatformExportPage("environment", model); //$NON-NLS-1$
			addPage(fPage2);
		}	
	}
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}

	protected Document generateAntTask() {
		return null;
	}

	protected void scheduleExportJob() {
		ProductExportWizardPage page = (ProductExportWizardPage)fPage1;
		
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = page.doExportToDirectory();
		info.exportSource = page.doExportSource();
		info.destinationDirectory = page.getDestination();
		info.zipFileName = page.getFileName();
		info.targets = fPage2 == null ? null : 
				page.doMultiPlatform() ? fPage2.getTargets() : null;
		info.javacSource = page.getJavacSource();
		info.javacTarget = page.getJavacTarget();
		if (fProductModel.getProduct().useFeatures())
			info.items = getFeatureModels();
		else
			info.items = getPluginModels();
		
		String rootDirectory = page.getRootDirectory();
		if ("".equals(rootDirectory.trim())) //$NON-NLS-1$
			rootDirectory = ".";  //$NON-NLS-1$
		ProductExportJob job = new ProductExportJob(info, fProductModel, rootDirectory);
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
	}
	
	private IFeatureModel[] getFeatureModels() {
		ArrayList list = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault()
				.getFeatureModelManager();
		IProductFeature[] features = fProductModel.getProduct().getFeatures();
		for (int i = 0; i < features.length; i++) {
			IFeatureModel model = manager.findFeatureModel(features[i].getId(),
					features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private BundleDescription[] getPluginModels() {
		ArrayList list = new ArrayList();
		State state = TargetPlatform.getState();
		IProductPlugin[] plugins = fProductModel.getProduct().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription bundle = state.getBundle(plugins[i].getId(), null);
			if (bundle != null)
				list.add(bundle);
		}
		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
	}
	
	protected boolean performPreliminaryChecks() {
		ProductExportWizardPage page = (ProductExportWizardPage)fPage1;
		fProductModel = new WorkspaceProductModel(page.getProductFile(), false);
		try {
			fProductModel.load();
			if (!fProductModel.isLoaded()) {
				MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductExportWizard_error, PDEUIMessages.ProductExportWizard_corrupt); // 
				return false;
			}
		} catch (CoreException e) {
			MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductExportWizard_error, PDEUIMessages.ProductExportWizard_corrupt); // 
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
