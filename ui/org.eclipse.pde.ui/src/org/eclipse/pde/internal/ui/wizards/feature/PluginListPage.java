/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 247265
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import com.ibm.icu.text.Collator;
import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class PluginListPage extends BasePluginListPage {
	class PluginContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return PluginRegistry.getActiveModels();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// If the PDE models are not initialized, initialize with option to cancel
			if (newInput != null && !PDECore.getDefault().areModelsInitialized()) {
				try {
					getContainer().run(true, false, monitor -> {
						// Target reloaded method clears existing models (which don't exist currently) and inits them with a progress monitor
						PDECore.getDefault().getModelManager().targetReloaded(monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					});
				} catch (InvocationTargetException e) {
				} catch (InterruptedException e) {
				}
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private Combo fLaunchConfigsCombo;
	private Button fInitLaunchConfigButton;
	private CheckboxTreeViewer pluginViewer;
	private static final String S_INIT_LAUNCH = "initLaunch"; //$NON-NLS-1$

	public PluginListPage() {
		super("pluginListPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.NewFeatureWizard_PlugPage_title);
		setDescription(PDEUIMessages.NewFeatureWizard_PlugPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		GridData gd;

		String[] launchConfigs = getLaunchConfigurations();

		IDialogSettings settings = getDialogSettings();
		boolean initLaunch = (settings != null) ? settings.getBoolean(S_INIT_LAUNCH) && launchConfigs.length > 0 : false;

		if (launchConfigs.length > 0) {
			fInitLaunchConfigButton = new Button(container, SWT.RADIO);
			fInitLaunchConfigButton.setText(PDEUIMessages.PluginListPage_initializeFromLaunch);
			fInitLaunchConfigButton.setSelection(initLaunch);
			fInitLaunchConfigButton.addSelectionListener(widgetSelectedAdapter(e -> {
				boolean initLaunchConfigs = fInitLaunchConfigButton.getSelection();
				fLaunchConfigsCombo.setEnabled(initLaunchConfigs);
				treePart.setEnabled(!initLaunchConfigs);
			}));

			fLaunchConfigsCombo = new Combo(container, SWT.READ_ONLY);
			fLaunchConfigsCombo.setItems(launchConfigs);
			gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			gd.horizontalSpan = 3;
			fLaunchConfigsCombo.setLayoutData(gd);
			fLaunchConfigsCombo.select(0);
			fLaunchConfigsCombo.setEnabled(initLaunch);

			Button initPluginsButton = new Button(container, SWT.RADIO);
			initPluginsButton.setText(PDEUIMessages.PluginListPage_initializeFromPlugins);
			gd = new GridData();
			gd.horizontalSpan = 4;
			initPluginsButton.setLayoutData(gd);
			initPluginsButton.setSelection(!initLaunch);
		}

		treePart.createControl(container, 4, true);
		pluginViewer = treePart.getTreeViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		gd = (GridData) treePart.getControl().getLayoutData();
		gd.horizontalIndent = 0;
		gd.heightHint = 250;
		gd.widthHint = 300;
		pluginViewer.setInput(PDECore.getDefault().getModelManager());
		treePart.setSelection(new Object[0]);
		treePart.setEnabled(!initLaunch);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_FEATURE_REFERENCED_PLUGINS);
		pluginViewer.addDoubleClickListener(event -> {
			TreeItem firstTI = pluginViewer.getTree().getSelection()[0];
			treePart.getTreeViewer().setChecked(firstTI.getData(), !firstTI.getChecked());
			treePart.updateCounterLabel();
		});
	}

	public IPluginBase[] getSelectedPlugins() {
		if (fInitLaunchConfigButton == null || !fInitLaunchConfigButton.getSelection()) {
			Object[] result = treePart.getTreeViewer().getCheckedLeafElements();
			IPluginBase[] plugins = new IPluginBase[result.length];
			for (int i = 0; i < result.length; i++) {
				IPluginModelBase model = (IPluginModelBase) result[i];
				plugins[i] = model.getPluginBase();
			}
			return plugins;
		}
		return new IPluginBase[0];
	}

	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_INIT_LAUNCH, fInitLaunchConfigButton != null && fInitLaunchConfigButton.getSelection());
	}

	private String[] getLaunchConfigurations() {
		TreeSet<String> launcherNames = new TreeSet<>(Collator.getInstance());
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			String[] types = new String[] {EclipseLaunchShortcut.CONFIGURATION_TYPE, IPDELauncherConstants.OSGI_CONFIGURATION_TYPE};
			for (int j = 0; j < 2; j++) {
				ILaunchConfigurationType type = manager.getLaunchConfigurationType(types[j]);
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				for (int i = 0; i < configs.length; i++) {
					if (!DebugUITools.isPrivate(configs[i]))
						launcherNames.add(configs[i].getName());
				}
			}
		} catch (CoreException e) {
		}
		return launcherNames.toArray(new String[launcherNames.size()]);
	}

	public ILaunchConfiguration getSelectedLaunchConfiguration() {
		if (fInitLaunchConfigButton == null || !fInitLaunchConfigButton.getSelection())
			return null;

		String configName = fLaunchConfigsCombo.getText();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			String[] types = new String[] {EclipseLaunchShortcut.CONFIGURATION_TYPE, IPDELauncherConstants.OSGI_CONFIGURATION_TYPE};
			for (int j = 0; j < 2; j++) {
				ILaunchConfigurationType type = manager.getLaunchConfigurationType(types[j]);
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				for (int i = 0; i < configs.length; i++) {
					if (configs[i].getName().equals(configName) && !DebugUITools.isPrivate(configs[i]))
						return configs[i];
				}
			}
		} catch (CoreException e) {
		}
		return null;
	}

}
