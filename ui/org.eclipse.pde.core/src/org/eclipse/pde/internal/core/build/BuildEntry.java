/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.core.build;
import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.util.*;
public class BuildEntry extends BuildObject implements IBuildEntry {
	private Vector tokens = new Vector();
	private String name;
	public BuildEntry(String name) {
		this.name = name;
	}
	public void addToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.add(token);
		getModel().fireModelChanged(
				new ModelChangedEvent(getModel(), IModelChangedEvent.INSERT,
						new Object[]{token}, null));
	}
	public String getName() {
		return name;
	}
	public String[] getTokens() {
		String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}
	public boolean contains(String token) {
		return tokens.contains(token);
	}
	void processEntry(String value) {
		StringTokenizer stok = new StringTokenizer(value, ",");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			token = token.trim();
			tokens.add(token);
		}
	}
	public void removeToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.remove(token);
		getModel().fireModelChanged(
				new ModelChangedEvent(getModel(), IModelChangedEvent.REMOVE,
						new Object[]{token}, null));
	}
	public void renameToken(String oldName, String newName)
			throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.elementAt(i).toString().equals(oldName)) {
				tokens.setElementAt(newName, i);
				break;
			}
		}
		getModel().fireModelChanged(
				new ModelChangedEvent(getModel(), IModelChangedEvent.CHANGE,
						new Object[]{oldName}, null));
	}
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		String oldValue = this.name;
		this.name = name;
		getModel().fireModelObjectChanged(this, P_NAME, oldValue, name);
	}
	public String toString() {
		return name;
	}
	public void write(String indent, PrintWriter writer) {
		PropertiesUtil.writeKeyValuePair(indent, name, tokens.elements(),
				writer);
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
			throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
	}
}