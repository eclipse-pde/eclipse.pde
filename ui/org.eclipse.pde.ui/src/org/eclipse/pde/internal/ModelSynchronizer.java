package org.eclipse.pde.internal;

import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.model.IModel;
import org.eclipse.pde.model.plugin.*;
import java.util.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.preferences.*;

/**
 * This class keeps workspace and external models in sync
 * by preventing the duplicates. It disables an external
 * model when a workspace model with the same id and 
 * version is added to the workspace. When this model
 * is removed, the external model will be enabled
 * unless it was disabled to start with.
 * <p>
 * The synchronizer is careful not to trip-load 
 * the external model manager by asking for models.
 * If the manager has not been loaded, it simply
 * saves the preference value. When the model
 * is loaded, it will be initialized with these values.
 * <p>
 */

public class ModelSynchronizer implements IPropertyChangeListener {
	private IModelProviderListener workspaceListener;
	private ExternalModelManager exMng;
	private Hashtable models;
	private String savedList;
	private boolean blockChanges=false;

	class ModelEntry {
		IPluginModelBase model;
		public String getId() {
			return model.getPluginBase().getId();
		}
	}

	public ModelSynchronizer() {
		models = new Hashtable();
		workspaceListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				workspaceModelsChanged(e);
			}
		};
		savedList = getPreference();
		getPreferenceStore().addPropertyChangeListener(this);
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		/*
		if (blockChanges) return;
		String property = e.getProperty();
		if (property.equals(ExternalPluginsBlock.CHECKED_PLUGINS))
			savedList = getPreference();
		*/
	}

	private IPreferenceStore getPreferenceStore() {
		return PDEPlugin.getDefault().getPreferenceStore();
	}
	private String getPreference() {
		return getPreferenceStore().getString(ExternalPluginsBlock.CHECKED_PLUGINS);
	}

	private void setPreference(String newList) {
		blockChanges=true;
		getPreferenceStore().setValue(ExternalPluginsBlock.CHECKED_PLUGINS, newList);
		blockChanges=false;
	}

	private void workspaceModelsChanged(IModelProviderEvent e) {
		/*
		if (savedList != null && savedList.equals(ExternalPluginsBlock.SAVED_NONE))
				return;
		IModel[] added = e.getAddedModels();
		IModel[] removed = e.getRemovedModels();
		if (added != null) {
			for (int i = 0; i < added.length; i++) {
				IPluginModelBase model = (IPluginModelBase) added[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, true);
			}
		}
		if (removed != null) {
			for (int i = 0; i < removed.length; i++) {
				IPluginModelBase model = (IPluginModelBase) removed[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, false);
			}
		}
		synchronize();
		*/
	}

	private void updateTable(String id, IPluginModelBase wmodel, boolean added) {
		ModelEntry entry = (ModelEntry) models.get(id);
		if (added && entry == null) {
			entry = new ModelEntry();
			models.put(id, entry);
		}
		if (added)
			entry.model = wmodel;
		else
			models.remove(id);
	}

	private void synchronize() {
		String newList = null;

		if (models.isEmpty()) {
			newList = savedList;
		} else {
			String initialList = savedList;
			if (initialList != null && initialList.equals(ExternalPluginsBlock.SAVED_ALL))
				initialList = null;

			ArrayList entries = createSavedEntries(initialList);
			for (Enumeration enum = models.elements(); enum.hasMoreElements();) {
				ModelEntry entry = (ModelEntry) enum.nextElement();
				String id = entry.getId();
				if (!entries.contains(id))
					entries.add(id);
			}
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < entries.size(); i++) {
				String entry = entries.get(i).toString();
				if (i > 0)
					buff.append(" ");
				buff.append(entry);
			}
			newList = buff.toString();
		}
		setPreference(newList);
		if (exMng.isLoaded()) {
			ExternalPluginsBlock.initialize(exMng, getPreferenceStore());
		}
	}
	private ArrayList createSavedEntries(String initialList) {
		ArrayList entries = new ArrayList();
		if (initialList != null && initialList.length() > 0) {
			StringTokenizer stok = new StringTokenizer(initialList);
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken();
				entries.add(token);
			}
		}
		return entries;
	}

	public void setExternalModelManager(ExternalModelManager manager) {
		exMng = manager;
	}
	public void register(WorkspaceModelManager manager) {
		manager.addModelProviderListener(workspaceListener);
	}
	public void unregister(WorkspaceModelManager manager) {
		manager.removeModelProviderListener(workspaceListener);
	}
	public void shutdown() {
		getPreferenceStore().removePropertyChangeListener(this);		
	}
}