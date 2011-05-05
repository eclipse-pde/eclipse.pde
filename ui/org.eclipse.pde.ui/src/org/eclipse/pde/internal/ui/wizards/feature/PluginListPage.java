/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import com.ibm.icu.text.Collator;
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
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class PluginListPage extends BasePluginListPage {
	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return PluginRegistry.getActiveModels();
		}
	}

	private Combo fLaunchConfigsCombo;
	private Button fInitLaunchConfigButton;
	private CheckboxTableViewer pluginViewer;
	private static final String S_INIT_LAUNCH = "initLaunch"; //$NON-NLS-1$

	public PluginListPage() {
		super("pluginListPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.NewFeatureWizard_PlugPage_title);
		setDescription(PDEUIMessages.NewFeatureWizard_PlugPage_desc);
	}

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
			fInitLaunchConfigButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean initLaunchConfigs = fInitLaunchConfigButton.getSelection();
					fLaunchConfigsCombo.setEnabled(initLaunchConfigs);
					tablePart.setEnabled(!initLaunchConfigs);
				}
			});

			fLaunchConfigsCombo = new Combo(container, SWT.READ_ONLY);
			fLaunchConfigsCombo.setItems(launchConfigs);
			gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			gd.horizontalSpan = 2;
			fLaunchConfigsCombo.setLayoutData(gd);
			fLaunchConfigsCombo.select(0);
			fLaunchConfigsCombo.setEnabled(initLaunch);

			Button initPluginsButton = new Button(container, SWT.RADIO);
			initPluginsButton.setText(PDEUIMessages.PluginListPage_initializeFromPlugins);
			gd = new GridData();
			gd.horizontalSpan = 3;
			initPluginsButton.setLayoutData(gd);
			initPluginsButton.setSelection(!initLaunch);
		}

		tablePart.createControl(container, 3, true);
		pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		gd = (GridData) tablePart.getControl().getLayoutData();
		if (launchConfigs.length > 0) {
			gd.horizontalIndent = 30;
			((GridData) tablePart.getCounterLabel().getLayoutData()).horizontalIndent = 30;
		}
		gd.heightHint = 250;
		gd.widthHint = 300;
		pluginViewer.setInput(PDECore.getDefault().getModelManager());
		tablePart.setSelection(new Object[0]);
		tablePart.setEnabled(!initLaunch);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_FEATURE_REFERENCED_PLUGINS);
		pluginViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				TableItem firstTI = pluginViewer.getTable().getSelection()[0];
				if (firstTI.getChecked()) {
					firstTI.setChecked(false);
				} else {
					firstTI.setChecked(true);
				}
				tablePart.updateCount(pluginViewer.getCheckedElements().length);
			}
		});
	}

	public IPluginBase[] getSelectedPlugins() {
		if (fInitLaunchConfigButton == null || !fInitLaunchConfigButton.getSelection()) {
			Object[] result = tablePart.getSelection();
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
		TreeSet launcherNames = new TreeSet(Collator.getInstance());
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
		return (String[]) launcherNames.toArray(new String[launcherNames.size()]);
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
