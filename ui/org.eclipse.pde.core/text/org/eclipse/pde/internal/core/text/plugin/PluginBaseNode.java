/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public abstract class PluginBaseNode extends PluginObjectNode implements IPluginBase {

	private static final long serialVersionUID = 1L;

	private String fSchemaVersion;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("runtime", true); //$NON-NLS-1$
		if (library instanceof PluginLibraryNode) {
			PluginLibraryNode node = (PluginLibraryNode) library;
			node.setModel(getModel());
			parent.addChildNode(node);
			fireStructureChanged(library, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("requires", true); //$NON-NLS-1$
		if (pluginImport instanceof PluginImportNode) {
			PluginImportNode node = (PluginImportNode) pluginImport;
			parent.addChildNode(node);
			fireStructureChanged(pluginImport, IModelChangedEvent.INSERT);
		}
	}

	public void add(IPluginImport[] pluginImports) {
		IDocumentElementNode parent = getEnclosingElement("requires", true); //$NON-NLS-1$
		for (int i = 0; i < pluginImports.length; i++) {
			if (pluginImports[i] != null && pluginImports[i] instanceof PluginImportNode) {
				PluginImportNode node = (PluginImportNode) pluginImports[i];
				parent.addChildNode(node);
			}
		}
		fireStructureChanged(pluginImports, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (parent != null) {
			parent.removeChildNode((IDocumentElementNode) pluginImport);
			pluginImport.setInTheModel(false);
			fireStructureChanged(pluginImport, IModelChangedEvent.REMOVE);
		}
	}

	public void remove(IPluginImport[] pluginImports) {
		IDocumentElementNode parent = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (parent != null) {
			for (int i = 0; i < pluginImports.length; i++) {
				parent.removeChildNode((IDocumentElementNode) pluginImports[i]);
				pluginImports[i].setInTheModel(false);
			}
			fireStructureChanged(pluginImports, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		ArrayList result = new ArrayList();
		IDocumentElementNode requiresNode = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (requiresNode != null) {
			IDocumentElementNode[] children = requiresNode.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPluginLibrary)
					result.add(children[i]);
			}
		}

		return (IPluginLibrary[]) result.toArray(new IPluginLibrary[result.size()]);
	}

	private IDocumentElementNode getEnclosingElement(String elementName, boolean create) {
		PluginElementNode element = null;
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginElement) {
				if (((PluginElementNode) children[i]).getXMLTagName().equals(elementName)) {
					element = (PluginElementNode) children[i];
					break;
				}
			}
		}
		if (element == null && create) {
			element = new PluginElementNode();
			element.setXMLTagName(elementName);
			element.setParentNode(this);
			element.setModel(getModel());
			element.setInTheModel(true);
			if (elementName.equals("runtime")) { //$NON-NLS-1$
				addChildNode(element, 0);
			} else if (elementName.equals("requires")) { //$NON-NLS-1$
				if (children.length > 0 && children[0].getXMLTagName().equals("runtime")) { //$NON-NLS-1$
					addChildNode(element, 1);
				} else {
					addChildNode(element, 0);
				}
			}
		}
		return element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		ArrayList result = new ArrayList();
		IDocumentElementNode requiresNode = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (requiresNode != null) {
			IDocumentElementNode[] children = requiresNode.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPluginImport)
					result.add(children[i]);
			}
		}

		return (IPluginImport[]) result.toArray(new IPluginImport[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		return getXMLAttributeValue(P_PROVIDER);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getVersion()
	 */
	public String getVersion() {
		return getXMLAttributeValue(P_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (parent != null) {
			parent.removeChildNode((IDocumentElementNode) library);
			library.setInTheModel(false);
			fireStructureChanged(library, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		setXMLAttribute(P_PROVIDER, providerName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		setXMLAttribute(P_VERSION, version);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary, org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
		IDocumentElementNode node = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (node != null) {
			node.swap((IDocumentElementNode) l1, (IDocumentElementNode) l2);
			firePropertyChanged(node, P_LIBRARY_ORDER, l1, l2);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getSchemaVersion()
	 */
	public String getSchemaVersion() {
		return fSchemaVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setSchemaVersion(java.lang.String)
	 */
	public void setSchemaVersion(String schemaVersion) throws CoreException {
		fSchemaVersion = schemaVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
		if (extension instanceof PluginExtensionNode) {
			PluginExtensionNode node = (PluginExtensionNode) extension;
			node.setModel(getModel());
			addChildNode(node);
			fireStructureChanged(extension, IModelChangedEvent.INSERT);
		}
	}

	/**
	 * @param extension
	 * @param position
	 * @throws CoreException
	 */
	public void add(IPluginExtension extension, int position) throws CoreException {
		// TODO: MP: DND: Make API?
		if ((extension instanceof PluginExtensionNode) == false) {
			return;
		} else if ((position < 0) || (position > getChildCount())) {
			return;
		}
		PluginExtensionNode node = (PluginExtensionNode) extension;
		node.setModel(getModel());
		addChildNode(node, position);
		fireStructureChanged(extension, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
		if (extensionPoint instanceof PluginExtensionPointNode) {
			PluginExtensionPointNode node = (PluginExtensionPointNode) extensionPoint;
			node.setModel(getModel());
			extensionPoint.setInTheModel(true);
			node.setParentNode(this);
			IPluginExtensionPoint[] extPoints = getExtensionPoints();
			if (extPoints.length > 0)
				addChildNode(node, indexOf((IDocumentElementNode) extPoints[extPoints.length - 1]) + 1);
			else {
				IDocumentElementNode requires = getEnclosingElement("requires", false); //$NON-NLS-1$
				if (requires != null) {
					addChildNode(node, indexOf(requires) + 1);
				} else {
					IDocumentElementNode runtime = getEnclosingElement("runtime", false); //$NON-NLS-1$
					if (runtime != null)
						addChildNode(node, indexOf(runtime) + 1);
					else
						addChildNode(node, 0);
				}
			}
			fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensionPoints()
	 */
	public IPluginExtensionPoint[] getExtensionPoints() {
		ArrayList result = new ArrayList();
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginExtensionPoint)
				result.add(children[i]);
		}
		return (IPluginExtensionPoint[]) result.toArray(new IPluginExtensionPoint[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensions()
	 */
	public IPluginExtension[] getExtensions() {
		ArrayList result = new ArrayList();
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginExtension)
				result.add(children[i]);
		}
		return (IPluginExtension[]) result.toArray(new IPluginExtension[result.size()]);
	}

	public int getIndexOf(IPluginExtension e) {
		IPluginExtension[] children = getExtensions();
		for (int i = 0; i < children.length; i++) {
			if (children[i].equals(e))
				return i;
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void remove(IPluginExtension extension) throws CoreException {
		if (extension instanceof IDocumentElementNode) {
			removeChildNode((IDocumentElementNode) extension);
			extension.setInTheModel(false);
			fireStructureChanged(extension, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint) throws CoreException {
		if (extensionPoint instanceof IDocumentElementNode) {
			removeChildNode((IDocumentElementNode) extensionPoint);
			extensionPoint.setInTheModel(false);
			fireStructureChanged(extensionPoint, IModelChangedEvent.REMOVE);
		}
	}

	public void remove(IPluginObject node) {
		if (node instanceof IDocumentElementNode) {
			removeChildNode((IDocumentElementNode) node);
			node.setInTheModel(false);
			fireStructureChanged(node, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension, org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException {
		swap((IDocumentElementNode) e1, (IDocumentElementNode) e2);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IPluginImport import1, IPluginImport import2) throws CoreException {
		IDocumentElementNode node = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (node != null) {
			node.swap((IDocumentElementNode) import1, (IDocumentElementNode) import2);
			firePropertyChanged(node, P_IMPORT_ORDER, import1, import2);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String newLine = getLineDelimiter();

		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine); //$NON-NLS-1$
		buffer.append("<?eclipse version=\"3.0\"?>" + newLine); //$NON-NLS-1$

		buffer.append(writeShallow(false) + newLine);

		IDocumentElementNode runtime = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (runtime != null) {
			runtime.setLineIndent(getLineIndent() + 3);
			buffer.append(runtime.write(true) + newLine);
		}

		IDocumentElementNode requires = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (requires != null) {
			requires.setLineIndent(getLineIndent() + 3);
			buffer.append(requires.write(true) + newLine);
		}

		IPluginExtensionPoint[] extPoints = getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			IDocumentElementNode extPoint = (IDocumentElementNode) extPoints[i];
			extPoint.setLineIndent(getLineIndent() + 3);
			buffer.append(extPoint.write(true) + newLine);
		}

		IPluginExtension[] extensions = getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IDocumentElementNode extension = (IDocumentElementNode) extensions[i];
			extension.setLineIndent(getLineIndent() + 3);
			buffer.append(extension.write(true) + newLine);
		}

		buffer.append("</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow()
	 */
	public String writeShallow(boolean terminate) {
		String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();
		buffer.append("<" + getXMLTagName()); //$NON-NLS-1$
		buffer.append(newLine);

		String id = getId();
		if (id != null && id.trim().length() > 0)
			buffer.append("   " + P_ID + "=\"" + getWritableString(id) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String name = getName();
		if (name != null && name.trim().length() > 0)
			buffer.append("   " + P_NAME + "=\"" + getWritableString(name) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String version = getVersion();
		if (version != null && version.trim().length() > 0)
			buffer.append("   " + P_VERSION + "=\"" + getWritableString(version) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String provider = getProviderName();
		if (provider != null && provider.trim().length() > 0) {
			buffer.append("   " + P_PROVIDER + "=\"" + getWritableString(provider) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String[] specific = getSpecificAttributes();
		for (int i = 0; i < specific.length; i++)
			buffer.append(newLine + specific[i]);
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$

		return buffer.toString();
	}

	protected abstract String[] getSpecificAttributes();

	public boolean isRoot() {
		return true;
	}
}
