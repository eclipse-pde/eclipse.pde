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
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.plugin.IPluginBase;
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
	private Label restoreLabel;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private IPluginModelBase[] externalModels;
	private IPluginModelBase[] workspaceModels;
	private Button defaultsButton;
	private Button pluginPathButton;
	private int numExternalChecked = 0;
	private int numWorkspaceChecked = 0;
	private boolean firstReveal = true;
	private Image image;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModelBase)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				return externalModels;
			}
			if (parent == workspacePlugins) {
				return workspaceModels;
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) child;
				if (model.getUnderlyingResource() != null)
					return workspacePlugins;
				else
					return externalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}


	public AdvancedLauncherTab() {
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
		GridLayout layout = new GridLayout();
		GridData gd;
		composite.setLayout(layout);

		createStartingSpace(composite, 1);

		useDefaultRadio = new Button(composite, SWT.RADIO);
		useDefaultRadio.setText(PDEPlugin.getResourceString(KEY_USE_DEFAULT));
		fillIntoGrid(useDefaultRadio, 1, false);

		useFeaturesRadio = new Button(composite, SWT.RADIO);
		useFeaturesRadio.setText(PDEPlugin.getResourceString(KEY_USE_FEATURES));
		fillIntoGrid(useFeaturesRadio, 1, false);

		useListRadio = new Button(composite, SWT.RADIO);
		useListRadio.setText(PDEPlugin.getResourceString(KEY_USE_LIST));
		fillIntoGrid(useListRadio, 1, false);

		visibleLabel = new Label(composite, SWT.NULL);
		visibleLabel.setText(PDEPlugin.getResourceString(KEY_VISIBLE_LIST));
		fillIntoGrid(visibleLabel, 1, false);

		Control list = createPluginList(composite);
		gd = new GridData(GridData.FILL_BOTH);
		list.setLayoutData(gd);

		Composite buttonContainer = new Composite(composite, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		buttonContainer.setLayout(layout);

		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);

		pluginPathButton = new Button(buttonContainer, SWT.PUSH);
		pluginPathButton.setText(PDEPlugin.getResourceString(KEY_PLUGIN_PATH));
		pluginPathButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(pluginPathButton);

		defaultsButton = new Button(buttonContainer, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_DEFAULTS));
		defaultsButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(defaultsButton);

		hookListeners();
		setControl(composite);

		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	private void hookListeners() {
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (firstReveal) {
					pluginTreeViewer.reveal(workspacePlugins);
					firstReveal = false;
				}
				useDefaultChanged();
			}
		};
		useDefaultRadio.addSelectionListener(adapter);
		useFeaturesRadio.addSelectionListener(adapter);
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator
					.showWhile(
						pluginTreeViewer.getControl().getDisplay(),
						new Runnable() {
					public void run() {
						computeInitialCheckState();
						updateStatus();
						setChanged(true);
					}
				});
			}
		});
		pluginPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPluginPaths();
			}
		});
	}

	private void useDefaultChanged() {
		boolean useDefault = !useListRadio.getSelection();
		adjustCustomControlEnableState(!useDefault);
		pluginPathButton.setEnabled(!useFeaturesRadio.getSelection());
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		//		visibleLabel.setEnabled(enable);
		//		pluginTreeViewer.getTree().setEnabled(enable);
		//		defaultsButton.setEnabled(enable);
		visibleLabel.setVisible(enable);
		pluginTreeViewer.getTree().setVisible(enable);
		defaultsButton.setVisible(enable);
	}

	private GridData fillIntoGrid(Control control, int hspan, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = hspan;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	protected Control createPluginList(final Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final Object element = event.getElement();
				BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
					public void run() {
						if (element instanceof IPluginModelBase) {
							IPluginModelBase model =
								(IPluginModelBase) event.getElement();
							handleCheckStateChanged(model, event.getChecked());
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
				if (obj == externalPlugins)
					return 1;
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

	public static TreeSet parseDeselectedWSIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet deselected = new TreeSet();
		String deselectedPluginIDs = config.getAttribute(WSPROJECT, (String) null);
		if (deselectedPluginIDs != null) {
			StringTokenizer tok =
				new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				deselected.add(token);
			}
		}
		return deselected;
	}
	
	public static TreeSet parseSelectedExtIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet selected = new TreeSet();
		String selectedPluginIDs = config.getAttribute(EXTPLUGINS, (String) null);
		if (selectedPluginIDs != null) {
			StringTokenizer tok =
				new StringTokenizer(selectedPluginIDs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int loc = token.lastIndexOf(',');
				if (loc == -1) {
					selected.add(token);
				} else if (token.charAt(loc + 1) == 't') {
					selected.add(token.substring(0, loc));
				}
			}
		}
		return selected;
	}

	private void initWorkspacePluginsState(ILaunchConfiguration config)
		throws CoreException {

		numWorkspaceChecked = workspaceModels.length;
		pluginTreeViewer.setSubtreeChecked(workspacePlugins, true);

		TreeSet deselected = parseDeselectedWSIds(config);
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

		numExternalChecked = externalModels.length;
		pluginTreeViewer.setSubtreeChecked(externalPlugins, true);

		TreeSet selected = parseSelectedExtIds(config);
		for (int i = 0; i < externalModels.length; i++) {
			if (!selected.contains(externalModels[i].getPluginBase().getId())) {
				if (pluginTreeViewer.setChecked(externalModels[i], false))
					numExternalChecked -= 1;
			}
		}

		if (numExternalChecked == 0)
			pluginTreeViewer.setChecked(externalPlugins, false);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			numExternalChecked > 0 && numExternalChecked < externalModels.length);
	}

	public void initialize(ILaunchConfiguration config) {

		try {
			useDefaultRadio.setSelection(config.getAttribute(USECUSTOM, true));
			useFeaturesRadio.setSelection(config.getAttribute(USEFEATURES, false));
			useListRadio.setSelection(
				!useDefaultRadio.getSelection() && !useFeaturesRadio.getSelection());
			if (pluginTreeViewer.getInput() == null)
				//pluginTreeViewer.setUseHashlookup(true);
				pluginTreeViewer.setInput(PDEPlugin.getDefault());

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

		pluginTreeViewer.setSubtreeChecked(externalPlugins, true);
		numExternalChecked = externalModels.length;
		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = externalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (masked || !model.isEnabled()) {
				pluginTreeViewer.setChecked(model, false);
				numExternalChecked -= 1;
			}
		}

		if (numExternalChecked == 0)
			pluginTreeViewer.setChecked(externalPlugins, false);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			numExternalChecked > 0
				&& numExternalChecked < externalModels.length);

	}

	private void handleCheckStateChanged(
		IPluginModelBase model,
		boolean checked) {

		if (model.getUnderlyingResource() == null) {
			if (checked) {
				numExternalChecked += 1;
			} else {
				numExternalChecked -= 1;
			}
			pluginTreeViewer.setChecked(
				externalPlugins,
				numExternalChecked > 0);
			pluginTreeViewer.setGrayed(
				externalPlugins,
				numExternalChecked > 0
					&& numExternalChecked < externalModels.length);
		} else {
			if (checked) {
				numWorkspaceChecked += 1;
			} else {
				numWorkspaceChecked -= 1;
			}
			pluginTreeViewer.setChecked(
				workspacePlugins,
				numWorkspaceChecked > 0);
			pluginTreeViewer.setGrayed(
				workspacePlugins,
				numWorkspaceChecked > 0
					&& numWorkspaceChecked < workspaceModels.length);
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
		config.setAttribute(USEFEATURES, false);
	}


	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (!isChanged())
			return;

		final ILaunchConfigurationWorkingCopy config = configuration;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				config.setAttribute(USECUSTOM, useDefaultRadio.getSelection());
				config.setAttribute(USEFEATURES, useFeaturesRadio.getSelection());

				if (!useListRadio.getSelection()) {
					setChanged(false);
					return;
				}
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
					Object element = checked[i];
					if (element instanceof ExternalPluginModelBase) {
						ExternalPluginModelBase model = (ExternalPluginModelBase) element;
						exbuf.append(
							model.getPluginBase().getId() + File.pathSeparatorChar);
					}
				}
				config.setAttribute(EXTPLUGINS, exbuf.toString());
				
				setChanged(false);
			}
		});
	}

	private void showPluginPaths() {
		IPluginModelBase[] plugins = getPlugins();
		try {
			URL[] urls = TargetPlatform.createPluginPath(plugins);
			PluginPathDialog dialog =
				new PluginPathDialog(pluginPathButton.getShell(), urls);
			dialog.create();
			dialog.getShell().setText(
				PDEPlugin.getResourceString(KEY_PLUGIN_PATH_TITLE));
			//SWTUtil.setDialogSize(dialog, 500, 400);
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
		if (!useFeaturesRadio.getSelection()) {

			IPluginModelBase[] plugins = getPlugins();
			if (plugins.length == 0) {
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_NO_PLUGINS));
			}
			IPluginModelBase boot = findModel("org.eclipse.core.boot", plugins);
			if (boot == null) {
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_NO_BOOT));
			}
			for (int i = 0; i < plugins.length; i++) {
				IPluginModelBase model = plugins[i];
				if (model.isLoaded() == false) {
					return createStatus(
						IStatus.WARNING,
						PDEPlugin.getResourceString(KEY_ERROR_BROKEN_PLUGINS));
				}
			}
		}
		else {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			String lastSegment = workspacePath.lastSegment();
			boolean badSetup = false;
			if (lastSegment.equalsIgnoreCase("plugins")==false)
				badSetup = true;
			IPath featurePath = workspacePath.removeLastSegments(1).append("features");
			if (featurePath.toFile().exists()==false) {
				badSetup = true;
			}
			if (badSetup)
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(KEY_ERROR_FEATURE_SETUP));
		}
		return createStatus(IStatus.OK, "");
	}

	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase != null) {
				String pid = pluginBase.getId();
				if (pid != null && pid.equals(id))
					return model;
			}
		}
		return null;
	}

	/**
	 * Returns the selected plugins.
	 */

	public IPluginModelBase[] getPlugins() {
		ArrayList res = new ArrayList();
		boolean useDefault = useDefaultRadio.getSelection();
		if (useDefault) {
			TreeSet wtable = new TreeSet();
			for (int i = 0; i < workspaceModels.length; i++) {
				// check for null is to accomodate previous unclean exits (e.g. workspace crashes)
				if (workspaceModels[i].getPluginBase().getId() != null) {
					res.add(workspaceModels[i]);
					wtable.add(
						workspaceModels[i].getPluginBase().getId());
				}
			}
			for (int i = 0; i < externalModels.length; i++) {
				IPluginModelBase model = externalModels[i];
				boolean masked =
					wtable.contains(model.getPluginBase().getId());
				if (!masked && externalModels[i].isEnabled())
					res.add(externalModels[i]);
			}

		} else {
			Object[] elements = pluginTreeViewer.getCheckedElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof IPluginModelBase)
					res.add(elements[i]);
			}
		}
		return (IPluginModelBase[]) res.toArray(
			new IPluginModelBase[res.size()]);
	}
	
	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	
	public Image getImage() {
			return image;
	}
}
