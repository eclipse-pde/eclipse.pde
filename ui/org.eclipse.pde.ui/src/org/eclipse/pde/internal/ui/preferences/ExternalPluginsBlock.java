/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;


public class ExternalPluginsBlock {
	private CheckboxTableViewer pluginListViewer;
	private TargetPlatformPreferencePage page;
	private static final String KEY_RELOAD = "ExternalPluginsBlock.reload"; //$NON-NLS-1$
	private static final String KEY_WORKSPACE = "ExternalPluginsBlock.workspace"; //$NON-NLS-1$

	private boolean reloaded;
	private TablePart tablePart;
	private HashSet changed = new HashSet();
	private IPluginModelBase[] initialModels;
	private IPluginModelBase[] fModels;
	private PDEState fCurrentState;
	private Button fIncludeFragments;

	
	class ReloadOperation implements IRunnableWithProgress {
		private URL[] pluginPaths;
		
		public ReloadOperation(URL[] pluginPaths) {
			 this.pluginPaths = pluginPaths;
		}
			
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {	
			fCurrentState = new PDEState(pluginPaths, true, monitor);
			fModels = fCurrentState.getModels();		
		}
		
	}
	
	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getAllModels();
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String[] buttonLabels) {
			super(null, buttonLabels);
		}

		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0:
					handleReload();
					break;
				case 5:
					handleWorkingSets();
					break;
				case 6:
					handleAddRequired();
					break;
				case 7:
					selectNotInWorkspace();
					break;
				default:
					super.buttonSelected(button, index);
			}
		}

		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormToolkit toolkit) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, toolkit);
			viewer.setSorter(ListUtil.PLUGIN_SORTER);
			return viewer;
		}

		protected void elementChecked(Object element, boolean checked) {
			IPluginModelBase model = (IPluginModelBase) element;
			if (changed.contains(model) && model.isEnabled() == checked) {
				changed.remove(model);
			} else if (model.isEnabled() != checked) {
				changed.add(model);
			}
			super.elementChecked(element, checked);
		}
		
		protected void handleSelectAll(boolean select) {
			super.handleSelectAll(select);
			IPluginModelBase[] allModels = getAllModels();
			for (int i = 0; i < allModels.length; i++) {
				IPluginModelBase model = allModels[i];
				if (model.isEnabled() != select) {
					changed.add(model);
				} else if (changed.contains(model) && model.isEnabled() == select) {
					changed.remove(model);
				}
			}
		}
		
		public void incrementCounter(int increment) {
			updateCounter(getSelectionCount() + increment);
		}
	}

	public ExternalPluginsBlock(TargetPlatformPreferencePage page) {
		this.page = page;
		String[] buttonLabels =
			{
				PDEPlugin.getResourceString(KEY_RELOAD),
				null,
				null,
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_DESELECT_ALL),
				PDEPlugin.getResourceString("ExternalPluginsBlock.workingSet"), //$NON-NLS-1$
				PDEPlugin.getResourceString("ExternalPluginsBlock.addRequired"), //$NON-NLS-1$
				PDEPlugin.getResourceString(KEY_WORKSPACE)};
		tablePart = new TablePart(buttonLabels);
		tablePart.setSelectAllIndex(3);
		tablePart.setDeselectAllIndex(4);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	void computeDelta() {
		int type = 0;
		IModel[] addedArray = null;
		IModel[] removedArray = null;
		IModel[] changedArray = null;
		if (reloaded) {
			type =
				IModelProviderEvent.MODELS_REMOVED
					| IModelProviderEvent.MODELS_ADDED;
			removedArray = initialModels;
			addedArray = getAllModels();
		} else if (changed.size() > 0) {
			type |= IModelProviderEvent.MODELS_CHANGED;
			changedArray = (IModel[]) changed.toArray(new IModel[changed.size()]);
		}
		changed.clear();
		if (type != 0) {
			ExternalModelManager registry =
				PDECore.getDefault().getExternalModelManager();
			ModelProviderEvent event =
				new ModelProviderEvent(
					registry,
					type,
					addedArray,
					removedArray,
					changedArray);
			registry.fireModelProviderEvent(event);
		}
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		container.setLayout(layout);

		tablePart.createControl(container);

		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 100;
		
		Label label = new Label(container, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
				
		fIncludeFragments = new Button(container, SWT.CHECK);
		fIncludeFragments.setText(PDEPlugin.getResourceString("ExternalPluginsBlock.includeFragments")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeFragments.setLayoutData(gd);
		fIncludeFragments.setSelection(PDECore.getDefault().getPluginPreferences().getBoolean(ICoreConstants.INCLUDE_FRAGMENTS));
		return container;
	}


	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private IPluginModelBase[] getAllModels() {
		if (fModels == null) {
			initialModels =
				PDECore.getDefault().getExternalModelManager().getAllModels();
			return initialModels;
		}
		return fModels;
	}

	protected void handleReload() {
		String platformPath = page.getPlatformPath();
		if (platformPath != null && platformPath.length() > 0) {
			URL[] pluginPaths = PluginPathFinder.getPluginPaths(platformPath);
			ReloadOperation op = new ReloadOperation(pluginPaths);
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, false, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
			pluginListViewer.setInput(PDECore.getDefault().getExternalModelManager());
			changed.clear();
			handleSelectAll(true);
			reloaded = true;
		}
		page.resetNeedsReload();
	}

	public void initialize() {
		String platformPath = page.getPlatformPath();
		if (platformPath != null && platformPath.length() == 0)
			return;

		pluginListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		IPluginModelBase[] allModels = getAllModels();

		Vector selection = new Vector();
		for (int i = 0; i < allModels.length; i++) {
			IPluginModelBase model = allModels[i];
			if (model.isEnabled()) {
				selection.add(model);
			}
		}
		tablePart.setSelection(selection.toArray());
	}

	public void save() {
		BusyIndicator.showWhile(page.getShell().getDisplay(), new Runnable() {
			public void run() {
				savePreferences();
				if (reloaded)
					EclipseHomeInitializer.resetEclipseHomeVariable();
				updateModels();
				computeDelta();
			}
		});
	}
	
	private void savePreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		IPath newPath = new Path(page.getPlatformPath());
		IPath defaultPath = new Path(ExternalModelManager.computeDefaultPlatformPath());
		String mode =
			ExternalModelManager.arePathsEqual(newPath, defaultPath)
				? ICoreConstants.VALUE_USE_THIS
				: ICoreConstants.VALUE_USE_OTHER;
		preferences.setValue(ICoreConstants.TARGET_MODE, mode);
		preferences.setValue(ICoreConstants.PLATFORM_PATH, page.getPlatformPath());
		String[] locations = page.getPlatformLocations();
		for (int i = 0; i < locations.length && i < 5; i++) {
			preferences.setValue(ICoreConstants.SAVED_PLATFORM + i, locations[i]);
		}
		preferences.setValue(ICoreConstants.INCLUDE_FRAGMENTS, fIncludeFragments.getSelection());
		PDECore.getDefault().savePluginPreferences();
	}
	
	private void updateModels() {
		Iterator iter = changed.iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			model.setEnabled(tablePart.getTableViewer().getChecked(model));
		}

		if (reloaded) {
			PDECore.getDefault().getExternalModelManager().reset(fCurrentState, fModels);
		}
	}
	
	private void selectNotInWorkspace() {
		WorkspaceModelManager wm = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wsModels = wm.getAllModels();
		IPluginModelBase[] exModels = getAllModels();
		Vector selected = new Vector();
		for (int i = 0; i < exModels.length; i++) {
			IPluginModelBase exModel = exModels[i];
			boolean inWorkspace = false;
			for (int j = 0; j < wsModels.length; j++) {
				IPluginModelBase wsModel = wsModels[j];
				String extId = exModel.getPluginBase().getId();
				String wsId = wsModel.getPluginBase().getId();
				if (extId != null && wsId != null && extId.equals(wsId)) {
					inWorkspace = true;
					break;
				}
			}
			if (!inWorkspace) {
				selected.add(exModel);
			}
			if (exModel.isEnabled() == inWorkspace)
				changed.add(exModel);
			else if (changed.contains(exModel))
				changed.remove(exModel);
		}
		tablePart.setSelection(selected.toArray());
	}
	
	public void handleSelectAll(boolean selected) {
		tablePart.selectAll(selected);
	}
	
	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(tablePart.getControl().getShell(), true);
		if (dialog.open() == Window.OK) {
			HashSet set = getPluginIDs(dialog.getSelection());
			IPluginModelBase[] models = getAllModels();
			int counter = 0;
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					if (!pluginListViewer.getChecked(models[i])) {
						pluginListViewer.setChecked(models[i], true);
						counter += 1;
						if (!models[i].isEnabled())
							changed.add(models[i]);
					}
					set.remove(id);
				}
				if (set.isEmpty())
					break;				
			}
			tablePart.incrementCounter(counter);
		}
	}
	
	private HashSet getPluginIDs(IWorkingSet[] workingSets) {
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
						IPluginModelBase model = (IPluginModelBase)PDECore.getDefault().getWorkspaceModelManager().getWorkspacePluginModel((IProject)element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return set;
	}
	
	private void handleAddRequired() {
		TableItem[] items = tablePart.getTableViewer().getTable().getItems();
		
		if (items.length == 0)
			return;
		
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			IPluginModelBase model = (IPluginModelBase)items[i].getData();
			if (tablePart.getTableViewer().getChecked(model))
				addPluginAndDependencies((IPluginModelBase) items[i].getData(), result);
		}
		tablePart.setSelection(result.toArray());
	}
	
	protected void addPluginAndDependencies(
			IPluginModelBase model,
			ArrayList selected) {
				
			if (!selected.contains(model)) {
				selected.add(model);
				if (!model.isEnabled())
					changed.add(model);
				addDependencies(getAllModels(), model, selected);
			}
		}
		
	protected void addDependencies(
	    IPluginModelBase[] models,
		IPluginModelBase model,
		ArrayList selected) {
		
		IPluginImport[] required = model.getPluginBase().getImports();
		if (required.length > 0) {
			for (int i = 0; i < required.length; i++) {
				IPluginModelBase found = findModel(models, required[i].getId());
				if (found != null) {
					addPluginAndDependencies(found, selected);
				}
			}
		}
		
		if (model instanceof IPluginModel) {
			boolean addFragments = fIncludeFragments.getSelection();
			if (!addFragments) {
				IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
				for (int i = 0; i < libraries.length; i++) {
					if (ClasspathUtilCore.containsVariables(libraries[i].getName())) {
						addFragments = true;
						break;
					}
				}
			}
			if (addFragments) {
				IFragmentModel[] fragments = findFragments(models, ((IPluginModel)model).getPlugin());
				for (int i = 0; i < fragments.length; i++) {
					addPluginAndDependencies(fragments[i], selected);
				}
			}
		} else {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IPluginModelBase found = findModel(models, fragment.getPluginId());
			if (found != null) {
				addPluginAndDependencies(found, selected);
			}
		}
	}

	private IPluginModelBase findModel(IPluginModelBase[] models, String id) {
		for (int i = 0; i < models.length; i++) {
			String modelId = models[i].getPluginBase().getId();
			if (modelId != null && modelId.equals(id))
				return models[i];
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPluginModelBase[] models, IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (models[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) models[i]).getFragment();
				if (plugin.getId().equalsIgnoreCase(fragment.getPluginId())) {
					result.add(models[i]);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

}
