package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.core.IWritable;
import java.io.*;

public class ImportObject extends PluginReference implements IWritable, Serializable {
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
	public void write(String indent, PrintWriter writer) {
		iimport.write(indent, writer);
	}
}
