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

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {

	private Button fUseDefaultRadio;
	private Button fUseFeaturesRadio;
	private Button fUseListRadio;
	private Button fAddRequiredButton;
	private Button fOnePluginRadio;
	private Label fPluginLabel;
	private Text fPluginText;
	private Button fBrowseButton;
	private CheckboxTreeViewer fPluginTreeViewer;
	private Label fVisibleLabel;
	private NamedElement fWorkspacePlugins;
	private NamedElement fExternalPlugins;
	private IPluginModelBase[] fExternalModels;
	private IPluginModelBase[] fWorkspaceModels;
	private Button fDefaultsButton;
	private int fNumExternalChecked = 0;
	private int fNumWorkspaceChecked = 0;
	private Image fImage;
	private boolean fShowFeatures = true;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModelBase);
		}
		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins)
				return fExternalModels;
			if (parent == fWorkspacePlugins)
				return fWorkspaceModels;
			return new Object[0];
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { fWorkspacePlugins, fExternalPlugins };
		}
	}

	public AdvancedLauncherTab() {
		this(true);
	}

	public AdvancedLauncherTab(boolean showFeatures) {
		this.fShowFeatures = showFeatures;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fExternalModels = PDECore.getDefault().getExternalModelManager().getAllModels();
		fWorkspaceModels = PDECore.getDefault().getWorkspaceModelManager().getAllModels();
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		fImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		fUseDefaultRadio = new Button(composite, SWT.RADIO);
		fUseDefaultRadio.setText(
			PDEPlugin.getResourceString("AdvancedLauncherTab.useDefault"));

		if (fShowFeatures) {
			fUseFeaturesRadio = new Button(composite, SWT.RADIO);
			fUseFeaturesRadio.setText(
				PDEPlugin.getResourceString("AdvancedLauncherTab.useFeatures"));
		}

		fOnePluginRadio = new Button(composite, SWT.RADIO);
		fOnePluginRadio.setText(
			PDEPlugin.getResourceString("AdvancedLauncherTab.onePlugin"));

		createOnePluginSection(composite);

		fUseListRadio = new Button(composite, SWT.RADIO);
		fUseListRadio.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.useList"));

		createPluginList(composite);

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
		fUseDefaultRadio.addSelectionListener(adapter);

		if (fShowFeatures)
			fUseFeaturesRadio.addSelectionListener(adapter);

		fOnePluginRadio.addSelectionListener(adapter);

		fDefaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				computeInitialCheckState();
				updateStatus();
			}
		});

		fAddRequiredButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
					public void run() {
						computeSubset();
						updateStatus();
					}
				});
			}
		});

		fPluginText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});

		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browsePluginId();
			}
		});
	}
	private void browsePluginId() {
		final Display display = getControl().getDisplay();
		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {
				PluginSelectionDialog dialog =
					new PluginSelectionDialog(display.getActiveShell(), false, false);
				dialog.create();
				if (dialog.open() == PluginSelectionDialog.OK) {
					IPluginModel model = (IPluginModel) dialog.getFirstResult();
					IPlugin plugin = model.getPlugin();
					fPluginText.setText(plugin.getId());
				}
			}
		});
	}

	private void useDefaultChanged() {
		adjustCustomControlEnableState(fUseListRadio.getSelection());
		adjustPluginArea(fOnePluginRadio.getSelection());
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		fVisibleLabel.setVisible(enable);
		fPluginTreeViewer.getTree().setVisible(enable);
		fAddRequiredButton.setVisible(enable);
		fDefaultsButton.setVisible(enable);
	}

	private void adjustPluginArea(boolean enable) {
		fPluginLabel.setEnabled(enable);
		fPluginText.setEnabled(enable);
		fBrowseButton.setEnabled(enable);
	}

	private void createPluginList(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fVisibleLabel = new Label(composite, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fVisibleLabel.setLayoutData(gd);
		fVisibleLabel.setText(
			PDEPlugin.getResourceString("AdvancedLauncherTab.visibleList"));

		createPluginViewer(composite);
		createButtonContainer(composite);
	}

	private void createOnePluginSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 20;
		layout.marginHeight = 0;
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPluginLabel = new Label(composite, SWT.NONE);
		fPluginLabel.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.pluginId"));

		fPluginText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseButton = new Button(composite, SWT.PUSH);
		fBrowseButton.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.browse"));
		fBrowseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseButton);
	}

	private void computeSubset() {
		Object[] checked = fPluginTreeViewer.getCheckedElements();
		TreeMap map = new TreeMap();
		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) checked[i];
				addPluginAndDependencies(model, map);
			}
		}
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			addPluginAndDependencies(findPlugin("org.eclipse.osgi"), map);
			addPluginAndDependencies(findPlugin("org.eclipse.osgi.services"), map);
			addPluginAndDependencies(findPlugin("org.eclipse.osgi.util"), map);
			addPluginAndDependencies(findPlugin("org.eclipse.update.configurator"), map);
		} else {
			addPluginAndDependencies(findPlugin("org.eclipse.core.boot"), map);
		}
		addPluginAndDependencies(findPlugin("org.eclipse.core.runtime"), map);

		checked = map.values().toArray();

		fPluginTreeViewer.setCheckedElements(map.values().toArray());
		fNumExternalChecked = 0;
		fNumWorkspaceChecked = 0;
		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof WorkspacePluginModelBase)
				fNumWorkspaceChecked += 1;
			else
				fNumExternalChecked += 1;
		}
		adjustGroupState();
	}

	private void addPluginAndDependencies(IPluginModelBase model, TreeMap map) {
		if (model == null)
			return;

		String id = model.getPluginBase().getId();
		if (map.containsKey(id))
			return;

		map.put(id, model);

		if (model instanceof IFragmentModel) {
			IPluginModelBase parent =
				findPlugin(((IFragmentModel) model).getFragment().getPluginId());
			addPluginAndDependencies(parent, map);
		} else {
			IFragmentModel[] fragments = findFragments(model.getPluginBase());
			for (int i = 0; i < fragments.length; i++) {
				addPluginAndDependencies(fragments[i], map);
			}
		}

		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			addPluginAndDependencies(findPlugin(imports[i].getId()), map);
		}
		
		if (!map.containsKey("org.apache.ant")) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if (extensions[i].getPoint().startsWith("org.eclipse.ant.core")) {
					addPluginAndDependencies(findPlugin("org.apache.ant"), map);
					break;
				}
			}
		}
	}

	private IPluginModelBase findPlugin(String id) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getActiveModel();
			if (fPluginTreeViewer.getChecked(model))
				return model;

			model = entry.getExternalModel();
			if (model != null && fPluginTreeViewer.getChecked(model)) {
				return model;
			}
			return entry.getActiveModel();
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPluginBase plugin) {
		ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
		ArrayList result = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			IPluginModelBase model = entry.getActiveModel();
			if (model instanceof IFragmentModel) {
				String id = ((IFragmentModel) model).getFragment().getPluginId();
				if (id.equals(plugin.getId())) {
					if (fPluginTreeViewer.getChecked(model)) {
						result.add(model);
					} else {
						model = entry.getExternalModel();
						if (model != null && fPluginTreeViewer.getChecked(model)) {
							result.add(model);
						} else {
							result.add(entry.getActiveModel());
						}
					}
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

	private void adjustGroupState() {
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(
			fExternalPlugins,
			fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	private void createPluginViewer(Composite composite) {
		fPluginTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginTreeViewer.setAutoExpandLevel(2);
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					handleCheckStateChanged(
						(IPluginModelBase) element,
						event.getChecked());
				} else {
					handleGroupStateChanged(element, event.getChecked());
				}
				updateLaunchConfigurationDialog();
			}
		});
		fPluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
			public int category(Object obj) {
				if (obj == fWorkspacePlugins)
					return -1;
				return 0;
			}
		});

		fPluginTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));

		Image pluginsImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);

		fWorkspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString("AdvancedLauncherTab.workspacePlugins"),
				pluginsImage);
		fExternalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString("AdvancedLauncherTab.externalPlugins"),
				pluginsImage);
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddRequiredButton = new Button(composite, SWT.PUSH);
		fAddRequiredButton.setText(
			PDEPlugin.getResourceString("AdvancedLauncherTab.subset"));
		fAddRequiredButton.setLayoutData(
			new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fAddRequiredButton);

		fDefaultsButton = new Button(composite, SWT.PUSH);
		fDefaultsButton.setText(
			PDEPlugin.getResourceString("AdvancedLauncherTab.defaults"));
		fDefaultsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fDefaultsButton);
	}
	private void initWorkspacePluginsState(ILaunchConfiguration config)
		throws CoreException {
		fNumWorkspaceChecked = fWorkspaceModels.length;
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);

		TreeSet deselected = LauncherUtils.parseDeselectedWSIds(config);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			if (deselected.contains(fWorkspaceModels[i].getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], false))
					fNumWorkspaceChecked -= 1;
			}
		}

		if (fNumWorkspaceChecked == 0)
			fPluginTreeViewer.setChecked(fWorkspacePlugins, false);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	private void initExternalPluginsState(ILaunchConfiguration config)
		throws CoreException {
		fNumExternalChecked = 0;

		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		TreeSet selected = LauncherUtils.parseSelectedExtIds(config);
		for (int i = 0; i < fExternalModels.length; i++) {
			if (selected.contains(fExternalModels[i].getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(fExternalModels[i], true))
					fNumExternalChecked += 1;
			}
		}

		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(
			fExternalPlugins,
			fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fUseDefaultRadio.setSelection(config.getAttribute(USECUSTOM, true));
			fOnePluginRadio.setSelection(config.getAttribute(USE_ONE_PLUGIN, false));
			if (fShowFeatures) {
				fUseFeaturesRadio.setSelection(config.getAttribute(USEFEATURES, false));
				fUseListRadio.setSelection(
					!fUseDefaultRadio.getSelection()
						&& !fUseFeaturesRadio.getSelection()
						&& !fOnePluginRadio.getSelection());
			} else {
				fUseListRadio.setSelection(
					!fUseDefaultRadio.getSelection() && !fOnePluginRadio.getSelection());
			}

			fPluginText.setText(config.getAttribute(ONE_PLUGIN_ID, ""));

			if (fPluginTreeViewer.getInput() == null) {
				fPluginTreeViewer.setUseHashlookup(true);
				fPluginTreeViewer.setInput(PDEPlugin.getDefault());
				fPluginTreeViewer.reveal(fWorkspacePlugins);
			}

			if (fUseDefaultRadio.getSelection()) {
				computeInitialCheckState();
			} else if (fUseListRadio.getSelection()) {
				initWorkspacePluginsState(config);
				initExternalPluginsState(config);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		adjustCustomControlEnableState(fUseListRadio.getSelection());
		adjustPluginArea(fOnePluginRadio.getSelection());
		updateStatus();
	}

	private void computeInitialCheckState() {
		TreeSet wtable = new TreeSet();
		fNumWorkspaceChecked = 0;
		fNumExternalChecked = 0;

		for (int i = 0; i < fWorkspaceModels.length; i++) {
			IPluginModelBase model = fWorkspaceModels[i];
			fNumWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.add(model.getPluginBase().getId());
		}
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);

		fNumExternalChecked = 0;
		for (int i = 0; i < fExternalModels.length; i++) {
			IPluginModelBase model = fExternalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				fPluginTreeViewer.setChecked(model, true);
				fNumExternalChecked += 1;
			}
		}
		adjustGroupState();
	}

	private void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		if (model.getUnderlyingResource() == null) {
			if (checked) {
				fNumExternalChecked += 1;
			} else {
				fNumExternalChecked -= 1;
			}
		} else {
			if (checked) {
				fNumWorkspaceChecked += 1;
			} else {
				fNumWorkspaceChecked -= 1;
			}
		}
		adjustGroupState();
	}

	private void handleGroupStateChanged(Object group, boolean checked) {
		fPluginTreeViewer.setSubtreeChecked(group, checked);
		fPluginTreeViewer.setGrayed(group, false);

		if (group == fWorkspacePlugins)
			fNumWorkspaceChecked = checked ? fWorkspaceModels.length : 0;
		else if (group == fExternalPlugins)
			fNumExternalChecked = checked ? fExternalModels.length : 0;

	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (fShowFeatures) {
			config.setAttribute(USECUSTOM, true);
			config.setAttribute(USEFEATURES, false);
			config.setAttribute(USE_ONE_PLUGIN, false);
			config.setAttribute(ONE_PLUGIN_ID, "");
		} else {
			config.setAttribute(USECUSTOM, true);
			config.setAttribute(USE_ONE_PLUGIN, false);
			try {
				String projectName= config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
				config.setAttribute(ONE_PLUGIN_ID, getPluginId(projectName));
			} catch (CoreException e) {
			}
		}
	}
	
	private String getPluginId(String projectName) {
		IResource project = PDEPlugin.getWorkspace().getRoot().findMember(projectName);
		if (project != null && project instanceof IProject) {
			IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel((IProject)project);
			if (model != null && model instanceof IPluginModelBase) {
				return ((IPluginModelBase)model).getPluginBase().getId();
			}
		}
		return "";
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USECUSTOM, fUseDefaultRadio.getSelection());
		if (fShowFeatures)
			config.setAttribute(USEFEATURES, fUseFeaturesRadio.getSelection());
		config.setAttribute(USE_ONE_PLUGIN, fOnePluginRadio.getSelection());
		config.setAttribute(ONE_PLUGIN_ID, fPluginText.getText());
		if (fUseListRadio.getSelection()) {
			// store deselected projects
			StringBuffer wbuf = new StringBuffer();
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				IPluginModelBase model = (IPluginModelBase) fWorkspaceModels[i];
				if (!fPluginTreeViewer.getChecked(model))
					wbuf.append(model.getPluginBase().getId() + File.pathSeparatorChar);
			}
			config.setAttribute(WSPROJECT, wbuf.toString());

			// Store selected external models
			StringBuffer exbuf = new StringBuffer();
			Object[] checked = fPluginTreeViewer.getCheckedElements();
			for (int i = 0; i < checked.length; i++) {
				if (checked[i] instanceof ExternalPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) checked[i];
					exbuf.append(model.getPluginBase().getId() + File.pathSeparatorChar);
				}
			}
			config.setAttribute(EXTPLUGINS, exbuf.toString());
		} else {
			config.setAttribute(WSPROJECT, (String) null);
			config.setAttribute(EXTPLUGINS, (String) null);
		}
	}

	private void updateStatus() {
		updateStatus(validate());
	}

	private IStatus validate() {
		if (fShowFeatures && fUseFeaturesRadio.getSelection()) {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			IPath featurePath = workspacePath.removeLastSegments(1).append("features");
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins")
				|| !featurePath.toFile().exists())
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(
						"AdvancedLauncherTab.error.featureSetup"));
		} else if (fOnePluginRadio.getSelection()) {
			if (fPluginText.getText().length() == 0)
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString("AdvancedLauncherTab.error.noPlugin"));
			if (PDECore.getDefault().getModelManager().findEntry(fPluginText.getText())
				== null)
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString(
						"AdvancedLauncherTab.error.pluginNotExists"));
		}
		return createStatus(IStatus.OK, "");
	}

	public String getName() {
		return PDEPlugin.getResourceString("AdvancedLauncherTab.name");
	}

	public Image getImage() {
		return fImage;
	}
}
