package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.pde.internal.ui.*;
import java.util.*;
import java.io.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.core.*;
import org.eclipse.swt.custom.*;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;

public class ExternalPluginsBlock {
	private CheckboxTableViewer pluginListViewer;
	private Control control;
	private TargetPlatformPreferencePage page;
	private static final String KEY_RELOAD = "ExternalPluginsBlock.reload";
	private static final String KEY_WORKSPACE = "ExternalPluginsBlock.workspace";
	private ExternalModelManager registry;
	private IModel[] initialModels;
	private boolean reloaded;
	private Vector changed;
	private TablePart tablePart;
	private final static boolean DEFAULT_STATE = false;

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
		protected void elementChecked(Object element, boolean checked) {
			IPluginModelBase model = (IPluginModelBase) element;
			model.setEnabled(checked);
			if (changed == null)
				changed = new Vector();
			if (!changed.contains(model))
				changed.add(model);
			super.elementChecked(element, checked);
		}
		protected void handleSelectAll(boolean select) {
			super.handleSelectAll(select);
			IPluginModelBase[] models = getAllModels();
			globalSelect(models, select);
		}
		protected void buttonSelected(Button button, int index) {
			if (index == 0)
				handleReload();
			else if (index == 5)
				selectNotInWorkspace();
			else
				super.buttonSelected(button, index);
		}
	}

	public ExternalPluginsBlock(TargetPlatformPreferencePage page) {
		registry = PDECore.getDefault().getExternalModelManager();
		this.page = page;
		String[] buttonLabels =
			{
				PDEPlugin.getResourceString(KEY_RELOAD),
				null,
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_DESELECT_ALL),
				null,
				PDEPlugin.getResourceString(KEY_WORKSPACE)};
		tablePart = new TablePart(buttonLabels);
		tablePart.setSelectAllIndex(2);
		tablePart.setDeselectAllIndex(3);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		tablePart.createControl(container);

		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 200;
		this.control = container;
		return container;
	}

	private void selectNotInWorkspace() {
		WorkspaceModelManager wm = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wsModels = wm.getWorkspacePluginModels();
		IPluginModelBase[] exModels = getAllModels();
		Vector selected = new Vector();
		for (int i = 0; i < exModels.length; i++) {
			IPluginModelBase exModel = exModels[i];
			boolean inWorkspace = false;
			for (int j = 0; j < wsModels.length; j++) {
				IPluginModelBase wsModel = wsModels[j];
				if (exModel.getPluginBase().getId().equals(wsModel.getPluginBase().getId())) {
					inWorkspace = true;
					break;
				}
			}
			exModel.setEnabled(!inWorkspace);
			if (!inWorkspace)
				selected.add(exModel);
		}
		tablePart.setSelection(selected.toArray());
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	public Control getControl() {
		return control;
	}
	private void globalSelect(IPluginModelBase[] models, boolean selected) {
		if (changed == null)
			changed = new Vector();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			model.setEnabled(selected);
			if (!changed.contains(model))
				changed.add(model);
		}
	}

	void handleReload() {
		final String platformPath = page.getPlatformPath();
		final boolean useOther = page.getUseOther();
		if (platformPath != null && platformPath.length() > 0) {
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
						//monitor.beginTask("Reloading", IProgressMonitor.UNKNOWN);
	if (useOther)
						registry.reload(platformPath, monitor);
					else
						registry.reloadFromLive(monitor);
					monitor.done();
				}
			};
			ProgressMonitorDialog pm = new ProgressMonitorDialog(control.getShell());
			try {
				pm.run(false, false, op);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			}

		} else {
			registry.clear();
		}
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed()) {
						BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
							public void run() {
								pluginListViewer.refresh();
								initializeDefault(DEFAULT_STATE);
							}
						});
					}
				}
			});
			reloaded = true;
		}
	}

	public void initialize() {
		String platformPath = page.getPlatformPath();
		if (platformPath != null && platformPath.length() == 0)
			return;

		pluginListViewer.setInput(registry);
		CoreSettings store = PDECore.getDefault().getSettings();
		String saved = store.getString(ICoreConstants.CHECKED_PLUGINS);
		IPluginModelBase [] allModels = getAllModels();
		if (saved.length() == 0 || saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			initializeDefault(false);
		} else if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			initializeDefault(true);
		} else {
			Vector savedList = createSavedList(saved);

			Vector selection = new Vector();
			for (int i = 0; i < allModels.length; i++) {
				IPluginModelBase model = allModels[i];
				if (model.isEnabled())
					selection.add(model);
			}
			tablePart.setSelection(selection.toArray());
		}
		initialModels = allModels;
	}
	private static void initializeDefault(
		ExternalModelManager registry,
		boolean enabled) {
		IPluginModelBase[] models = getAllModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			model.setEnabled(enabled);
		}
	}
	public void initializeDefault(boolean enabled) {
		initializeDefault(registry, enabled);
		tablePart.selectAll(enabled);
	}

	private static boolean isChecked(String name, Vector list) {
		for (int i = 0; i < list.size(); i++) {
			if (name.equals(list.elementAt(i)))
				return false;
		}
		return true;
	}
	private static IPluginModelBase[] getAllModels() {
		ExternalModelManager registry = PDECore.getDefault().getExternalModelManager();
		IPluginModel[] models = registry.getModels();
		IFragmentModel[] fmodels = registry.getFragmentModels(null);
		IPluginModelBase[] all = new IPluginModelBase[models.length + fmodels.length];
		System.arraycopy(models, 0, all, 0, models.length);
		System.arraycopy(fmodels, 0, all, models.length, fmodels.length);
		return all;
	}
	public void save() {
		String saved = "";
		IPluginModelBase[] models = getAllModels();
		if (tablePart.getSelectionCount() == models.length) {
			saved = ICoreConstants.VALUE_SAVED_ALL;
		} else if (tablePart.getSelectionCount() == 0) {
			saved = ICoreConstants.VALUE_SAVED_NONE;
		} else {
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				if (!model.isEnabled()) {
					if (i > 0)
						saved += " ";
					saved += model.getPluginBase().getId();
				}
			}
		}
		PDECore.getDefault().getSettings().setValue(
			ICoreConstants.CHECKED_PLUGINS,
			saved);
		computeDelta();
	}
	private Vector createSavedList(String saved) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(saved);
		while (stok.hasMoreTokens()) {
			result.add(stok.nextToken());
		}
		return result;
	}

	void computeDelta() {
		int type = 0;
		IModel[] addedArray = null;
		IModel[] removedArray = null;
		IModel[] changedArray = null;
		if (reloaded) {
			type = IModelProviderEvent.MODELS_REMOVED | IModelProviderEvent.MODELS_ADDED;
			removedArray = initialModels;
			addedArray = getAllModels();
		}
		if (changed != null && changed.size() > 0) {
			type |= IModelProviderEvent.MODELS_CHANGED;
			changedArray = (IModel[]) changed.toArray(new IModel[changed.size()]);
			changed = null;
		}
		if (type != 0) {
			ModelProviderEvent event =
				new ModelProviderEvent(registry, type, addedArray, removedArray, changedArray);
			registry.fireModelProviderEvent(event);
		}
	}

}