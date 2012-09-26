/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class AbstractExtensions extends PluginObject implements IExtensions {

	private static final long serialVersionUID = 1L;

	protected String fSchemaVersion;

	protected List<IPluginExtension> fExtensions = null;
	protected List<IPluginExtensionPoint> fExtensionPoints = null;
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
		List<IPluginExtensionPoint> extPoints = getExtensionPointsList();
		return extPoints.toArray(new IPluginExtensionPoint[extPoints.size()]);
	}

	public IPluginExtension[] getExtensions() {
		List<IPluginExtension> extensions = getExtensionsList();
		return extensions.toArray(new IPluginExtension[extensions.size()]);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_EXTENSION_ORDER)) {
			swap((IPluginExtension) oldValue, (IPluginExtension) newValue);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	public void load(IExtensions srcExtensions) {
		List<IPluginExtension> extensionsList = getExtensionsList();
		IPluginExtension[] extensions = srcExtensions.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i] != null) {
				extensionsList.add(extensions[i]);
			}
		}

		List<IPluginExtensionPoint> extensionPointsList = getExtensionPointsList();
		IPluginExtensionPoint[] extensionPoints = srcExtensions.getExtensionPoints();
		for (int i = 0; i < extensionPoints.length; i++) {
			if (extensionPoints[i] != null) {
				extensionPointsList.add(extensionPoints[i]);
			}
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
		List<IPluginExtension> extensions = getExtensionsList();
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
		List<IPluginExtension> extensions = getExtensionsList();
		int size = extensions.size();
		for (int i = 0; i < size; i++) {
			IPluginExtension extension = extensions.get(i);
			if (!extension.isValid())
				return false;
		}
		// validate extension points
		List<IPluginExtensionPoint> extPoints = getExtensionPointsList();
		size = extPoints.size();
		for (int i = 0; i < size; i++) {
			IPluginExtensionPoint expoint = extPoints.get(i);
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

	protected List<IPluginExtension> getExtensionsList() {
		if (fExtensions == null) {
			IPluginBase base = getPluginBase();
			if (base != null) {
				if (fCache)
					fExtensions = new ArrayList<IPluginExtension>(Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getPluginModel())));
				else
					return Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionsForPlugin(base.getPluginModel()));
			} else {
				return Collections.emptyList();
			}
		}
		return fExtensions;
	}

	protected List<IPluginExtensionPoint> getExtensionPointsList() {
		if (fExtensionPoints == null) {
			IPluginBase base = getPluginBase();
			if (base != null) {
				if (fCache)
					fExtensionPoints = new ArrayList<IPluginExtensionPoint>(Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getPluginModel())));
				else
					return Arrays.asList(PDECore.getDefault().getExtensionsRegistry().findExtensionPointsForPlugin(base.getPluginModel()));
			} else {
				return Collections.emptyList();
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
			fExtensions = new ArrayList<IPluginExtension>();
		if (fExtensionPoints == null)
			fExtensionPoints = new ArrayList<IPluginExtensionPoint>();

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
