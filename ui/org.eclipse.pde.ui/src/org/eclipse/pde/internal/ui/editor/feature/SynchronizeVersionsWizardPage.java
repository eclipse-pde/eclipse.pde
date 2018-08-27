/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class SynchronizeVersionsWizardPage extends WizardPage {
	public static final int USE_PLUGINS_AT_BUILD = 0;
	public static final int USE_FEATURE = 1;
	public static final int USE_PLUGINS = 2;
	private FeatureEditor fFeatureEditor;
	private Button fUsePluginsAtBuildButton;
	private Button fUseComponentButton;
	private Button fUsePluginsButton;
	private boolean fIsForceVersionEnabled;

	private static final String PREFIX = PDEPlugin.getPluginId() + ".synchronizeVersions."; //$NON-NLS-1$
	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode"; //$NON-NLS-1$

	public SynchronizeVersionsWizardPage(FeatureEditor featureEditor) {
		super("featureJar"); //$NON-NLS-1$
		setTitle(PDEUIMessages.VersionSyncWizard_title);
		setDescription(PDEUIMessages.VersionSyncWizard_desc);
		this.fFeatureEditor = featureEditor;
		this.fIsForceVersionEnabled = isForceVersionEnabled();
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
		group.setText(PDEUIMessages.VersionSyncWizard_group);

		fUsePluginsAtBuildButton = new Button(group, SWT.RADIO);
		fUsePluginsAtBuildButton.setText(PDEUIMessages.VersionSyncWizard_usePluginsAtBuild);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUsePluginsAtBuildButton.setLayoutData(gd);

		fUsePluginsButton = new Button(group, SWT.RADIO);
		fUsePluginsButton.setText(PDEUIMessages.VersionSyncWizard_usePlugins);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUsePluginsButton.setLayoutData(gd);

		if (fIsForceVersionEnabled) {
			fUseComponentButton = new Button(group, SWT.RADIO);
			fUseComponentButton.setText(PDEUIMessages.VersionSyncWizard_useComponent);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			fUseComponentButton.setLayoutData(gd);
		}

		setControl(container);
		Dialog.applyDialogFont(container);
		loadSettings();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.FEATURE_SYNCHRONIZE_VERSIONS);
	}

	private IPluginModelBase findModel(String id) {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		for (IPluginModelBase model : models) {
			if (model != null && id.equals(model.getPluginBase().getId()))
				return model;
		}
		return null;
	}

	public boolean finish() {
		final int mode = saveSettings();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor monitor) {
				try {
					runOperation(mode, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} catch (BadLocationException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), operation, PDEPlugin.getWorkspace().getRoot());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Forces a version into plugin/fragment .xml
	 */
	private void forceVersion(final String targetVersion, IModel modelBase, IProgressMonitor monitor) {
		IFile file = (IFile) modelBase.getUnderlyingResource();
		if (file == null)
			return;

		PDEModelUtility.modifyModel(new ModelModification(file) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					modifyVersion(((IBundlePluginModelBase) model).getBundleModel(), targetVersion);
				} else if (model instanceof IPluginModelBase) {
					modifyVersion((IPluginModelBase) model, targetVersion);
				}
			}
		}, monitor);
	}

	private void modifyVersion(IBundleModel model, String targetVersion) {
		model.getBundle().setHeader(Constants.BUNDLE_VERSION, targetVersion);
	}

	private void modifyVersion(IPluginModelBase model, String version) throws CoreException {
		model.getPluginBase().setVersion(version);
	}

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings.get(PROP_SYNCHRO_MODE) != null) {
			int mode = settings.getInt(PROP_SYNCHRO_MODE);
			switch (mode) {
				case USE_FEATURE :
				if (fIsForceVersionEnabled) {
						fUseComponentButton.setSelection(true);
					} else {
						fUsePluginsAtBuildButton.setSelection(true);
					}
					break;
				case USE_PLUGINS :
					fUsePluginsButton.setSelection(true);
					break;
				default : // USE_PLUGINS_AT_BUILD
					fUsePluginsAtBuildButton.setSelection(true);
					break;
			}
		} else
			fUsePluginsAtBuildButton.setSelection(true);
	}

	private void runOperation(int mode, IProgressMonitor monitor) throws CoreException, BadLocationException {
		WorkspaceFeatureModel model = (WorkspaceFeatureModel) fFeatureEditor.getAggregateModel();
		IFeature feature = model.getFeature();
		if (fIsForceVersionEnabled) {
			IFeaturePlugin[] plugins = feature.getPlugins();
			int size = plugins.length;
			monitor.beginTask(PDEUIMessages.VersionSyncWizard_synchronizing, size);
			for (IFeaturePlugin plugin : plugins) {
				synchronizeVersion(mode, feature.getVersion(), plugin, monitor);
			}
		} else {
			IFeatureChild[] features = feature.getIncludedFeatures();
			int size = features.length;
			monitor.beginTask(PDEUIMessages.VersionSyncWizard_synchronizing, size);
			for (IFeatureChild feat : features) {
				synchronizeVersion(mode, feature.getVersion(), feat, monitor);
			}
		}
	}

	private boolean isForceVersionEnabled() {
		Object selectedPage = fFeatureEditor.getSelectedPage();
		if (selectedPage instanceof FeatureIncludesPage) {
			// We must differentiate between features and plugins pages
			// because we can't currently write current feature version into
			// included features (see org.eclipse.pde.internal.core.text.XMLEditingModel)
			return false;
		}
		return true;
	}

	private int saveSettings() {
		IDialogSettings settings = getDialogSettings();
		int mode = USE_PLUGINS_AT_BUILD;
		if (fIsForceVersionEnabled && fUseComponentButton.getSelection())
			mode = USE_FEATURE;
		else if (fUsePluginsButton.getSelection())
			mode = USE_PLUGINS;
		settings.put(PROP_SYNCHRO_MODE, mode);
		return mode;
	}

	/**
	 * @param mode
	 * @param featureVersion
	 * @param ref
	 * @param monitor
	 * @throws CoreException
	 * @throws BadLocationException
	 */
	private void synchronizeVersion(int mode, String featureVersion, IFeaturePlugin ref, IProgressMonitor monitor) throws CoreException, BadLocationException {
		String id = ref.getId();

		if (mode == USE_PLUGINS_AT_BUILD) {
			if (!ICoreConstants.DEFAULT_VERSION.equals(ref.getVersion()))
				ref.setVersion(ICoreConstants.DEFAULT_VERSION);
		} else if (mode == USE_PLUGINS) {
			IPluginModelBase modelBase = PluginRegistry.findModel(id);
			if (modelBase == null)
				return;
			String baseVersion = modelBase.getPluginBase().getVersion();
			if (!ref.getVersion().equals(baseVersion))
				ref.setVersion(baseVersion);
		} else /* mode == USE_FEATURE */{
			IPluginModelBase modelBase = findModel(id);
			if (modelBase == null)
				return;
			ref.setVersion(featureVersion);
			String baseVersion = modelBase.getPluginBase().getVersion();
			if (!featureVersion.equals(baseVersion))
				forceVersion(featureVersion, modelBase, monitor);
		}
		monitor.worked(1);
	}

	private void synchronizeVersion(int mode, String featureVersion, IFeatureChild ref, IProgressMonitor monitor)
			throws CoreException {
		String id = ref.getId();

		if (mode == USE_PLUGINS_AT_BUILD) {
			if (!ICoreConstants.DEFAULT_VERSION.equals(ref.getVersion()))
				ref.setVersion(ICoreConstants.DEFAULT_VERSION);
		} else if (mode == USE_PLUGINS) {
			FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel modelBase = fmm.findFeatureModel(id);
			if (modelBase == null)
				return;
			String baseVersion = modelBase.getFeature().getVersion();
			if (!ref.getVersion().equals(baseVersion)) {
				ref.setVersion(baseVersion);
			}
		} else /* mode == USE_FEATURE */ {
			// not supported yet
		}
		monitor.worked(1);
	}

}
