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

import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;

public class ExternalPluginsBlock {
	private CheckboxTableViewer pluginListViewer;
	private Control control;
	private ExternalPluginsEditor editor;
	public static final String SAVED_ALL = "[all]";
	public static final String SAVED_NONE = "[none]";
	private static final String KEY_RELOAD = "ExternalPluginsBlock.reload";
	private static final String KEY_WORKSPACE = "ExternalPluginsBlock.workspace";

	private final static int SELECT_ALL = 1;
	private final static int DESELECT_ALL = -1;
	private final static int SELECT_SOME = 0;

	private ExternalModelManager registry;
	private IModel[] initialModels;
	private boolean reloaded;
	private Vector changed;
	private TablePart tablePart;
	private final static boolean DEFAULT_STATE = false;

	public static final String CHECKED_PLUGINS = "PluginPath.checkedPlugins";

	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (editor == null || editor.getPlatformPath().length() > 0) {
				Object[] models = registry.getModels();
				return models;

			} else
				return new Object[0];
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String[] buttonLabels) {
			super(null, buttonLabels);
		}
		protected void elementChecked(Object element, boolean checked) {
			IPluginModel model = (IPluginModel) element;
			model.setEnabled(checked);
			if (changed == null)
				changed = new Vector();
			if (!changed.contains(model))
				changed.add(model);
			super.elementChecked(element, checked);
		}
		protected void handleSelectAll(boolean select) {
			super.handleSelectAll(select);
			IPluginModel[] models = registry.getModels();
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

	public ExternalPluginsBlock(ExternalPluginsEditor editor) {
		registry = PDEPlugin.getDefault().getExternalModelManager();
		this.editor = editor;
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
		if (editor == null) {
			gd.heightHint = 300;
			gd.widthHint = 300;
		}
		this.control = container;
		return container;
	}

	private void selectNotInWorkspace() {
		WorkspaceModelManager wm = PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wsModels = wm.getWorkspacePluginModels();
		IPluginModelBase[] exModels = registry.getModels();
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

	private static Vector createSavedList(String saved) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(saved);
		while (stok.hasMoreTokens()) {
			result.add(stok.nextToken());
		}
		return result;
	}
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	public Control getControl() {
		return control;
	}
	private void globalSelect(IPluginModel[] models, boolean selected) {
		if (changed == null)
			changed = new Vector();
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			model.setEnabled(selected);
			if (!changed.contains(model))
				changed.add(model);
		}
	}

	void handleReload() {
		final String platformPath = editor.getPlatformPath();
		final boolean useOther = editor.getUseOther();
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

	public void initialize(IPreferenceStore store) {
		String platformPath = null;
		if (editor != null)
			platformPath = editor.getPlatformPath();
		if (platformPath != null && platformPath.length() == 0)
			return;

		//store.setDefault(CHECKED_PLUGINS, SAVED_ALL);
		store.setDefault(CHECKED_PLUGINS, SAVED_NONE);

		pluginListViewer.setInput(registry);
		String saved = store.getString(CHECKED_PLUGINS);
		if (saved.length() == 0 || saved.equals(SAVED_NONE)) {
			initializeDefault(false);
		} else if (saved.equals(SAVED_ALL)) {
			initializeDefault(true);
		} else {
			Vector savedList = createSavedList(saved);

			IPluginModel[] models = registry.getModels();
			Vector selection = new Vector();
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				String id = model.getPlugin().getId();
				model.setEnabled(isChecked(id, savedList));
				if (model.isEnabled())
					selection.add(model);
			}
			tablePart.setSelection(selection.toArray());
		}
		initialModels = registry.getModels();
	}
	public static void initialize(
		ExternalModelManager registry,
		IPreferenceStore store) {
		String saved = store.getString(CHECKED_PLUGINS);

		if (saved.length() == 0 || saved.equals(SAVED_NONE)) {
			initializeDefault(registry, false);
		} else if (saved.equals(SAVED_ALL)) {
			initializeDefault(registry, true);
		} else {
			Vector savedList = createSavedList(saved);

			IPluginModel[] models = registry.getModels();
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				String id = model.getPlugin().getId();
				model.setEnabled(isChecked(id, savedList));
			}
		}
	}
	private static void initializeDefault(
		ExternalModelManager registry,
		boolean enabled) {
		IPluginModel[] models = registry.getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
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
	public void save(IPreferenceStore store) {
		String saved = "";
		IPluginModel[] models = registry.getModels();
		if (tablePart.getSelectionCount() == models.length) {
			saved = SAVED_ALL;
		} else if (tablePart.getSelectionCount() == 0) {
			saved = SAVED_NONE;
		} else {
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				if (!model.isEnabled()) {
					if (i > 0)
						saved += " ";
					saved += model.getPlugin().getId();
				}
			}
		}
		store.setValue(CHECKED_PLUGINS, saved);
		computeDelta();
	}

	public static boolean hasEnabledModels(IPreferenceStore store) {
		String saved = store.getString(CHECKED_PLUGINS);
		if (saved != null && saved.equals(SAVED_NONE))
			// we know for sure that nothing is enabled
			return false;
		// must load
		return true;
	}

	void computeDelta() {
		int type = 0;
		IModel[] addedArray = null;
		IModel[] removedArray = null;
		IModel[] changedArray = null;
		if (reloaded) {
			type = IModelProviderEvent.MODELS_REMOVED | IModelProviderEvent.MODELS_ADDED;
			removedArray = initialModels;
			addedArray = registry.getModels();
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