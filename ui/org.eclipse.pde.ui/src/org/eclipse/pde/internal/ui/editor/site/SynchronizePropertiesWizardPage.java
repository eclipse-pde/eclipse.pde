/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 181878
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

	public static final int SELECTED_FEATURES = 1;

	private static final String PREFIX = PDEPlugin.getPluginId() + ".synchronizeFeatueEnvironment."; //$NON-NLS-1$

	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode"; //$NON-NLS-1$

	private Button fAllFeaturesButton;

	private final ISiteModel fModel;

	private Button fSelectedFeaturesButton;

	private final ISiteFeature[] fSiteFeatures;

	/**
	 *
	 * @param siteFeatures
	 *            selected feature or null
	 */
	public SynchronizePropertiesWizardPage(ISiteFeature[] siteFeatures, ISiteModel model) {
		super("featureSelection"); //$NON-NLS-1$
		setTitle(PDEUIMessages.SynchronizePropertiesWizardPage_title);
		setDescription(PDEUIMessages.SynchronizePropertiesWizardPage_desc);
		fSiteFeatures = siteFeatures != null ? siteFeatures : new ISiteFeature[0];
		fModel = model;
	}

	@Override
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

		fSelectedFeaturesButton = new Button(group, SWT.RADIO);
		fSelectedFeaturesButton.setText(PDEUIMessages.SynchronizePropertiesWizardPage_selectedFeatures);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fSelectedFeaturesButton.setLayoutData(gd);

		fAllFeaturesButton = new Button(group, SWT.RADIO);
		fAllFeaturesButton.setText(PDEUIMessages.SynchronizePropertiesWizardPage_allFeatures);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fAllFeaturesButton.setLayoutData(gd);

		setControl(container);
		Dialog.applyDialogFont(container);
		loadSettings();
		// TODO add own F1 context
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.FEATURE_SYNCHRONIZE_VERSIONS);
	}

	public boolean finish() {
		final int mode = saveSettings();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor monitor) {
				try {
					runOperation(mode, monitor);
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
	 * @return IFeatureModel or null
	 */
	private IFeature getFeature(ISiteFeature siteFeature) {
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModel(siteFeature.getId(), siteFeature.getVersion());
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
		for (IFeatureImport featureImport : imports) {
			if (featureImport.isPatch()) {
				patch = true;
				break;
			}
		}
		final boolean isPatch = patch;
		getShell().getDisplay().syncExec(() -> {
			try {
				siteFeature.setNL(feature.getNL());
				siteFeature.setOS(feature.getOS());
				siteFeature.setWS(feature.getWS());
				siteFeature.setArch(feature.getArch());
				siteFeature.setIsPatch(isPatch);
			} catch (CoreException ce) {
				PDEPlugin.log(ce);
			}
		});
	}

	private void importEnvironment(ISiteFeature[] siteFeatures, IProgressMonitor monitor) {
		for (ISiteFeature siteFeature : siteFeatures) {
			if (monitor.isCanceled()) {
				return;
			}
			monitor.subTask(siteFeature.getId() + "_" + siteFeature.getVersion()); //$NON-NLS-1$
			importEnvironment(siteFeature);
			monitor.worked(1);
		}
	}

	private void loadSettings() {
		if (fSiteFeatures != null) {
			IDialogSettings settings = getDialogSettings();
			if (settings.get(PROP_SYNCHRO_MODE) != null) {
				int mode = settings.getInt(PROP_SYNCHRO_MODE);
				switch (mode) {
					case SELECTED_FEATURES -> fSelectedFeaturesButton.setSelection(true);
					case ALL_FEATURES -> fAllFeaturesButton.setSelection(true);
					default -> fSelectedFeaturesButton.setSelection(true);
				}
			} else
				fSelectedFeaturesButton.setSelection(true);
		} else {
			fSelectedFeaturesButton.setEnabled(false);
			fAllFeaturesButton.setSelection(true);
		}
	}

	private void runOperation(int mode, IProgressMonitor monitor) {
		ISiteFeature[] siteFeatures;
		if (mode == SELECTED_FEATURES) {
			siteFeatures = fSiteFeatures;
		} else {
			siteFeatures = fModel.getSite().getFeatures();
		}
		int size = siteFeatures.length;
		monitor.beginTask(PDEUIMessages.SynchronizePropertiesWizardPage_synchronizing, size);
		importEnvironment(siteFeatures, monitor);
	}

	private int saveSettings() {
		IDialogSettings settings = getDialogSettings();

		int mode = SELECTED_FEATURES;

		if (fAllFeaturesButton.getSelection())
			mode = ALL_FEATURES;
		settings.put(PROP_SYNCHRO_MODE, mode);
		return mode;
	}
}
