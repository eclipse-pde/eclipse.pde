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

public abstract class AbstractExtensions
	extends PluginObject
	implements IExtensions {
	protected Vector extensions = new Vector();
	protected Vector extensionPoints = new Vector();

	public AbstractExtensions() {
	}
	public void add(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		extensions.addElement(extension);
		((PluginExtension) extension).setInTheModel(true);
		((PluginExtension) extension).setParent(this);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}
	public void add(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		ensureModelEditable();
		extensionPoints.addElement(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(true);
		((PluginExtensionPoint) extensionPoint).setParent(this);
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
	}

	public IPluginExtensionPoint[] getExtensionPoints() {
		IPluginExtensionPoint[] result =
			new IPluginExtensionPoint[extensionPoints.size()];
		extensionPoints.copyInto(result);
		return result;
	}
	public IPluginExtension[] getExtensions() {
		IPluginExtension[] result = new IPluginExtension[extensions.size()];
		extensions.copyInto(result);
		return result;
	}

	/*void load(PluginModel pd) {
		// add extensions
		loadExtensions(pd.getDeclaredExtensions());
		// add extension points
		loadExtensionPoints(pd.getDeclaredExtensionPoints());
	}*/

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_EXTENSION_ORDER)) {
			swap((IPluginExtension) oldValue, (IPluginExtension) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IExtensions srcExtensions) {
		addArrayToVector(extensions, srcExtensions.getExtensions());
		addArrayToVector(extensionPoints, srcExtensions.getExtensionPoints());
	}

	protected void addArrayToVector(Vector vector, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			Object obj= array[i];
			if (obj instanceof PluginObject)
				((PluginObject) obj).setParent(this);
			vector.add(obj);
		}
	}

	protected void processChild(Node child, Hashtable lineTable) {
		String name = child.getNodeName().toLowerCase();
		if (name.equals("extension")) { //$NON-NLS-1$
			PluginExtension extension = new PluginExtension();
			extension.setModel(getModel());
			extension.setParent(this);
			extensions.add(extension);
			extension.setInTheModel(true);
			extension.load(child, lineTable);
		} else if (name.equals("extension-point")) { //$NON-NLS-1$
			PluginExtensionPoint point = new PluginExtensionPoint();
			point.setModel(getModel());
			point.setParent(this);
			point.setInTheModel(true);
			extensionPoints.add(point);
			point.load(child, lineTable);
		}
	}
	public void remove(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		extensions.removeElement(extension);
		((PluginExtension) extension).setInTheModel(false);
		fireStructureChanged(extension, ModelChangedEvent.REMOVE);
	}
	public void remove(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		ensureModelEditable();
		extensionPoints.removeElement(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(false);
		fireStructureChanged(extensionPoint, ModelChangedEvent.REMOVE);
	}

	public void reset() {
		extensions = new Vector();
		extensionPoints = new Vector();
	}

	public int getExtensionCount() {
		return extensions.size();
	}

	public int getIndexOf(IPluginExtension e) {
		return extensions.indexOf(e);
	}
	public void swap(IPluginExtension e1, IPluginExtension e2)
		throws CoreException {
		ensureModelEditable();
		int index1 = extensions.indexOf(e1);
		int index2 = extensions.indexOf(e2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECore.getResourceString("AbstractExtensions.extensionsNotFoundException")); //$NON-NLS-1$
		extensions.setElementAt(e1, index2);
		extensions.setElementAt(e2, index1);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}
	protected void writeChildren(
		String indent,
		String tag,
		Object[] children,
		PrintWriter writer) {
		writer.println(indent + "<" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.length; i++) {
			IPluginObject obj = (IPluginObject) children[i];
			obj.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "</" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean hasRequiredAttributes(){
		// validate extensions
		for (int i = 0; i < extensions.size(); i++) {
			IPluginExtension extension = (IPluginExtension)extensions.get(i);
			if (!extension.isValid()) return false;
		}
		// validate extension points
		for (int i = 0; i < extensionPoints.size(); i++) {
			IPluginExtensionPoint expoint = (IPluginExtensionPoint)extensionPoints.get(i);
			if (!expoint.isValid()) return false;
		}
		return true;
	}
}
