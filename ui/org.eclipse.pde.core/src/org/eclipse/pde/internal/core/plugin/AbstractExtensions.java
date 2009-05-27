/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class AbstractExtensions extends PluginObject implements IExtensions {

	private static final long serialVersionUID = 1L;

	protected String fSchemaVersion;

	protected List fExtensions = null;
	protected List fExtensionPoints = null;
	boolean fCache = false;

	public AbstractExtensions(boolean readOnly) {
		fCache = !readOnly;
	}

	public void add(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		getExtensionsList().add(extension);
		((PluginExtension) extension).setInTheModel(true);
		((PluginExtension) extension).setParent(this);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}

	public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		getExtensionPointsList().add(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(true);
		((PluginExtensionPoint) extensionPoint).setParent(this);
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
	}

	public IPluginExtensionPoint[] getExtensionPoints() {
		List extPoints = getExtensionPointsList();
		return (IPluginExtensionPoint[]) extPoints.toArray(new IPluginExtensionPoint[extPoints.size()]);
	}

	public IPluginExtension[] getExtensions() {
		List extensions = getExtensionsList();
		return (IPluginExtension[]) extensions.toArray(new IPluginExtension[extensions.size()]);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_EXTENSION_ORDER)) {
			swap((IPluginExtension) oldValue, (IPluginExtension) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IExtensions srcExtensions) {
		addArrayToVector(getExtensionsList(), srcExtensions.getExtensions());
		addArrayToVector(getExtensionPointsList(), srcExtensions.getExtensionPoints());
	}

	protected void addArrayToVector(List vector, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			Object obj = array[i];
			if (obj instanceof PluginObject)
				((PluginObject) obj).setParent(this);
			vector.add(obj);
		}
	}

	public void remove(IPluginExtension extension) throws CoreException {
		ensureModelEditable();
		getExtensionsList().remove(extension);
		((PluginExtension) extension).setInTheModel(false);
		fireStructureChanged(extension, IModelChangedEvent.REMOVE);
	}

	public void remove(IPluginExtensionPoint extensionPoint) throws CoreException {
		ensureModelEditable();
		getExtensionPointsList().remove(extensionPoint);
		((PluginExtensionPoint) extensionPoint).setInTheModel(false);
		fireStructureChanged(extensionPoint, IModelChangedEvent.REMOVE);
	}

	public void reset() {
		resetExtensions();
	}

	public void resetExtensions() {
		fExtensions = null;
		fExtensionPoints = null;
	}

	public int getExtensionCount() {
		return getExtensionsList().size();
	}

	public int getIndexOf(IPluginExtension e) {
		return getExtensionsList().indexOf(e);
	}

	public void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException {
		ensureModelEditable();
		List extensions = getExtensionsList();
		int index1 = extensions.indexOf(e1);
		int index2 = extensions.indexOf(e2);
		if (index1 == -1 || index2 == -1)
			throwCoreException(PDECoreMessages.AbstractExtensions_extensionsNotFoundException);
		extensions.set(index2, e1);
		extensions.set(index2, e2);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}

	protected void writeChildren(String indent, String tag, Object[] children, PrintWriter writer) {
		writer.println(indent + "<" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < children.length; i++) {
			IPluginObject obj = (IPluginObject) children[i];
			obj.write(indent + "   ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "</" + tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean hasRequiredAttributes() {
		// validate extensions
		List extensions = getExtensionsList();
		int size = extensions.size();
		for (int i = 0; i < size; i++) {
			IPluginExtension extension = (IPluginExtension) extensions.get(i);
			if (!extension.isValid())
				return false;
		}
		// validate extension points
		List extPoints = getExtensionPointsList();
		size = extPoints.size();
		for (int i = 0; i < size; i++) {
			IPluginExtensionPoint expoint = (IPluginExtensionPoint) extPoints.get(i);
			if (!expoint.isValid())
				return false;
		}
		return true;
	}

	public String getSchemaVersion() {
		if (fSchemaVersion == null) {
			// since schema version is only needed on workspace models in very few situations, reading information from the file should suffice
			ISharedPluginModel model = getModel();
			if (model != null) {
				org.eclipse.core.resources.IResource res = model.getUnderlyingResource();
				if (res != null && res instanceof IFile) {
					try {
						InputStream stream = new BufferedInputStream(((IFile) res).getContents(true));
						PluginHandler handler = new PluginHandler(true);
						SAXParserFactory.newInstance().newSAXParser().parse(stream, handler);
						return handler.getSchemaVersion();
					} catch (CoreException e) {
					} catch (SAXException e) {
					} catch (IOException e) {
					} catch (ParserConfigurationException e) {
					}
				}
			}
		}
		return fSchemaVersion;
	}

	public void setSchemaVersion(String schemaVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = fSchemaVersion;
		fSchemaVersion = schemaVersion;
		firePropertyChanged(IPluginBase.P_SCHEMA_VERSION, oldValue, schemaVersion);
	}

	protected List getExtensionsList() {
		if (fExtensions == null) {
			IPluginBase base = getPluginBase();
			if (base != null) {
				if (fCache)
					fExtensions = new ArrayList(Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getPluginModel())));
				else
					return Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getPluginModel()));
			} else {
				return Collections.EMPTY_LIST;
			}
		}
		return fExtensions;
	}

	protected List getExtensionPointsList() {
		if (fExtensionPoints == null) {
			IPluginBase base = getPluginBase();
			if (base != null) {
				if (fCache)
					fExtensionPoints = new ArrayList(Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getPluginModel())));
				else
					return Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getPluginModel()));
			} else {
				return Collections.EMPTY_LIST;
			}
		}
		return fExtensionPoints;
	}

	/*
	 * If this function is used to load the model, the extension registry cache will not be used when querying model.
	 */
	protected void processChild(Node child) {
		String name = child.getNodeName();
		if (fExtensions == null)
			fExtensions = new ArrayList();
		if (fExtensionPoints == null)
			fExtensionPoints = new ArrayList();

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
}
