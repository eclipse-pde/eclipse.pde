package org.eclipse.pde.internal.core.plugin;

import java.io.*;

import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.*;

public class ImportObject extends PluginReference implements IWritable, Serializable {
	private IPluginImport iimport;
	
	public ImportObject() {
		super();
	}
	public ImportObject(IPluginImport iimport) {
		super(iimport.getId());
		this.iimport = iimport;
	}
	public ImportObject(IPluginImport iimport, IPlugin plugin) {
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
