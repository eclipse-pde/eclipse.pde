/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
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
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
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
			if (child instanceof IPluginModelBase) {
				IResource resource = ((IPluginModelBase)child).getUnderlyingResource();
				return resource == null ? fExternalPlugins : fWorkspacePlugins;
			}
			return null;
		}

		public Object[] getElements(Object input) {
			ArrayList list = new ArrayList();
			if (fWorkspaceModels.length > 0)
				list.add(fWorkspacePlugins);
			if (fExternalModels.length > 0)
				list.add(fExternalPlugins);
			return list.toArray();
		}
	}
	
	public AbstractPluginBlock(AbstractLauncherTab tab) {
		fTab = tab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fExternalModels = getExternalModels(); //PluginRegistry.getExternalModels(); //getExternalModels();
		fWorkspaceModels = PluginRegistry.getWorkspaceModels();
	}
	
	protected IPluginModelBase[] getExternalModels() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_NONE))
			return new IPluginModelBase[0];

		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL))
			return models;
		
		ArrayList list = new ArrayList(models.length);
		for (int i = 0; i < models.length; i++) {
			if (models[i].isEnabled()) {
				list.add(models[i]);
			}
		}
		return (IPluginModelBase[])list.toArray(new IPluginModelBase[list.size()]);
	}
	
	protected void updateCounter() {
		if (fCounter != null) {
			int checked = fNumExternalChecked + fNumWorkspaceChecked;
			int total = fWorkspaceModels.length + fExternalModels.length;
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void createControl(Composite parent, int span, int indent) {
		createPluginViewer(parent, span - 1, indent);
		createButtonContainer(parent);
		
		fIncludeOptionalButton = new Button(parent, SWT.CHECK);
		fIncludeOptionalButton.setText(NLS.bind(PDEUIMessages.AdvancedLauncherTab_includeOptional, 
											fTab.getName().toLowerCase(Locale.ENGLISH))); 
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fIncludeOptionalButton.setLayoutData(gd);
		fIncludeOptionalButton.addSelectionListener(fListener);
		
		fAddWorkspaceButton = new Button(parent, SWT.CHECK);
		fAddWorkspaceButton.setText(NLS.bind(PDEUIMessages.AdvancedLauncherTab_addNew, 
											fTab.getName().toLowerCase(Locale.ENGLISH))); 
		gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fAddWorkspaceButton.setLayoutData(gd);
		fAddWorkspaceButton.addSelectionListener(fListener);		
	}
	
	protected ILabelProvider getLabelProvider() {
		return PDEPlugin.getDefault().getLabelProvider();
	}
	
	protected void createPluginViewer(Composite composite, int span, int indent) {
		fPluginTreeViewer = new CheckboxTreeViewer(composite, getTreeViewerStyle());
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
		fPluginTreeViewer.setComparator(new ListUtil.PluginComparator() {
			public int category(Object obj) {
				if (obj == fWorkspacePlugins)
					return -1;
				return 0;
			}
		});
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fPluginTreeViewer.getTree().setLayoutData(gd);

		Image siteImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_SITE_OBJ);

		fWorkspacePlugins =
			new NamedElement(
				PDEUIMessages.AdvancedLauncherTab_workspacePlugins, 
				siteImage);
		fExternalPlugins =
			new NamedElement(
				PDEUIMessages.PluginsTab_target, 
				siteImage);
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
	
		new Label(composite, SWT.NONE);
		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll); 		
		fDeselectButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll); 		
		fWorkingSetButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_workingSet); 
		fAddRequiredButton = createButton(composite, NLS.bind(PDEUIMessages.AdvancedLauncherTab_subset, fTab.getName())); 
		if (includeDefaultButton())
			fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults); 
		
		fCounter = new Label(composite, SWT.NONE);
		fCounter.setLayoutData(new GridData(GridData.FILL_BOTH|GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}
	
	protected boolean includeDefaultButton() {
		return true;
	}
	
	protected int getTreeViewerStyle() {
		return SWT.BORDER;
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
			
	protected void toggleGroups(boolean select) {
		handleGroupStateChanged(fWorkspacePlugins, select);
		handleGroupStateChanged(fExternalPlugins, select);
	}

	private void handleWorkingSets() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			String[] ids = getPluginIDs(dialog.getSelection());
			for (int i = 0; i < ids.length; i++) {
				IPluginModelBase model = PluginRegistry.findModel(ids[i]);
				if (model != null) {
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
						IPluginModelBase model = PluginRegistry.findModel((IProject)element);
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
			IFragmentModel[] fragments = findFragments(model);
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
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getModel();
			if (fPluginTreeViewer.getChecked(model))
				return model;

			IPluginModelBase[] models = entry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
			}
			
			models = entry.getExternalModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
			}
			return entry.getModel();
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();
		BundleDescription[] fragments = desc.getFragments();
		ArrayList result = new ArrayList(fragments.length);
		for (int i = 0; i < fragments.length; i++) {
			String id = fragments[i].getSymbolicName();
			if (!fragments[i].isResolved() ||
					"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
				continue;
			IPluginModelBase fragment = PluginRegistry.findModel(fragments[i]);
			if (fragment instanceof IFragmentModel ) {
				if (fPluginTreeViewer.getChecked(fragment)) {
					result.add(fragment);
				} else {
					ModelEntry entry = PluginRegistry.findEntry(id);
					if (entry != null) {
						IPluginModelBase[] candidates = entry.getExternalModels();
						for (int j = 0; j < candidates.length; j++) {
							if (j == candidates.length - 1 
								|| fPluginTreeViewer.getChecked(candidates[j])) {
								result.add(candidates[j]);
							}
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
		if (includeDefaultButton())
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
	
	protected Shell getShell() {
		// use Shell of launcher window.  If launcher window is disposed (not sure how it could happen), use workbenchwindow.  Bug 168198
		try {
			Control c = fTab.getControl();
			if (!c.isDisposed())
				return c.getShell();
		} catch (SWTException e) {
		}
		return PDEPlugin.getActiveWorkbenchShell();
	}
	
}
