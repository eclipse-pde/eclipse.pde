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
package org.eclipse.pde.internal.ui.launcher;

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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public abstract class AbstractPluginBlock {

	protected AbstractLauncherTab fTab;
	
	protected CheckboxTreeViewer fPluginTreeViewer;
	protected NamedElement fWorkspacePlugins;
	protected NamedElement fExternalPlugins;
	protected IPluginModelBase[] fExternalModels;
	protected IPluginModelBase[] fWorkspaceModels;
	protected int fNumExternalChecked;
	protected int fNumWorkspaceChecked;

	private Button fIncludeOptionalButton;
	protected Button fAddWorkspaceButton;
	
	private Button fSelectAllButton;
	private Button fDeselectButton;
	private Button fWorkingSetButton;
	private Button fAddRequiredButton;
	private Button fDefaultsButton;
	
	private Listener fListener = new Listener();

	private Label fCounter;
	
	class Listener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fSelectAllButton) {
				toggleGroups(true);
			} else if (source == fDeselectButton) {
				toggleGroups(false);
			} else if (source == fWorkingSetButton) {
				handleWorkingSets();
			} else if (source == fAddRequiredButton) {
				computeSubset();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			}
			fTab.updateLaunchConfigurationDialog();
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements
			ITreeContentProvider {
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
	
	public AbstractPluginBlock(AbstractLauncherTab tab) {
		fTab = tab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fExternalModels = PDECore.getDefault().getModelManager().getExternalModels();
		fWorkspaceModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
	}
	
	protected void updateCounter() {
		if (fCounter != null) {
			int checked = fNumExternalChecked + fNumWorkspaceChecked;
			int total = fWorkspaceModels.length + fExternalModels.length;
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createPluginViewer(composite);
		createButtonContainer(composite);
		
		fIncludeOptionalButton = new Button(composite, SWT.CHECK);
		fIncludeOptionalButton.setText(PDEUIMessages.AdvancedLauncherTab_includeOptional); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeOptionalButton.setLayoutData(gd);
		fIncludeOptionalButton.addSelectionListener(fListener);
		
		fAddWorkspaceButton = new Button(composite, SWT.CHECK);
		fAddWorkspaceButton.setText(PDEUIMessages.AdvancedLauncherTab_addNew); 
		fAddWorkspaceButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddWorkspaceButton.addSelectionListener(fListener);
		
		Button button = new Button(composite, SWT.PUSH);
		button.setText(PDEUIMessages.AdvancedLauncherTab_validatePlugins); 
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleValidatePlugins();
			}
		});		
		SWTUtil.setButtonDimensionHint(button);
	}
	
	protected ILabelProvider getLabelProvider() {
		return PDEPlugin.getDefault().getLabelProvider();
	}
	
	protected void createPluginViewer(Composite composite) {
		fPluginTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER|SWT.FULL_SELECTION);
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(getLabelProvider());
		fPluginTreeViewer.setAutoExpandLevel(2);
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					handleCheckStateChanged(event);
				} else {
					handleGroupStateChanged(element, event.getChecked());
				}
				fTab.updateLaunchConfigurationDialog();
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
				PDEUIMessages.AdvancedLauncherTab_workspacePlugins, 
				pluginsImage);
		fExternalPlugins =
			new NamedElement(
				PDEUIMessages.PluginsTab_target, 
				pluginsImage);
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
	
		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll); 		
		fDeselectButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll); 		
		fWorkingSetButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_workingSet); 
		fAddRequiredButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_subset); 
		fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults); 
		
		fCounter = new Label(composite, SWT.NONE);
		fCounter.setLayoutData(new GridData(GridData.FILL_BOTH|GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}
	
	private Button createButton(Composite composite, String text) {
		Button button = new Button(composite, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(fListener);
		return button;
	}
	
	protected void handleCheckStateChanged(CheckStateChangedEvent event) {
		IPluginModelBase model = (IPluginModelBase)event.getElement();
		if (model.getUnderlyingResource() == null) {
			if (event.getChecked()) {
				fNumExternalChecked += 1;
			} else {
				fNumExternalChecked -= 1;
			}
		} else {
			if (event.getChecked()) {
				fNumWorkspaceChecked += 1;
			} else {
				fNumWorkspaceChecked -= 1;
			}
		}
		adjustGroupState();
	}

	protected void handleGroupStateChanged(Object group, boolean checked) {
		fPluginTreeViewer.setSubtreeChecked(group, checked);
		fPluginTreeViewer.setGrayed(group, false);

		if (group == fWorkspacePlugins)
			fNumWorkspaceChecked = checked ? fWorkspaceModels.length : 0;
		else if (group == fExternalPlugins)
			fNumExternalChecked = checked ? fExternalModels.length : 0;

	}
	
	private void handleValidatePlugins() {
		PluginValidationOperation op = createValidationOperation();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (op.hasErrors())
				new PluginStatusDialog(fTab.getControl().getShell(), op).open();
			else
				MessageDialog.openInformation(fTab.getControl().getShell(), PDEUIMessages.AdvancedLauncherTab_pluginValidation, PDEUIMessages.AdvancedLauncherTab_noProblems); // 
		}
	}
	
	protected PluginValidationOperation createValidationOperation() {
		return new PluginValidationOperation(getPluginsToValidate());
	}
	
	protected IPluginModelBase[] getPluginsToValidate() {
		if (!fPluginTreeViewer.getTree().isEnabled())
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
	
	protected void toggleGroups(boolean select) {
		handleGroupStateChanged(fWorkspacePlugins, select);
		handleGroupStateChanged(fExternalPlugins, select);
	}

	private void handleWorkingSets() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(fTab.getControl().getShell(), true);
		if (dialog.open() == Window.OK) {
			String[] ids = getPluginIDs(dialog.getSelection());
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < ids.length; i++) {
				ModelEntry entry = manager.findEntry(ids[i]);
				if (entry != null) {
					IPluginModelBase model = entry.getActiveModel();
					if (!fPluginTreeViewer.getChecked(model)) {
						setChecked(model, true);
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
	
	protected void setChecked(IPluginModelBase model, boolean checked) {
		fPluginTreeViewer.setChecked(model, checked);
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
	
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		fIncludeOptionalButton.setSelection(config.getAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true));
		fAddWorkspaceButton.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true));

		if (fPluginTreeViewer.getInput() == null) {
			fPluginTreeViewer.setUseHashlookup(true);
			fPluginTreeViewer.setInput(PDEPlugin.getDefault());
			fPluginTreeViewer.reveal(fWorkspacePlugins);
		}
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

		setCheckedElements(checked);
		fNumExternalChecked = 0;
		fNumWorkspaceChecked = 0;
		for (int i = 0; i < checked.length; i++) {
			if (((IPluginModelBase)checked[i]).getUnderlyingResource() != null)
				fNumWorkspaceChecked += 1;
			else
				fNumExternalChecked += 1;
		}
		adjustGroupState();
	}
	
	protected void setCheckedElements(Object[] checked) {
		fPluginTreeViewer.setCheckedElements(checked);
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
			if ("org.eclipse.ui.workbench.compatibility".equals(model.getPluginBase().getId())) //$NON-NLS-1$
				continue;
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

	protected void adjustGroupState() {
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(
			fExternalPlugins,
			fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, fIncludeOptionalButton.getSelection());
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, fAddWorkspaceButton.getSelection());
		savePluginState(config);
		updateCounter();
	}
	
	protected abstract void savePluginState(ILaunchConfigurationWorkingCopy config);
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
	}
	
	public void enableViewer(boolean enable) {
		fPluginTreeViewer.getTree().setEnabled(enable);
		fAddRequiredButton.setEnabled(enable);
		fDefaultsButton.setEnabled(enable);
		fWorkingSetButton.setEnabled(enable);
		fSelectAllButton.setEnabled(enable);
		fDeselectButton.setEnabled(enable);
		fIncludeOptionalButton.setEnabled(enable);
		fAddWorkspaceButton.setEnabled(enable);
		fCounter.setEnabled(enable);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	protected boolean isEnabled() {
		return fPluginTreeViewer.getTree().isEnabled();
	}
	
	protected void handleRestoreDefaults() {
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
	
}
