package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.core.PDECore;

public class PluginReference extends PlatformObject {
	private String id;
	private IPlugin plugin;
	
	public PluginReference(String id) {
		this.id = id;
		plugin = PDECore.getDefault().findPlugin(id);
	}
	public PluginReference(IPlugin plugin) {
		this.id = plugin.getId();
		this.plugin = plugin;
	}
	public String getId() {
		return id;
	}
	public IPlugin getPlugin() {
		return plugin;
	}
	public String toString() {
		if (plugin!=null) {
			return plugin.getTranslatedName();
		}
		return id;
	}
	public boolean isResolved() {
		return plugin!=null;
	}
}
