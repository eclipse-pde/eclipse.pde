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
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class ExternalPluginsBlock {
	private CheckboxTableViewer pluginListViewer;
	private TargetPlatformPreferencePage page;
	private static final String KEY_RELOAD = "ExternalPluginsBlock.reload";
	private static final String KEY_WORKSPACE = "ExternalPluginsBlock.workspace";
	private Vector models;
	private Vector fmodels;
	private boolean reloaded;
	private TablePart tablePart;
	private HashSet changed = new HashSet();
	private IPluginModelBase[] initialModels;


	private final static boolean DEFAULT_STATE = true;
	
	class ReloadOperation implements IRunnableWithProgress {
		private Vector models = new Vector();
		private Vector fmodels = new Vector();
		private String[] pluginPaths;
		
		public ReloadOperation(String[] pluginPaths) {
			 this.pluginPaths = pluginPaths;
		}
			
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
			RegistryLoader.reload(pluginPaths, models, fmodels, monitor);
		}
		
		public Vector getPluginModels() {
			return models;
		}
		
		public Vector getFragmentModels() {
			return fmodels;
		}

	}
	
	class SaveOperation implements IWorkspaceRunnable {
		public void run(IProgressMonitor monitor) {
			savePreferences();
			if (reloaded) {
				EclipseHomeInitializer.resetEclipseHomeVariables();
			}
			updateModels();
			computeDelta();
			if (reloaded) {
				PDECore.getDefault().getSourceLocationManager().reinitializeClasspathVariables(null);
			}
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
				case 0 :
					handleReload();
					break;
				case 5 :
					selectNotInWorkspace();
					break;
				default :
					super.buttonSelected(button, index);
			}
		}

		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
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


	}

	public ExternalPluginsBlock(TargetPlatformPreferencePage page) {
		this.page = page;
		String[] buttonLabels =
			{
				PDEPlugin.getResourceString(KEY_RELOAD),
				null,
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_DESELECT_ALL),
				null,
				PDEPlugin.getResourceString(KEY_WORKSPACE)};
		tablePart = new TablePart(buttonLabels);
		tablePart.setSelectAllIndex(2);
		tablePart.setDeselectAllIndex(3);
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
		}
		if (changed.size() > 0) {
			type |= IModelProviderEvent.MODELS_CHANGED;
			changedArray = (IModel[]) changed.toArray(new IModel[changed.size()]);
			changed.clear();
		}
		if (type != 0) {
			IExternalModelManager registry =
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
		layout.marginHeight = 15;
		layout.marginWidth = 0;
		container.setLayout(layout);

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ExternalPluginsBlock.title"));
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		tablePart.createControl(container);

		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 100;
		return container;
	}


	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private IPluginModelBase[] getAllModels() {
		if (models == null && fmodels == null) {
			initialModels =
				PDECore.getDefault().getExternalModelManager().getAllModels();
			return initialModels;
		}

		IPluginModelBase[] allModels =
			new IPluginModelBase[models.size() + fmodels.size()];
		System.arraycopy(
			models.toArray(new IPluginModel[models.size()]),
			0,
			allModels,
			0,
			models.size());
		System.arraycopy(
			fmodels.toArray(new IFragmentModel[fmodels.size()]),
			0,
			allModels,
			models.size(),
			fmodels.size());

		return allModels;
	}

	void handleReload() {
		String platformPath = page.getPlatformPath();
		if (platformPath != null && platformPath.length() > 0) {
			String[] pluginPaths = PluginPathFinder.getPluginPaths(platformPath);
			ReloadOperation op = new ReloadOperation(pluginPaths);
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			try {
				pmd.run(true, false, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
			models = op.getPluginModels();
			fmodels = op.getFragmentModels();
			pluginListViewer.refresh();
			
			tablePart.selectAll(DEFAULT_STATE);
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
				try {
					JavaCore.run(new SaveOperation(), null);
				} catch (CoreException e) {
				}
			}
		});
	}
	
	private void savePreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		IPath newPath = new Path(page.getPlatformPath());
		IPath defaultPath = new Path(ExternalModelManager.computeDefaultPlatformPath());
		String mode =
			newPath.equals(defaultPath)
				? ICoreConstants.VALUE_USE_THIS
				: ICoreConstants.VALUE_USE_OTHER;
		preferences.setValue(ICoreConstants.TARGET_MODE, mode);
		preferences.setValue(ICoreConstants.PLATFORM_PATH, page.getPlatformPath());
		String[] locations = page.getPlatformLocations();
		for (int i = 0; i < locations.length && i < 5; i++) {
			preferences.setValue(ICoreConstants.SAVED_PLATFORM + i, locations[i]);
		}
		PDECore.getDefault().savePluginPreferences();
	}
	
	private void updateModels() {
		Iterator iter = changed.iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			model.setEnabled(tablePart.getTableViewer().getChecked(model));
		}

		if (reloaded) {
			PDECore.getDefault().getExternalModelManager().resetModels(models, fmodels);
		}
	}


	private void selectNotInWorkspace() {
		IWorkspaceModelManager wm = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wsModels = wm.getAllModels();
		IPluginModelBase[] exModels = getAllModels();
		Vector selected = new Vector();
		for (int i = 0; i < exModels.length; i++) {
			IPluginModelBase exModel = exModels[i];
			boolean inWorkspace = false;
			for (int j = 0; j < wsModels.length; j++) {
				IPluginModelBase wsModel = wsModels[j];
				if (exModel
					.getPluginBase()
					.getId()
					.equals(wsModel.getPluginBase().getId())) {
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

}
