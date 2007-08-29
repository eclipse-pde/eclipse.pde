package org.eclipse.pde.internal.core.search;

import java.util.HashSet;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;

public class ExtensionPluginSearchScope extends PluginSearchScope {
	
	PluginSearchInput fInput = null;

	public ExtensionPluginSearchScope(PluginSearchInput input) {
		super();
		fInput = input;
	}

	public ExtensionPluginSearchScope(int workspaceScope, int externalScope,
			HashSet selectedResources, PluginSearchInput input) {
		super(workspaceScope, externalScope, selectedResources);
		fInput = input;
	}

	public IPluginModelBase[] getMatchingModels() {
		if (fInput == null)
			return new IPluginModelBase[0];
		String pointId = fInput.getSearchString();
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IPluginModelBase[] models = null;
		if (fInput.getSearchLimit() == PluginSearchInput.LIMIT_REFERENCES) {
			models =registry.findExtensionPlugins(pointId); 
		} else {
			IPluginModelBase base = registry.findExtensionPointPlugin(pointId);
			models = (base == null) ? new IPluginModelBase[0] : new IPluginModelBase[] { base };
		}
		return addRelevantModels(models);
	}

}
