/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;

import org.eclipse.pde.core.*;
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
	public Object getAdapter(Class key) {
		if (key.equals(ISourceObject.class)) {
			if (iimport instanceof ISourceObject)
				return iimport;
		}
		return super.getAdapter(key);
	}
}
