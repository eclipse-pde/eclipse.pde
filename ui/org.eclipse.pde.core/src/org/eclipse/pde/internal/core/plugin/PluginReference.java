package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.core.PDECore;

public class PluginReference extends PlatformObject {
	private String id;
	private transient IPlugin plugin;
	
	public PluginReference() {}
	
	public PluginReference(String id) {
		this.id = id;
		if (id!=null)
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
		if (plugin == null && id!=null)
			plugin = PDECore.getDefault().findPlugin(id);		
		return plugin;
	}
	public String toString() {
		if (plugin!=null) {
			return plugin.getTranslatedName();
		}
		return id!=null?id:"?";
	}
	public boolean isResolved() {
		return plugin!=null;
	}
}
