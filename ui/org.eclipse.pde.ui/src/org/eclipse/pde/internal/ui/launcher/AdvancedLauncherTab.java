/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.help.WorkbenchHelp;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {
	private static final String KEY_NAME = "AdvancedLauncherTab.name";
	private static final String KEY_WORKSPACE_PLUGINS =
		"AdvancedLauncherTab.workspacePlugins";
	private static final String KEY_EXTERNAL_PLUGINS =
		"AdvancedLauncherTab.externalPlugins";
	private static final String KEY_USE_DEFAULT =
		"AdvancedLauncherTab.useDefault";
	private static final String KEY_USE_FEATURES =
		"AdvancedLauncherTab.useFeatures";
	private static final String KEY_USE_LIST = "AdvancedLauncherTab.useList";
	private static final String KEY_VISIBLE_LIST =
		"AdvancedLauncherTab.visibleList";
	private static final String KEY_DEFAULTS = "AdvancedLauncherTab.defaults";
	private static final String KEY_PLUGIN_PATH =
		"AdvancedLauncherTab.pluginPath";
	private static final String KEY_PLUGIN_PATH_TITLE =
		"AdvancedLauncherTab.pluginPath.title";
	private static final String KEY_ERROR_NO_PLUGINS =
		"AdvancedLauncherTab.error.no_plugins";
	private static final String KEY_ERROR_NO_BOOT =
		"AdvancedLauncherTab.error.no_boot";
	private static final String KEY_ERROR_BROKEN_PLUGINS =
		"AdvancedLauncherTab.error.brokenPlugins";
	private static final String KEY_ERROR_FEATURE_SETUP =
		"AdvancedLauncherTab.error.featureSetup";

	private Button useDefaultRadio;
	private Button useFeaturesRadio;
	private Button useListRadio;
	private CheckboxTreeViewer pluginTreeViewer;
	private Label visibleLabel;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private IPluginModelBase[] externalModels;
	private IPluginModelBase[] workspaceModels;
	private Button defaultsButton;
	private Button pluginPathButton;
	private int numExternalChecked = 0;
	private int numWorkspaceChecked = 0;
	private Image image;
	private boolean showFeatures = true;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModelBase)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins)
				return externalModels;
			if (parent == workspacePlugins)
				return workspaceModels;
			return new Object[0];
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}

	public AdvancedLauncherTab() {
		this(true);
	}
	
	public AdvancedLauncherTab(boolean showFeatures) {
		this.showFeatures = showFeatures;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		image = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		externalModels = PDECore.getDefault().getExternalModelManager().getAllModels();
		workspaceModels = PDECore.getDefault().getWorkspaceModelManager().getAllModels();
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		image.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		createStartingSpace(composite, 1);

		useDefaultRadio = new Button(composite, SWT.RADIO);
		useDefaultRadio.setText(PDEPlugin.getResourceString(KEY_USE_DEFAULT));

		if (showFeatures) {
			useFeaturesRadio = new Button(composite, SWT.RADIO);
			useFeaturesRadio.setText(PDEPlugin.getResourceString(KEY_USE_FEATURES));
		}

		useListRadio = new Button(composite, SWT.RADIO);
		useListRadio.setText(PDEPlugin.getResourceString(KEY_USE_LIST));

		visibleLabel = new Label(composite, SWT.NULL);
		visibleLabel.setText(PDEPlugin.getResourceString(KEY_VISIBLE_LIST));

		Control list = createPluginList(composite);
		list.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite buttonContainer = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		defaultsButton = new Button(buttonContainer, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_DEFAULTS));
		defaultsButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(defaultsButton);

		pluginPathButton = new Button(buttonContainer, SWT.PUSH);
		pluginPathButton.setText(PDEPlugin.getResourceString(KEY_PLUGIN_PATH));
		pluginPathButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(pluginPathButton);

		hookListeners();
		setControl(composite);

		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	private void hookListeners() {
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultChanged();
			}
		};
		useDefaultRadio.addSelectionListener(adapter);
		if (showFeatures)
			useFeaturesRadio.addSelectionListener(adapter);
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				computeInitialCheckState();
				updateStatus();
				setChanged(true);
			}
		});
		pluginPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPluginPaths();
			}
		});
	}

	private void useDefaultChanged() {
		adjustCustomControlEnableState(useListRadio.getSelection());
		if (showFeatures)
			pluginPathButton.setEnabled(!useFeaturesRadio.getSelection());
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		visibleLabel.setVisible(enable);
		pluginTreeViewer.getTree().setVisible(enable);
		defaultsButton.setVisible(enable);
	}

	protected Control createPluginList(final Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final Object element = event.getElement();
				BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
					public void run() {
						if (element instanceof IPluginModelBase) {
							handleCheckStateChanged((IPluginModelBase)element, event.getChecked());
						} else {
							handleGroupStateChanged(element, event.getChecked());
						}
						updateStatus();
					}
				});
			}
		});
		pluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
			public int category(Object obj) {
				if (obj == workspacePlugins)
					return -1;
				return 0;
			}
		});

		Image pluginsImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);

		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS),
				pluginsImage);
		return pluginTreeViewer.getTree();
	}

	public void initializeFrom(ILaunchConfiguration config) {
	}

	private void initWorkspacePluginsState(ILaunchConfiguration config)
		throws CoreException {
		numWorkspaceChecked = workspaceModels.length;
		pluginTreeViewer.setSubtreeChecked(workspacePlugins, true);

		TreeSet deselected = LauncherUtils.parseDeselectedWSIds(config);
		for (int i = 0; i < workspaceModels.length; i++) {
			if (deselected.contains(workspaceModels[i].getPluginBase().getId())) {
				if (pluginTreeViewer.setChecked(workspaceModels[i], false))
					numWorkspaceChecked -= 1;
			}
		}

		if (numWorkspaceChecked == 0)
			pluginTreeViewer.setChecked(workspacePlugins, false);
		pluginTreeViewer.setGrayed(
			workspacePlugins,
			numWorkspaceChecked > 0 && numWorkspaceChecked < workspaceModels.length);
	}

	private void initExternalPluginsState(ILaunchConfiguration config)
		throws CoreException {
		numExternalChecked = 0;

		TreeSet selected = LauncherUtils.parseSelectedExtIds(config);
		for (int i = 0; i < externalModels.length; i++) {
			if (selected.contains(externalModels[i].getPluginBase().getId())) {
				if (pluginTreeViewer.setChecked(externalModels[i], true))
					numExternalChecked += 1;
			}
		}

		pluginTreeViewer.setChecked(externalPlugins, numExternalChecked > 0);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			numExternalChecked > 0 && numExternalChecked < externalModels.length);
	}

	public void initialize(ILaunchConfiguration config) {
		try {
			useDefaultRadio.setSelection(config.getAttribute(USECUSTOM, true));
			if (showFeatures) {
				useFeaturesRadio.setSelection(config.getAttribute(USEFEATURES, false));
				useListRadio.setSelection(
					!useDefaultRadio.getSelection() && !useFeaturesRadio.getSelection());
			} else {
				useListRadio.setSelection(!useDefaultRadio.getSelection());
			}
			if (pluginTreeViewer.getInput() == null) {
				pluginTreeViewer.setUseHashlookup(true);
				pluginTreeViewer.setInput(PDEPlugin.getDefault());
				pluginTreeViewer.reveal(workspacePlugins);
			}

			if (useDefaultRadio.getSelection()) {
				computeInitialCheckState();
			} else if (useListRadio.getSelection()) {
				initWorkspacePluginsState(config);
				initExternalPluginsState(config);
			} else {
				pluginPathButton.setEnabled(false);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		adjustCustomControlEnableState(useListRadio.getSelection());
		updateStatus();
	}

	private void computeInitialCheckState() {
		TreeSet wtable = new TreeSet();
		numWorkspaceChecked = 0;
		numExternalChecked = 0;

		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModelBase model = workspaceModels[i];
			numWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.add(model.getPluginBase().getId());
		}

		if (numWorkspaceChecked > 0) {
			pluginTreeViewer.setSubtreeChecked(workspacePlugins, true);
		}

		numExternalChecked = 0;
		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = externalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				pluginTreeViewer.setChecked(model, true);
				numExternalChecked += 1;
			}
		}

		pluginTreeViewer.setChecked(externalPlugins, numExternalChecked > 0);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			numExternalChecked > 0 && numExternalChecked < externalModels.length);
	}

	private void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		if (model.getUnderlyingResource() == null) {
			if (checked) {
				numExternalChecked += 1;
			} else {
				numExternalChecked -= 1;
			}
			pluginTreeViewer.setChecked(externalPlugins, numExternalChecked > 0);
			pluginTreeViewer.setGrayed(
				externalPlugins,
				numExternalChecked > 0 && numExternalChecked < externalModels.length);
		} else {
			if (checked) {
				numWorkspaceChecked += 1;
			} else {
				numWorkspaceChecked -= 1;
			}
			pluginTreeViewer.setChecked(workspacePlugins, numWorkspaceChecked > 0);
			pluginTreeViewer.setGrayed(
				workspacePlugins,
				numWorkspaceChecked > 0 && numWorkspaceChecked < workspaceModels.length);
		}
		setChanged(true);
	}

	private void handleGroupStateChanged(Object group, boolean checked) {
		pluginTreeViewer.setSubtreeChecked(group, checked);
		pluginTreeViewer.setGrayed(group, false);

		if (group == workspacePlugins)
			numWorkspaceChecked = checked ? workspaceModels.length : 0;
		else if (group == externalPlugins)
			numExternalChecked = checked ? externalModels.length : 0;

		setChanged(true);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USECUSTOM, true);
		if (showFeatures)
			config.setAttribute(USEFEATURES, false);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (!isChanged())
			return;

		final ILaunchConfigurationWorkingCopy config = configuration;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				config.setAttribute(USECUSTOM, useDefaultRadio.getSelection());
				if (showFeatures)
					config.setAttribute(USEFEATURES, useFeaturesRadio.getSelection());
				if (useListRadio.getSelection()) {
					// store deselected projects
					StringBuffer wbuf = new StringBuffer();
					for (int i = 0; i < workspaceModels.length; i++) {
						IPluginModelBase model = (IPluginModelBase) workspaceModels[i];
						if (!pluginTreeViewer.getChecked(model))
							wbuf.append(
								model.getPluginBase().getId() + File.pathSeparatorChar);
					}
					config.setAttribute(WSPROJECT, wbuf.toString());

					// Store selected external models
					StringBuffer exbuf = new StringBuffer();
					Object[] checked = pluginTreeViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						if (checked[i] instanceof ExternalPluginModelBase) {
							IPluginModelBase model = (IPluginModelBase) checked[i];
							exbuf.append(
								model.getPluginBase().getId() + File.pathSeparatorChar);
						}
					}
					config.setAttribute(EXTPLUGINS, exbuf.toString());
				}
				setChanged(false);
			}
		});
	}

	private void showPluginPaths() {
		try {
			URL[] urls = TargetPlatform.createPluginPath(getPlugins());
			PluginPathDialog dialog =
				new PluginPathDialog(pluginPathButton.getShell(), urls);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString(KEY_PLUGIN_PATH_TITLE));
			dialog.getShell().setSize(500, 500);
			dialog.open();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void updateStatus() {
		updateStatus(validatePlugins());
	}

	private IStatus validatePlugins() {
		if (showFeatures && useFeaturesRadio.getSelection()) {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			IPath featurePath = workspacePath.removeLastSegments(1).append("features");
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins")
				|| !featurePath.toFile().exists())
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_FEATURE_SETUP));
		} else {
			IPluginModelBase[] plugins = getPlugins();
			if (plugins.length == 0)
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_NO_PLUGINS));

			if (findModel("org.eclipse.core.boot", plugins) == null)
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_NO_BOOT));

			for (int i = 0; i < plugins.length; i++) {
				if (!plugins[i].isLoaded())
					return createStatus(
						IStatus.WARNING,
						PDEPlugin.getResourceString(KEY_ERROR_BROKEN_PLUGINS));
			}
		}
		return createStatus(IStatus.OK, "");
	}

	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			String pid = model.getPluginBase().getId();
			if (pid != null && pid.equals(id))
				return model;
		}
		return null;
	}

	private IPluginModelBase[] getPlugins() {
		if (useDefaultRadio.getSelection()) {
			HashMap map = new HashMap();
			for (int i = 0; i < workspaceModels.length; i++) {
				// check for null is to accomodate previous unclean exits (e.g. workspace crashes)
				String id = workspaceModels[i].getPluginBase().getId();
				if (id != null)
					map.put(id, workspaceModels[i]);
			}
			for (int i = 0; i < externalModels.length; i++) {
				String id = externalModels[i].getPluginBase().getId();
				if (id != null && !map.containsKey(id) && externalModels[i].isEnabled())
					map.put(id, externalModels[i]);
			}
			return (IPluginModelBase[]) map.values().toArray(
				new IPluginModelBase[map.size()]);
		}

		ArrayList result = new ArrayList();
		Object[] elements = pluginTreeViewer.getCheckedElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IPluginModelBase)
				result.add(elements[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	
	public Image getImage() {
			return image;
	}
}
