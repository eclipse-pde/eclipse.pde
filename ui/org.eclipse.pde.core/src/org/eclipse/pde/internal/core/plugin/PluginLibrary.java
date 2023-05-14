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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PluginLibrary extends PluginObject implements IPluginLibrary {

	private static final long serialVersionUID = 1L;
	private String[] fContentFilters;
	private boolean fExported = false;
	private String fType;

	public PluginLibrary() {
	}

	@Override
	public boolean isValid() {
		return fName != null;
	}

	@Override
	public String[] getContentFilters() {
		IPluginModelBase model = (IPluginModelBase) getModel();
		if (ClasspathUtilCore.hasBundleStructure(model)) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				ArrayList<String> list = new ArrayList<>();
				ExportPackageDescription[] exports = desc.getExportPackages();
				for (ExportPackageDescription export : exports) {
					list.add(export.getName());
				}
				return list.toArray(new String[list.size()]);
			}
		}
		if (!isExported()) {
			return new String[0];
		}
		return isFullyExported() ? new String[] {"**"} : fContentFilters; //$NON-NLS-1$
	}

	@Override
	public void addContentFilter(String filter) throws CoreException {
	}

	@Override
	public void removeContentFilter(String filter) throws CoreException {
	}

	@Override
	public String[] getPackages() {
		return new String[0];
	}

	@Override
	public boolean isExported() {
		return fExported;
	}

	@Override
	public boolean isFullyExported() {
		return fExported && (fContentFilters == null || fContentFilters.length == 0);
	}

	@Override
	public String getType() {
		return fType;
	}

	public void load(String name) {
		fName = name;
		fExported = true;
	}

	void load(Node node) {
		fName = getNodeAttribute(node, "name"); //$NON-NLS-1$
		fType = getNodeAttribute(node, "type"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		ArrayList<String> exports = new ArrayList<>();
		boolean all = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
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
				}
			}
		}
		if (!exports.isEmpty()) {
			fContentFilters = new String[exports.size()];
			exports.toArray(fContentFilters);
		}
		fExported = all || !exports.isEmpty();
	}

	@Override
	public void setContentFilters(String[] filters) throws CoreException {
		ensureModelEditable();
		ArrayList<String> oldValue = createArrayList(fContentFilters);
		fContentFilters = filters;
		firePropertyChanged(P_CONTENT_FILTERS, oldValue, createArrayList(filters));
	}

	@Override
	public void setPackages(String[] packages) throws CoreException {
	}

	@Override
	public void setExported(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = Boolean.valueOf(fExported);
		fExported = value;
		firePropertyChanged(P_EXPORTED, oldValue, Boolean.valueOf(value));
	}

	@Override
	public void setType(String type) throws CoreException {
		ensureModelEditable();
		String oldValue = fType;
		fType = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_CONTENT_FILTERS)) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) newValue;
			if (list != null) {
				setContentFilters(list.toArray(new String[list.size()]));
			} else {
				setContentFilters(null);
			}
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

	private ArrayList<String> createArrayList(String[] array) {
		if (array == null) {
			return null;
		}
		ArrayList<String> list = new ArrayList<>();
		Collections.addAll(list, array);
		return list;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		// Get the model
		IPluginModelBase modelBase = getPluginModel();
		// check to see if the model is a bundle model
		if ((modelBase instanceof IBundlePluginModelBase) == false) {
			writer.print(indent);
			writer.print("<library name=\"" + PDEXMLHelper.getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (fType != null) {
				writer.print(" type=\"" + fType + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (!isExported()) {
				writer.println("/>"); //$NON-NLS-1$
			} else {
				writer.println(">"); //$NON-NLS-1$
				String indent2 = indent + "   "; //$NON-NLS-1$
				if (isExported()) {
					if (isFullyExported()) {
						writer.println(indent2 + "<export name=\"*\"/>"); //$NON-NLS-1$
					} else {
						for (String filter : fContentFilters) {
							writer.println(indent2 + "<export name=\"" //$NON-NLS-1$
									+ filter + "\"/>"); //$NON-NLS-1$
						}
					}
				}
				writer.println(indent + "</library>"); //$NON-NLS-1$
			}
		} else {
			writer.print(PDEXMLHelper.getWritableString(getName()));
		}
	}

	@Override
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		super.reconnect(model, parent);
		// No transient fields
	}

	@Override
	public void writeDelimeter(PrintWriter writer) {
		writer.println(',');
		writer.print(' ');
	}

}
