package org.eclipse.pde.internal.model;

import org.eclipse.pde.model.plugin.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.PDEPlugin;

public class ImportObject extends PluginReference {
	private IPluginImport iimport;
	
	public ImportObject(IPluginImport iimport) {
		super(iimport.getId());
		this.iimport = iimport;
	}
	public ImportObject(IPluginImport iiport, IPlugin plugin) {
		super(plugin);
		this.iimport = iimport;
	}
	public IPluginImport getImport() {
		return iimport;
	}
	public boolean equals(Object object) {
		if (object instanceof ImportObject) {
			ImportObject io = (ImportObject)object;
			if (iimport.equals(io.getImport()))
			   return true;
		}
		return false;
	}
}
