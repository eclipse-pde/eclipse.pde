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
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;

public abstract class PluginObject
	extends PlatformObject
	implements IPluginObject, ISourceObject, Serializable {
	protected String name;
	private String translatedName;
	private transient IPluginObject parent;
	private transient ISharedPluginModel model;
	protected Vector leadComments;
	protected int[] range;
	private boolean inTheModel;

	public PluginObject() {
	}
	public boolean isValid() {
		return true;
	}
	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECore.getResourceString("PluginObject.readOnlyChange")); //$NON-NLS-1$
		}
	}

	public void setInTheModel(boolean value) {
		inTheModel = value;
	}

	public boolean isInTheModel() {
		return inTheModel;
	}

	protected void firePropertyChanged(
		String property,
		Object oldValue,
		Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
	protected void firePropertyChanged(
		IPluginObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelObjectChanged(
				object,
				property,
				oldValue,
				newValue);
		}
	}
	protected void fireStructureChanged(IPluginObject child, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e =
				new ModelChangedEvent((IModelChangeProvider)model, changeType, new Object[] { child }, null);
			fireModelChanged(e);
		}
	}
	protected void fireModelChanged(IModelChangedEvent e) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(e);
		}
	}
	public ISharedPluginModel getModel() {
		return model;
	}
	
	public IPluginModelBase getPluginModel() {
		if (model instanceof IBundlePluginModelProvider)
			return ((IBundlePluginModelProvider)model).getBundlePluginModel();
		
		return model instanceof IPluginModelBase? (IPluginModelBase)model : null;
	}
	
	public String getName() {
		return name;
	}

	public String getTranslatedName() {
		if (translatedName != null && !model.isEditable())
			return translatedName;
		if (translatedName == null && name != null && model != null) {
			translatedName = model.getResourceString(name);
		}
		return translatedName;
	}

	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}
	public IPluginObject getParent() {
		return parent;
	}
	public IPluginBase getPluginBase() {
		IPluginModelBase pluginModel = getPluginModel();
		return pluginModel != null ? pluginModel.getPluginBase() : null;
	}
	public String getResourceString(String key) {
		return model.getResourceString(key);
	}
	static boolean isNotEmpty(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i)) == false)
				return true;
		}
		return false;
	}
	abstract void load(Node node, Hashtable lineTable);

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
	}

	public void setModel(ISharedPluginModel model) {
		this.model = model;
		translatedName = null;
	}
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		String oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}
	public void setParent(IPluginObject parent) {
		this.parent = parent;
	}
	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(
				IStatus.ERROR,
				PDECore.getPluginId(),
				IStatus.OK,
				message,
				null);
		CoreException ce = new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}
	public String toString() {
		if (name != null)
			return name;
		return super.toString();
	}

	public void addComments(Node node) {
		leadComments = addComments(node, leadComments);
	}

	public Vector addComments(Node node, Vector result) {
		for (Node prev = node.getPreviousSibling();
			prev != null;
			prev = prev.getPreviousSibling()) {
			if (prev.getNodeType() == Node.TEXT_NODE)
				continue;
			if (prev instanceof Comment) {
				String comment = prev.getNodeValue();
				if (result == null)
					result = new Vector();
				result.add(0,comment);
			} else
				break;
		}
		return result;
	}

	void writeComments(PrintWriter writer) {
		writeComments(writer, leadComments);
	}

	void writeComments(PrintWriter writer, Vector source) {
		if (source == null)
			return;
		for (int i = 0; i < source.size(); i++) {
			String comment = (String) source.elementAt(i);
			writer.println("<!--" + comment + "-->"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected boolean stringEqualWithNull(String a, String b) {
		return a == null && b == null || a != null && b != null && a.equals(b);
	}

	public String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	protected void bindSourceLocation(Node node, Map lineTable) {
		Integer[] lines = (Integer[]) lineTable.get(node);
		if (lines != null) {
			range = new int[2];
			range[0] = lines[0].intValue();
			range[1] = lines[1].intValue();
		}
	}

	public int getStartLine() {
		if (range == null)
			return -1;
		return range[0];
	}
	public int getStopLine() {
		if (range == null)
			return -1;
		return range[1];
	}
}
