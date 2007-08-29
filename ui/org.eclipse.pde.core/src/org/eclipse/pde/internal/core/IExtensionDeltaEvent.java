package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.plugin.IPluginModelBase;

public interface IExtensionDeltaEvent {
	/**
	 * Event is sent after the models have been added.
	 */
	int MODELS_ADDED = 0x1;
	/**
	 * Event is sent before the models will be removed.
	 */
	int MODELS_REMOVED = 0x2;
	/**
	 * Event is sent after the models have been changed.
	 */
	int MODELS_CHANGED = 0x4;
	
	public IPluginModelBase[] getAddedModels();
	public IPluginModelBase[] getChangedModels();
	public IPluginModelBase[] getRemovedModels();
	public int getEventTypes();

}
