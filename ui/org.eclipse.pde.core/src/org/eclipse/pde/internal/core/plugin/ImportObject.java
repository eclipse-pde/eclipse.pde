/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.io.Serializable;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class ImportObject extends PluginReference implements IWritable, Serializable, IWritableDelimiter {

	private static final long serialVersionUID = 1L;
	private final IPluginImport iimport;

	public ImportObject(IPluginImport iimport) {
		super(iimport.getId());
		this.iimport = iimport;
	}

	public IPluginImport getImport() {
		return iimport;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ImportObject) {
			ImportObject io = (ImportObject) object;
			if (iimport.equals(io.getImport())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		iimport.write(indent, writer);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> key) {
		if (key.equals(ISourceObject.class)) {
			if (iimport instanceof ISourceObject) {
				return (T) iimport;
			}
		}
		return super.getAdapter(key);
	}

	@Override
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

	@Override
	public void writeDelimeter(PrintWriter writer) {
		// Note:  Cannot make into a 'IDocument*' interface.  The functionality
		// is usually done by the '*Node' classes; but, it is the opposite here
		if (iimport instanceof PluginImport) {
			((PluginImport) iimport).writeDelimeter(writer);
		}
	}

	@Override
	protected IPluginModelBase findModel() {
		String version = iimport.getVersion();
		VersionRange range = new VersionRange(version);
		return PluginRegistry.findModel(getId(), range, null);
	}

}
