/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.io.Serializable;

import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ImportObject extends PluginReference implements IWritable, Serializable, IWritableDelimeter {

	private static final long serialVersionUID = 1L;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		iimport.write(indent, writer);
	}
	
	public Object getAdapter(Class key) {
		if (key.equals(ISourceObject.class)) {
			if (iimport instanceof ISourceObject)
				return iimport;
		}
		return super.getAdapter(key);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.PluginReference#reconnect(org.eclipse.pde.core.plugin.IPlugin)
	 */
	public void reconnect(IPlugin plugin) {
		super.reconnect(plugin);
		// Field that has transient fields:  Import
		IPluginModelBase model = plugin.getPluginModel();
		IPluginBase parent = model.getPluginBase();
		// TODO: MP: CCP: Make into interface?
		if (iimport instanceof PluginImport) {
			((PluginImport)iimport).reconnect(model, parent);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// TODO: MP: CCP: Make into interface?
		if (iimport instanceof PluginImport) {
			((PluginImport)iimport).writeDelimeter(writer);
		}
	}
	
}
