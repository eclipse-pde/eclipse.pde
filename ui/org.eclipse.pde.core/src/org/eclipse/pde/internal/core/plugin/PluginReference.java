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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

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
