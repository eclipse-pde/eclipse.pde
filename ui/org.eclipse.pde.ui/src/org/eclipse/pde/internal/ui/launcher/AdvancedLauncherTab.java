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
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
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
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {

	private Button fUseDefaultRadio;
	private Button fUseFeaturesRadio;
	private Button fUseListRadio;
	private Button fAddRequiredButton;
	private CheckboxTreeViewer fPluginTreeViewer;
	private NamedElement fWorkspacePlugins;
	private NamedElement fExternalPlugins;
	private IPluginModelBase[] fExternalModels;
	private IPluginModelBase[] fWorkspaceModels;
	private Button fDefaultsButton;
	private int fNumExternalChecked = 0;
	private int fNumWorkspaceChecked = 0;
	private Image fImage;
	private boolean fShowFeatures = true;
	private Button fSelectAllButton;
	private Button fDeselectButton;
	private Button fWorkingSetButton;
	private Button fIncludeFragmentsButton;
	private Button fAddWorkspaceButton;
	private String fProductID;
	private String fApplicationID;
	private Button fIncludeOptionalButton;

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
		fExternalModels = PDECore.getDefault().getModelManager().getExternalModels();
		fWorkspaceModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
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
			PDEUIMessages.AdvancedLauncherTab_useDefault); //$NON-NLS-1$

		if (fShowFeatures) {
			fUseFeaturesRadio = new Button(composite, SWT.RADIO);
			fUseFeaturesRadio.setText(
				PDEUIMessages.AdvancedLauncherTab_useFeatures); //$NON-NLS-1$
		}

		fUseListRadio = new Button(composite, SWT.RADIO);
		fUseListRadio.setText(PDEUIMessages.AdvancedLauncherTab_useList); //$NON-NLS-1$

		createPluginList(composite);
		
		createSeparator(composite, 1);
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText(PDEUIMessages.AdvancedLauncherTab_validatePlugins); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleValidatePlugins();
		}});
		
		SWTUtil.setButtonDimensionHint(button);
		
		hookListeners();
		setControl(composite);

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
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

		fDefaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				computeInitialCheckState();
				updateStatus();
			}
		});
		
		fWorkingSetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleWorkingSets();
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
		
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleGroups(true);
				updateStatus();
			}}
		);
		
		fDeselectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleGroups(false);
				updateStatus();
			}}
		);
		
		fIncludeFragmentsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateStatus();
			}
		});
		
		fIncludeOptionalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateStatus();
			}
		});

		fAddWorkspaceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateStatus();
			}
		});
	}

	private void handleWorkingSets() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(getControl().getShell(), true);
		if (dialog.open() == Window.OK) {
			String[] ids = getPluginIDs(dialog.getSelection());
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < ids.length; i++) {
				ModelEntry entry = manager.findEntry(ids[i]);
				if (entry != null) {
					IPluginModelBase model = entry.getActiveModel();
					if (!fPluginTreeViewer.getChecked(model)) {
						fPluginTreeViewer.setChecked(model, true);
						if (model.getUnderlyingResource() == null)
							fNumExternalChecked += 1;
						else
							fNumWorkspaceChecked += 1;
					}
				}
			}
			adjustGroupState();
		}
	}
	
	private String[] getPluginIDs(IWorkingSet[] workingSets) {
		HashSet set = new HashSet();
		for (int i = 0; i < workingSets.length; i++) {
			IAdaptable[] elements = workingSets[i].getElements();
			for (int j = 0; j < elements.length; j++) {
				Object element = elements[j];
				if (element instanceof PersistablePluginObject) {
					set.add(((PersistablePluginObject)element).getPluginID());
				} else {
					if (element instanceof IJavaProject)
						element = ((IJavaProject)element).getProject();
					if (element instanceof IProject) {
						IPluginModelBase model = PDECore.getDefault().getModelManager().findModel((IProject)element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return (String[])set.toArray(new String[set.size()]);
	}


	protected void toggleGroups(boolean select) {
		handleGroupStateChanged(fWorkspacePlugins, select);
		handleGroupStateChanged(fExternalPlugins, select);
	}

	private void useDefaultChanged() {
		adjustCustomControlEnableState(fUseListRadio.getSelection());
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		fPluginTreeViewer.getTree().setEnabled(enable);
		fAddRequiredButton.setEnabled(enable);
		fDefaultsButton.setEnabled(enable);
		fWorkingSetButton.setEnabled(enable);
		fSelectAllButton.setEnabled(enable);
		fDeselectButton.setEnabled(enable);
		fIncludeFragmentsButton.setEnabled(enable);
		fIncludeOptionalButton.setEnabled(enable);
		fAddWorkspaceButton.setEnabled(enable);
	}

	private void createPluginList(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = 30;
		composite.setLayoutData(gd);

		createPluginViewer(composite);
		createButtonContainer(composite);
		
		fIncludeFragmentsButton = new Button(composite, SWT.CHECK);
		fIncludeFragmentsButton.setText(PDEUIMessages.AdvancedLauncherTab_includeFragments); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeFragmentsButton.setLayoutData(gd);
		
		fIncludeOptionalButton = new Button(composite, SWT.CHECK);
		fIncludeOptionalButton.setText(PDEUIMessages.AdvancedLauncherTab_includeOptional); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeOptionalButton.setLayoutData(gd);
		
		fAddWorkspaceButton = new Button(composite, SWT.CHECK);
		fAddWorkspaceButton.setText(PDEUIMessages.AdvancedLauncherTab_addNew); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fAddWorkspaceButton.setLayoutData(gd);
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
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			addPluginAndDependencies(findPlugin("org.eclipse.core.runtime"), map); //$NON-NLS-1$
		}

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
			boolean addFragments = fIncludeFragmentsButton.getSelection()
						|| ClasspathUtilCore.hasExtensibleAPI((IPlugin)model.getPluginBase());
			if (!addFragments) {
				IPluginLibrary[] libs = model.getPluginBase().getLibraries();
				for (int i = 0; i < libs.length; i++) {
					if (ClasspathUtilCore.containsVariables(libs[i].getName())) {
						addFragments = true;
						break;
					}
				}
			}
			if (addFragments) {
				IFragmentModel[] fragments = findFragments(model.getPluginBase());
				for (int i = 0; i < fragments.length; i++) {
					addPluginAndDependencies(fragments[i], map);
				}
			}
		}

		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isOptional() && !fIncludeOptionalButton.getSelection())
				continue;
			addPluginAndDependencies(findPlugin(imports[i].getId()), map);
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
				PDEUIMessages.AdvancedLauncherTab_workspacePlugins, //$NON-NLS-1$
				pluginsImage);
		fExternalPlugins =
			new NamedElement(
				PDEUIMessages.AdvancedLauncherTab_externalPlugins, //$NON-NLS-1$
				pluginsImage);
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		
		fSelectAllButton = new Button(composite, SWT.PUSH);
		fSelectAllButton.setText(PDEUIMessages.AdvancedLauncherTab_selectAll); //$NON-NLS-1$
		fSelectAllButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fSelectAllButton);
		
		fDeselectButton = new Button(composite, SWT.PUSH);
		fDeselectButton.setText(PDEUIMessages.AdvancedLauncherTab_deselectAll); //$NON-NLS-1$
		fDeselectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fDeselectButton);
		
		fWorkingSetButton = new Button(composite, SWT.PUSH);
		fWorkingSetButton.setText(PDEUIMessages.AdvancedLauncherTab_workingSet); //$NON-NLS-1$
		fWorkingSetButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fWorkingSetButton);

		fAddRequiredButton = new Button(composite, SWT.PUSH);
		fAddRequiredButton.setText(PDEUIMessages.AdvancedLauncherTab_subset); //$NON-NLS-1$
		fAddRequiredButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fAddRequiredButton);

		fDefaultsButton = new Button(composite, SWT.PUSH);
		fDefaultsButton.setText(
			PDEUIMessages.AdvancedLauncherTab_defaults); //$NON-NLS-1$
		fDefaultsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fDefaultsButton);
	}
	
	/*
	 * if the "automatic add" option is selected, then we save the ids of plugins
	 * that have been "deselected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'checked',
	 * then we check the plugins that had been deselected and saved in the config.
	 *
	 * If the "automatic add" option is not selected, then we save the ids of plugins
	 * that were "selected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'unchecked',
	 * then we check the plugins that had been selected and saved in the config.
	 */
	private void initWorkspacePluginsState(ILaunchConfiguration config) throws CoreException {
		boolean automaticAdd = config.getAttribute(AUTOMATIC_ADD, true);		
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, automaticAdd);
		fNumWorkspaceChecked = automaticAdd ? fWorkspaceModels.length : 0;
		
		TreeSet ids = LauncherUtils.parseWorkspacePluginIds(config);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			String id = fWorkspaceModels[i].getPluginBase().getId();
			if (id == null)
				continue;
			if (automaticAdd && ids.contains(id)) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], false))
					fNumWorkspaceChecked -= 1;
			} else if (!automaticAdd && ids.contains(id)) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], true))
					fNumWorkspaceChecked += 1;
			} 
		}			

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	private void initExternalPluginsState(ILaunchConfiguration config)
		throws CoreException {
		fNumExternalChecked = 0;

		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		TreeSet selected = LauncherUtils.parseExternalPluginIds(config);
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
		initializeProductFrom(config);
		try {
			fUseDefaultRadio.setSelection(config.getAttribute(USE_DEFAULT, true));
			if (fShowFeatures) {
				fUseFeaturesRadio.setSelection(config.getAttribute(USEFEATURES, false));
				fUseListRadio.setSelection(
					!fUseDefaultRadio.getSelection()
						&& !fUseFeaturesRadio.getSelection());
			} else {
				fUseListRadio.setSelection(!fUseDefaultRadio.getSelection());
			}
			fIncludeFragmentsButton.setSelection(config.getAttribute(INCLUDE_FRAGMENTS, false));
			fIncludeOptionalButton.setSelection(config.getAttribute(INCLUDE_OPTIONAL, true));
			fAddWorkspaceButton.setSelection(config.getAttribute(AUTOMATIC_ADD, true));

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
	
	private void handleValidatePlugins() {
		PluginValidationOperation op = new PluginValidationOperation(
				getPluginsToValidate(), fProductID, fApplicationID);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (op.hasErrors())
				new PluginStatusDialog(getControl().getShell(), op).open();
			else
				MessageDialog.openInformation(getControl().getShell(), PDEUIMessages.AdvancedLauncherTab_pluginValidation, PDEUIMessages.AdvancedLauncherTab_noProblems); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private IPluginModelBase[] getPluginsToValidate() {
		if (!fUseListRadio.getSelection())
			return PDECore.getDefault().getModelManager().getPlugins();
		
		Map map = new HashMap();
		Object[] objects = fPluginTreeViewer.getCheckedElements();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)objects[i];
				String id = model.getPluginBase().getId();
				if (id == null)
					continue;
				if (!map.containsKey(id) || model.getUnderlyingResource() != null)
					map.put(id, model);
			}
		}
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (fShowFeatures) {
			config.setAttribute(USE_DEFAULT, true);
			config.setAttribute(USEFEATURES, false);
		} else {
			config.setAttribute(USE_DEFAULT, true);
		}
		config.setAttribute(INCLUDE_FRAGMENTS, false);
		config.setAttribute(INCLUDE_OPTIONAL, true);
		config.setAttribute(AUTOMATIC_ADD, true);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USE_DEFAULT, fUseDefaultRadio.getSelection());
		if (fShowFeatures)
			config.setAttribute(USEFEATURES, fUseFeaturesRadio.getSelection());
		config.setAttribute(INCLUDE_FRAGMENTS, fIncludeFragmentsButton.getSelection());
		config.setAttribute(INCLUDE_OPTIONAL, fIncludeOptionalButton.getSelection());
		config.setAttribute(AUTOMATIC_ADD, fAddWorkspaceButton.getSelection());
		if (fUseListRadio.getSelection()) {
			// store deselected projects
			StringBuffer wbuf = new StringBuffer();
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				IPluginModelBase model = fWorkspaceModels[i];
				// if "automatic add" option is selected, save "deselected" workspace plugins
				// Otherwise, save "selected" workspace plugins
				if (fPluginTreeViewer.getChecked(model) != fAddWorkspaceButton.getSelection())
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
			IPath featurePath = workspacePath.removeLastSegments(1).append("features"); //$NON-NLS-1$
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins") //$NON-NLS-1$
				|| !featurePath.toFile().exists())
				return createStatus(
					IStatus.ERROR,
					PDEUIMessages.AdvancedLauncherTab_error_featureSetup); //$NON-NLS-1$
		} 
		return createStatus(IStatus.OK, ""); //$NON-NLS-1$
	}

	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name; //$NON-NLS-1$
	}

	public Image getImage() {
		return fImage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy config) {
		initializeProductFrom(config);
	}

	/**
	 * @param config
	 */
	private void initializeProductFrom(ILaunchConfiguration config) {
		try {
			if (config.getAttribute(USE_PRODUCT, false)) {
				fProductID = config.getAttribute(PRODUCT, (String)null);
				fApplicationID = null;
			} else {
				String appToRun = config.getAttribute(APPLICATION, LauncherUtils.getDefaultApplicationName());
				if (fUseFeaturesRadio != null)
					fApplicationID = appToRun;
				else {
					if(JUnitLaunchConfiguration.CORE_APPLICATION.equals(appToRun)){
						fApplicationID = null;
					} else {
						fApplicationID = config.getAttribute(APP_TO_TEST, LauncherUtils.getDefaultApplicationName());
					}
				}
				fProductID = null;
			}
		} catch (CoreException e) {
		}
	}
	
	
}
