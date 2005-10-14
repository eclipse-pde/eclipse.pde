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
import org.eclipse.osgi.service.resolver.BundleDescription;
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
	private CheckboxTableViewer fPluginListViewer;
	private TargetPlatformPreferencePage fPage;
	private boolean fReloaded;
	private TablePart fTablePart;
	private HashSet fChangedModels = new HashSet();
	private IPluginModelBase[] fInitialModels;
	private IPluginModelBase[] fModels;
	private PDEState fCurrentState;
	private Button fIncludeFragments;

	
	class ReloadOperation implements IRunnableWithProgress {
		private String location;
		
		public ReloadOperation(String platformPath) {
			this.location = platformPath;
		}
			
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {	
			URL[] pluginPaths = PluginPathFinder.getPluginPaths(location);
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
			if (fChangedModels.contains(model) && model.isEnabled() == checked) {
				fChangedModels.remove(model);
			} else if (model.isEnabled() != checked) {
				fChangedModels.add(model);
			}
			super.elementChecked(element, checked);
		}
		
		protected void handleSelectAll(boolean select) {
			super.handleSelectAll(select);
			IPluginModelBase[] allModels = getAllModels();
			for (int i = 0; i < allModels.length; i++) {
				IPluginModelBase model = allModels[i];
				if (model.isEnabled() != select) {
					fChangedModels.add(model);
				} else if (fChangedModels.contains(model) && model.isEnabled() == select) {
					fChangedModels.remove(model);
				}
			}
		}
		
		public void incrementCounter(int increment) {
			updateCounter(getSelectionCount() + increment);
		}
	}

	public ExternalPluginsBlock(TargetPlatformPreferencePage page) {
		this.fPage = page;
		String[] buttonLabels =
			{
				PDEUIMessages.ExternalPluginsBlock_reload,
				null,
				null,
				PDEUIMessages.WizardCheckboxTablePart_selectAll,
				PDEUIMessages.WizardCheckboxTablePart_deselectAll,
				PDEUIMessages.ExternalPluginsBlock_workingSet, 
				PDEUIMessages.ExternalPluginsBlock_addRequired, 
				PDEUIMessages.ExternalPluginsBlock_workspace};
		fTablePart = new TablePart(buttonLabels);
		fTablePart.setSelectAllIndex(3);
		fTablePart.setDeselectAllIndex(4);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	void computeDelta() {
		int type = 0;
		IModel[] addedArray = null;
		IModel[] removedArray = null;
		IModel[] changedArray = null;
		if (fReloaded) {
			type =
				IModelProviderEvent.MODELS_REMOVED
					| IModelProviderEvent.MODELS_ADDED | IModelProviderEvent.TARGET_CHANGED;
			removedArray = fInitialModels;
			addedArray = getAllModels();
		} else if (fChangedModels.size() > 0) {
			type |= IModelProviderEvent.MODELS_CHANGED;
			changedArray = (IModel[]) fChangedModels.toArray(new IModel[fChangedModels.size()]);
		}
		fChangedModels.clear();
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
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		fTablePart.createControl(container);

		fPluginListViewer = fTablePart.getTableViewer();
		fPluginListViewer.setContentProvider(new PluginContentProvider());
		fPluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData) fTablePart.getControl().getLayoutData();
		gd.heightHint = 100;
				
		fIncludeFragments = new Button(container, SWT.CHECK);
		fIncludeFragments.setText(PDEUIMessages.ExternalPluginsBlock_includeFragments); 
		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.verticalIndent = 5;
		gd.horizontalIndent = 5;
		fIncludeFragments.setLayoutData(gd);
		fIncludeFragments.setSelection(PDECore.getDefault().getPluginPreferences().getBoolean(ICoreConstants.INCLUDE_FRAGMENTS));
		return container;
	}


	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IPluginModelBase[] getAllModels() {
		if (fModels == null) {
			fInitialModels =
				PDECore.getDefault().getModelManager().getExternalModels();
			return fInitialModels;
		}
		return fModels;
	}

	protected void handleReload() {
		String platformPath = fPage.getPlatformPath();
		if (platformPath != null && platformPath.length() > 0) {
			ReloadOperation op = new ReloadOperation(platformPath);
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, false, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
			fPluginListViewer.setInput(PDECore.getDefault().getExternalModelManager());
			fChangedModels.clear();
			handleSelectAll(true);
			fReloaded = true;
			fPage.getSourceBlock().resetExtensionLocations(getAllModels());
		}
		fPage.resetNeedsReload();
	}

	public void initialize() {
		String platformPath = fPage.getPlatformPath();
		if (platformPath != null && platformPath.length() == 0)
			return;

		fPluginListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		IPluginModelBase[] allModels = getAllModels();

		Vector selection = new Vector();
		for (int i = 0; i < allModels.length; i++) {
			IPluginModelBase model = allModels[i];
			if (model.isEnabled()) {
				selection.add(model);
			}
		}
		fTablePart.setSelection(selection.toArray());
	}

	public void performOk() {
		BusyIndicator.showWhile(fPage.getShell().getDisplay(), new Runnable() {
			public void run() {
				savePreferences();
				if (fReloaded)
					EclipseHomeInitializer.resetEclipseHomeVariable();
				if (fReloaded) {
					IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
					for (int i = 0; i < models.length; i++) {
						BundleDescription bundle = models[i].getBundleDescription();
						if (bundle == null)
							continue;
						BundleDescription[] conflicts = fCurrentState.getState().getBundles(bundle.getSymbolicName());
						for (int j = 0; j < conflicts.length; j++)
							fCurrentState.getState().removeBundle(conflicts[j]);
						fCurrentState.addBundle(models[i], false);
					}
					if (models.length > 0)
						fCurrentState.resolveState(true);
					PDECore.getDefault().getExternalModelManager().setModels(fCurrentState.getTargetModels());
					PDECore.getDefault().getModelManager().setState(fCurrentState);
					PDECore.getDefault().getFeatureModelManager().targetReloaded();				
				}
				updateModels();
				computeDelta();
			}
		});
	}
	
	private void savePreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		IPath newPath = new Path(fPage.getPlatformPath());
		IPath defaultPath = new Path(ExternalModelManager.computeDefaultPlatformPath());
		String mode =
			ExternalModelManager.arePathsEqual(newPath, defaultPath)
				? ICoreConstants.VALUE_USE_THIS
				: ICoreConstants.VALUE_USE_OTHER;
		preferences.setValue(ICoreConstants.TARGET_MODE, mode);
		preferences.setValue(ICoreConstants.PLATFORM_PATH, fPage.getPlatformPath());
		String[] locations = fPage.getPlatformLocations();
		for (int i = 0; i < locations.length && i < 5; i++) {
			preferences.setValue(ICoreConstants.SAVED_PLATFORM + i, locations[i]);
		}
		preferences.setValue(ICoreConstants.INCLUDE_FRAGMENTS, fIncludeFragments.getSelection());
		PDECore.getDefault().savePluginPreferences();
	}
	
	private void updateModels() {
		Iterator iter = fChangedModels.iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			model.setEnabled(fTablePart.getTableViewer().getChecked(model));
		}
	}
	
	private void selectNotInWorkspace() {
		IPluginModelBase[] wsModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
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
				fChangedModels.add(exModel);
			else if (fChangedModels.contains(exModel))
				fChangedModels.remove(exModel);
		}
		fTablePart.setSelection(selected.toArray());
	}
	
	public void handleSelectAll(boolean selected) {
		fTablePart.selectAll(selected);
	}
	
	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(fTablePart.getControl().getShell(), true);
		if (dialog.open() == Window.OK) {
			HashSet set = getPluginIDs(dialog.getSelection());
			IPluginModelBase[] models = getAllModels();
			int counter = 0;
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					if (!fPluginListViewer.getChecked(models[i])) {
						fPluginListViewer.setChecked(models[i], true);
						counter += 1;
						if (!models[i].isEnabled())
							fChangedModels.add(models[i]);
					}
					set.remove(id);
				}
				if (set.isEmpty())
					break;				
			}
			fTablePart.incrementCounter(counter);
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
						IPluginModelBase model = PDECore.getDefault().getModelManager().findModel((IProject)element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return set;
	}
	
	private void handleAddRequired() {
		TableItem[] items = fTablePart.getTableViewer().getTable().getItems();
		
		if (items.length == 0)
			return;
		
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			IPluginModelBase model = (IPluginModelBase)items[i].getData();
			if (fTablePart.getTableViewer().getChecked(model))
				addPluginAndDependencies((IPluginModelBase) items[i].getData(), result);
		}
		fTablePart.setSelection(result.toArray());
	}
	
	protected void addPluginAndDependencies(
			IPluginModelBase model,
			ArrayList selected) {
				
			if (!selected.contains(model)) {
				selected.add(model);
				if (!model.isEnabled())
					fChangedModels.add(model);
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
			boolean addFragments = fIncludeFragments.getSelection()
							|| ClasspathUtilCore.hasExtensibleAPI((IPlugin)model.getPluginBase());
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
