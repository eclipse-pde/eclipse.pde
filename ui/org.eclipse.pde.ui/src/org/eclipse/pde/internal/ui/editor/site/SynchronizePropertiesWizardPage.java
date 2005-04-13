/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class SynchronizePropertiesWizardPage extends WizardPage {

	public static final int ALL_FEATURES = 2;

	public static final int ONE_FEATURE = 1;

	private static final String PREFIX = PDEPlugin.getPluginId()
			+ ".synchronizeFeatueEnvironment."; //$NON-NLS-1$

	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode"; //$NON-NLS-1$

	private Button fAllFeaturesButton;

	private ISiteModel fModel;

	private Button fOneFeatureButton;

	private ISiteFeature fSiteFeature;

	/**
	 * 
	 * @param siteFeature
	 *            selected feature or null
	 */
	public SynchronizePropertiesWizardPage(ISiteFeature siteFeature,
			ISiteModel model) {
		super("featureSelection"); //$NON-NLS-1$
		setTitle(PDEUIMessages.SynchronizePropertiesWizardPage_title);
		setDescription(PDEUIMessages.SynchronizePropertiesWizardPage_desc);
		fSiteFeature = siteFeature != null && getFeature(siteFeature) != null ? siteFeature
				: null;
		fModel = model;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		group.setLayout(layout);
		group.setLayoutData(gd);
		group.setText(PDEUIMessages.SynchronizePropertiesWizardPage_group);

		fOneFeatureButton = new Button(group, SWT.RADIO);
		fOneFeatureButton.setText(PDEUIMessages.SynchronizePropertiesWizardPage_oneFeature);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fOneFeatureButton.setLayoutData(gd);

		fAllFeaturesButton = new Button(group, SWT.RADIO);
		fAllFeaturesButton.setText(PDEUIMessages.SynchronizePropertiesWizardPage_allFeatures);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fAllFeaturesButton.setLayoutData(gd);

		setControl(container);
		Dialog.applyDialogFont(container);
		loadSettings();
		// TODO add own F1 context
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
				IHelpContextIds.FEATURE_SYNCHRONIZE_VERSIONS);
	}

	public boolean finish() {
		final int mode = saveSettings();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					runOperation(mode, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} catch (InvocationTargetException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param siteFeature
	 * @return IFeatureModel or null
	 */
	private IFeature getFeature(ISiteFeature siteFeature) {
		IFeatureModel model = PDECore
				.getDefault()
				.getFeatureModelManager()
				.findFeatureModel(siteFeature.getId(), siteFeature.getVersion());
		if (model != null)
			return model.getFeature();
		return null;
	}

	private void importEnvironment(final ISiteFeature siteFeature) {
		final IFeature feature = getFeature(siteFeature);
		if (feature == null) {
			return;
		}
		boolean patch = false;
		IFeatureImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isPatch()) {
				patch = true;
				break;
			}
		}
		final boolean isPatch = patch;
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				try {
					siteFeature.setNL(feature.getNL());
					siteFeature.setOS(feature.getOS());
					siteFeature.setWS(feature.getWS());
					siteFeature.setArch(feature.getArch());
					siteFeature.setIsPatch(isPatch);
				} catch (CoreException ce) {
					PDEPlugin.log(ce);
				}
			}
		});
	}

	private void importEnvironment(ISiteFeature[] siteFeatures,
			IProgressMonitor monitor) {
		for (int i = 0; i < siteFeatures.length; i++) {
			if (monitor.isCanceled()) {
				return;
			}
			monitor.subTask(siteFeatures[i].getId()
					+ "_" + siteFeatures[i].getVersion()); //$NON-NLS-1$
			importEnvironment(siteFeatures[i]);
			monitor.worked(1);
		}
	}

	private void loadSettings() {
		if (fSiteFeature != null) {
			IDialogSettings settings = getDialogSettings();
			if (settings.get(PROP_SYNCHRO_MODE) != null) {
				int mode = settings.getInt(PROP_SYNCHRO_MODE);
				switch (mode) {
				case ONE_FEATURE:
					fOneFeatureButton.setSelection(true);
					break;
				case ALL_FEATURES:
					fAllFeaturesButton.setSelection(true);
					break;
				default:
					fOneFeatureButton.setSelection(true);
				}
			} else
				fOneFeatureButton.setSelection(true);
		} else {
			fOneFeatureButton.setEnabled(false);
			fAllFeaturesButton.setSelection(true);
		}
	}

	private void runOperation(int mode, IProgressMonitor monitor)
			throws CoreException, InvocationTargetException {
		ISiteFeature[] siteFeatures;
		if (mode == ONE_FEATURE) {
			siteFeatures = new ISiteFeature[] { fSiteFeature };
		} else {
			siteFeatures = fModel.getSite().getFeatures();
		}
		int size = siteFeatures.length;
		monitor.beginTask(PDEUIMessages.SynchronizePropertiesWizardPage_synchronizing, size);
		importEnvironment(siteFeatures, monitor);
	}

	private int saveSettings() {
		IDialogSettings settings = getDialogSettings();

		int mode = ONE_FEATURE;

		if (fAllFeaturesButton.getSelection())
			mode = ALL_FEATURES;
		settings.put(PROP_SYNCHRO_MODE, mode);
		return mode;
	}
}
