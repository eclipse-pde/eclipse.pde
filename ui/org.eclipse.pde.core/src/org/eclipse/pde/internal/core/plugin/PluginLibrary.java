/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.w3c.dom.*;

public class PluginLibrary extends PluginObject implements IPluginLibrary {
	private String[] contentFilters;
	private String[] packages;
	private boolean exported = false;
	private String type;
	private static final int GROUP_COUNT = Integer.MAX_VALUE;

	public PluginLibrary() {
	}
	
	public boolean isValid() {
		return name!=null;
	}
	public String[] getContentFilters() {
		return contentFilters;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#addContentFilter(java.lang.String)
	 */
	public void addContentFilter(String filter) throws CoreException {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#removeContentFilter(java.lang.String)
	 */
	public void removeContentFilter(String filter) throws CoreException {
	}
	public String[] getPackages() {
		return packages;
	}
	public boolean isExported() {
		return exported;
	}
	public boolean isFullyExported() {
		return exported
			&& (contentFilters == null || contentFilters.length == 0);
	}

	public String getType() {
		return type;
	}

	
	public void load(String name) {
		this.name = name;
		this.exported = true;
	}
	
	void load(Node node, Hashtable lineTable) {
		this.name = getNodeAttribute(node, "name"); //$NON-NLS-1$
		this.type = getNodeAttribute(node, "type"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		Vector exports = new Vector();
		Vector prefixes = new Vector();
		boolean all = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase();
				if (tag.equals("export")) { //$NON-NLS-1$
					String ename = getNodeAttribute(child, "name"); //$NON-NLS-1$
					if (ename != null) {
						ename = ename.trim();
						if (ename.equals("*")) { //$NON-NLS-1$
							all = true;
						} else {
							exports.add(ename);
						}
					}
				} else if (tag.equals("packages")) { //$NON-NLS-1$
					String ename = getNodeAttribute(child, "prefixes"); //$NON-NLS-1$
					if (ename != null) {
						ename = ename.trim();
						StringTokenizer stok = new StringTokenizer(ename, ","); //$NON-NLS-1$
						while (stok.hasMoreTokens()) {
							prefixes.add(stok.nextToken());
						}
					}
				}
			}
		}
		if (exports.size() > 0) {
			contentFilters = new String[exports.size()];
			exports.copyInto(contentFilters);
		}
		if (prefixes.size() > 0) {
			packages = new String[prefixes.size()];
			prefixes.copyInto(packages);
		}
		exported = all || exports.size() > 0;
		bindSourceLocation(node, lineTable);
	}
	public void setContentFilters(String[] filters) throws CoreException {
		ensureModelEditable();
		ArrayList oldValue = createArrayList(contentFilters);
		contentFilters = filters;
		firePropertyChanged(
			P_CONTENT_FILTERS,
			oldValue,
			createArrayList(filters));
	}

	public void setPackages(String[] packages) throws CoreException {
		ensureModelEditable();
		ArrayList oldValue = createArrayList(this.packages);
		this.packages = packages;
		firePropertyChanged(P_PACKAGES, oldValue, createArrayList(packages));
	}

	public void setExported(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.exported);
		this.exported = value;
		firePropertyChanged(P_EXPORTED, oldValue, new Boolean(value));
	}

	public void setType(String type) throws CoreException {
		ensureModelEditable();
		String oldValue = this.type;
		this.type = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_CONTENT_FILTERS)) {
			ArrayList list = (ArrayList) newValue;
			if (list != null)
				setContentFilters(
					(String[]) list.toArray(new String[list.size()]));
			else
				setContentFilters(null);
			return;
		}
		if (name.equals(P_PACKAGES)) {
			ArrayList list = (ArrayList) newValue;
			if (list != null)
				setPackages((String[]) list.toArray(new String[list.size()]));
			else
				setPackages(null);
			return;
		}
		if (name.equals(P_EXPORTED)) {
			setExported(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	private ArrayList createArrayList(String[] array) {
		if (array == null)
			return null;
		ArrayList list = new ArrayList();
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<library name=\"" + getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (type != null)
			writer.print(" type=\"" + type + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (isExported() == false && packages == null) {
			writer.println("/>"); //$NON-NLS-1$
		} else {
			writer.println(">"); //$NON-NLS-1$
			String indent2 = indent + "   "; //$NON-NLS-1$
			if (isExported()) {
				if (isFullyExported()) {
					writer.println(indent2 + "<export name=\"*\"/>"); //$NON-NLS-1$
				} else {
					for (int i = 0; i < contentFilters.length; i++) {
						writer.println(
							indent2
								+ "<export name=\"" //$NON-NLS-1$
								+ contentFilters[i]
								+ "\"/>"); //$NON-NLS-1$
					}
				}
			}
			if (packages != null) {
				ArrayList groups = computePackageGroups(packages);
				for (int i = 0; i < groups.size(); i++) {
					writer.println(
						indent2
							+ "<packages prefixes=\"" //$NON-NLS-1$
							+ (String)groups.get(i)
							+ "\"/>"); //$NON-NLS-1$
				}
			}
			writer.println(indent + "</library>"); //$NON-NLS-1$
		}
	}
	private ArrayList computePackageGroups(String [] packages) {
		StringBuffer buff = new StringBuffer();
		ArrayList list = new ArrayList();
		int counter = 0;
		
		for (int i=0; i<packages.length; i++) {
			counter++;
			
			if (counter>1)
				buff.append(","); //$NON-NLS-1$
			buff.append(packages[i]);
			
			if (counter==GROUP_COUNT) {
				counter=0;
				list.add(buff.toString());
				buff.delete(0, buff.length());
			}
		}
		if (counter>0)
			list.add(buff.toString());
		return list;
	}
}
