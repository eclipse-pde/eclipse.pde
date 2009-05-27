/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
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

public class ImportObject extends PluginReference implements IWritable, Serializable, IWritableDelimiter {

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
			ImportObject io = (ImportObject) object;
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
	public void reconnect(IPluginModelBase model) {
		super.reconnect(model);
		// Field that has transient fields:  Import
		IPluginBase parent = model.getPluginBase();
		// Note:  Cannot make into a 'IDocument*' interface.  The functionality
		// is usually done by the '*Node' classes; but, it is the opposite here
		if (iimport instanceof PluginImport) {
			((PluginImport) iimport).reconnect(model, parent);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// Note:  Cannot make into a 'IDocument*' interface.  The functionality
		// is usually done by the '*Node' classes; but, it is the opposite here
		if (iimport instanceof PluginImport) {
			((PluginImport) iimport).writeDelimeter(writer);
		}
	}

}
