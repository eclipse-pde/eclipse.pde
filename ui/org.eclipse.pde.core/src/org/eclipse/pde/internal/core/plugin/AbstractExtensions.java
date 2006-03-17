/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.w3c.dom.Node;

public abstract class AbstractExtensions extends PluginObject implements IExtensions {
	
	protected String fSchemaVersion;
	
	protected ArrayList fExtensions = new ArrayList(1);
	protected ArrayList fExtensionPoints = new ArrayList(1);

	public void add(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		fExtensions.add(extension);
		((PluginExtension) extension).setInTheModel(true);
		((PluginExtension) extension).setParent(this);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}
	
	public void add(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		ensureModelEditable();
		fExtensionPoints.add(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(true);
		((PluginExtensionPoint) extensionPoint).setParent(this);
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
	}

	public IPluginExtensionPoint[] getExtensionPoints() {
		return (IPluginExtensionPoint[])fExtensionPoints.toArray(new IPluginExtensionPoint[fExtensionPoints.size()]);
	}
	
	public IPluginExtension[] getExtensions() {
		return (IPluginExtension[])fExtensions.toArray(new IPluginExtension[fExtensions.size()]);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_EXTENSION_ORDER)) {
			swap((IPluginExtension) oldValue, (IPluginExtension) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IExtensions srcExtensions) {
		addArrayToVector(fExtensions, srcExtensions.getExtensions());
		addArrayToVector(fExtensionPoints, srcExtensions.getExtensionPoints());
	}

	protected void addArrayToVector(ArrayList vector, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			Object obj= array[i];
			if (obj instanceof PluginObject)
				((PluginObject) obj).setParent(this);
			vector.add(obj);
		}
	}

	protected void processChild(Node child) {
		String name = child.getNodeName();
		if (name.equals("extension")) { //$NON-NLS-1$
			PluginExtension extension = new PluginExtension();
			extension.setModel(getModel());
			extension.setParent(this);
			fExtensions.add(extension);
			extension.setInTheModel(true);
			extension.load(child);
		} else if (name.equals("extension-point")) { //$NON-NLS-1$
			PluginExtensionPoint point = new PluginExtensionPoint();
			point.setModel(getModel());
			point.setParent(this);
			point.setInTheModel(true);
			fExtensionPoints.add(point);
			point.load(child);
		}
	}
	
	public void remove(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		fExtensions.remove(extension);
		((PluginExtension) extension).setInTheModel(false);
		fireStructureChanged(extension, IModelChangedEvent.REMOVE);
	}
	
	public void remove(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		ensureModelEditable();
		fExtensionPoints.remove(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(false);
		fireStructureChanged(extensionPoint, IModelChangedEvent.REMOVE);
	}

	public void reset() {
		fExtensions = new ArrayList();
		fExtensionPoints = new ArrayList();
	}

	public int getExtensionCount() {
		return fExtensions.size();
	}

	public int getIndexOf(IPluginExtension e) {
		return fExtensions.indexOf(e);
	}
	
	public void swap(IPluginExtension e1, IPluginExtension e2)
		throws CoreException {
		ensureModelEditable();
		int index1 = fExtensions.indexOf(e1);
		int index2 = fExtensions.indexOf(e2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECoreMessages.AbstractExtensions_extensionsNotFoundException); 
		fExtensions.set(index2, e1);
		fExtensions.set(index2, e2);
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
		for (int i = 0; i < fExtensions.size(); i++) {
			IPluginExtension extension = (IPluginExtension)fExtensions.get(i);
			if (!extension.isValid()) return false;
		}
		// validate extension points
		for (int i = 0; i < fExtensionPoints.size(); i++) {
			IPluginExtensionPoint expoint = (IPluginExtensionPoint)fExtensionPoints.get(i);
			if (!expoint.isValid()) return false;
		}
		return true;
	}
	
	public String getSchemaVersion() {
		return fSchemaVersion;
	}
	
	public void setSchemaVersion(String schemaVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = fSchemaVersion;
		fSchemaVersion = schemaVersion;
		firePropertyChanged(IPluginBase.P_SCHEMA_VERSION, oldValue, schemaVersion);
	}
	

}
