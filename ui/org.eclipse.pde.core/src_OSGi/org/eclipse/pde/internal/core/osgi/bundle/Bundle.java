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
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.PrintWriter;
import java.util.*;
import java.util.Hashtable;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.osgi.bundle.IBundle;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.util.PropertiesUtil;

public class Bundle extends BundleObject implements IBundle {
	private Hashtable headers;

	public Bundle() {
		headers = new Hashtable();
	}

	public String getHeader(String key) {
		return (String)headers.get(key);
	}

	public void setHeader(String key, String value) throws CoreException {
		ensureModelEditable();
		String oldValue = getHeader(key);
		if (value == null)
			headers.remove(key);
		else
			headers.put(key, value);
		getModel().fireModelChanged(
			new ModelChangedEvent(this, key, oldValue, value));
	}

	public void processHeader(String name, String value) {
		headers.put(name, value);
	}

	public void reset() {
		headers.clear();
	}
	
	public boolean isValid() {
		// must have an id and a name
		return headers.containsValue(KEY_NAME) &&
		headers.containsValue(KEY_DESC) &&
		headers.containsValue(KEY_VERSION);
	}
	
	public void load(IPluginBase plugin, IProgressMonitor monitor) {
		reset();
		monitor.beginTask("", 1);
		// migrate from a plug-in
		headers.put(KEY_NAME, plugin.getId());
		headers.put(KEY_DESC, plugin.getName());
		headers.put(KEY_VENDOR, plugin.getProviderName());
		headers.put(KEY_VERSION, plugin.getVersion());
		monitor.worked(1);
	}

	public void write(String indent, PrintWriter writer) {
		for (Enumeration enum = headers.keys(); enum.hasMoreElements();) {
			String key = (String)enum.nextElement();
			String value = (String)headers.get(key);
			if (isCommaSeparated(key)) {
				StringTokenizer stok = new StringTokenizer(value, ",");
				PropertiesUtil.writeKeyValuePair("", key, stok, writer);
			}
			else {
				PropertiesUtil.writeKeyValuePair("", key, value, writer);
			}
		}
	}
	
	private boolean isCommaSeparated(String key) {
		for (int i=0; i<COMMA_SEPARATED_KEYS.length; i++) {
			if (COMMA_SEPARATED_KEYS[i].equals(key))
				return true;
		}
		return false;
	}
}
