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

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;

public class Bundle extends BundleObject implements IBundle {
	private Hashtable headers;

	public Bundle() {
		headers = new Hashtable();
	}

	public String getHeader(String key) {
		return (String) headers.get(key);
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
		if (value!=null)
			headers.put(name, value);
	}

	public void reset() {
		headers.clear();
	}

	public boolean isValid() {
		// must have an id and a name
		return headers.containsValue(KEY_SYMBOLIC_NAME)
			&& headers.containsValue(KEY_NAME)
			&& headers.containsValue(KEY_VERSION);
	}
	

	public void write(String indent, PrintWriter writer) {
		for (Enumeration enum = headers.keys(); enum.hasMoreElements();) {
			String key = (String) enum.nextElement();
			String value = (String) headers.get(key);
			if (isCommaSeparated(key)) {
				StringTokenizer stok = new StringTokenizer(value, ",");
				ArrayList list = new ArrayList();
				while (stok.hasMoreTokens()) {
					list.add(stok.nextToken().trim());
				}
				writeEntry(key, list, writer);
			} else {
				writeEntry(key, value, writer);
			}
		}
	}

	private void writeEntry(String key, Collection value, PrintWriter out) {
		if (value == null || value.size() == 0)
			return;
		if (value.size() == 1) {
			out.println(key + ": " + value.iterator().next());
			return;
		}
		key = key + ": ";
		out.println(key);
		out.print(' ');
		boolean first = true;
		for (Iterator i = value.iterator(); i.hasNext();) {
			if (first)
				first = false;
			else {
				out.println(',');
				out.print(' ');
			}
			out.print(i.next());
		}
		out.println();
	}

	private void writeEntry(String key, String value, PrintWriter out) {
		if (value != null && value.length() > 0)
			out.println(key + ": " + value);
	}

	private boolean isCommaSeparated(String key) {
		for (int i = 0; i < COMMA_SEPARATED_KEYS.length; i++) {
			if (COMMA_SEPARATED_KEYS[i].equals(key))
				return true;
		}
		return false;
	}
}