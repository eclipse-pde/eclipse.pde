/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public abstract class PluginBaseNode extends PluginObjectNode implements IPluginBase {

	private static final long serialVersionUID = 1L;

	private String fSchemaVersion;

	@Override
	public void add(IPluginLibrary library) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("runtime", true); //$NON-NLS-1$
		if (library instanceof PluginLibraryNode) {
			PluginLibraryNode node = (PluginLibraryNode) library;
			node.setModel(getModel());
			parent.addChildNode(node);
			fireStructureChanged(library, IModelChangedEvent.INSERT);
		}
	}

	@Override
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
		for (IPluginImport pluginImport : pluginImports) {
			if (pluginImport != null && pluginImport instanceof PluginImportNode) {
				PluginImportNode node = (PluginImportNode) pluginImport;
				parent.addChildNode(node);
			}
		}
		fireStructureChanged(pluginImports, IModelChangedEvent.INSERT);
	}

	@Override
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
			for (IPluginImport pluginImport : pluginImports) {
				parent.removeChildNode((IDocumentElementNode) pluginImport);
				pluginImport.setInTheModel(false);
			}
			fireStructureChanged(pluginImports, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public IPluginLibrary[] getLibraries() {
		ArrayList<IDocumentElementNode> result = new ArrayList<>();
		IDocumentElementNode requiresNode = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (requiresNode != null) {
			IDocumentElementNode[] children = requiresNode.getChildNodes();
			for (IDocumentElementNode childNode : children) {
				if (childNode instanceof IPluginLibrary) {
					result.add(childNode);
				}
			}
		}

		return result.toArray(new IPluginLibrary[result.size()]);
	}

	private IDocumentElementNode getEnclosingElement(String elementName, boolean create) {
		PluginElementNode element = null;
		IDocumentElementNode[] children = getChildNodes();
		for (IDocumentElementNode childNode : children) {
			if (childNode instanceof IPluginElement) {
				if (((PluginElementNode) childNode).getXMLTagName().equals(elementName)) {
					element = (PluginElementNode) childNode;
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

	@Override
	public IPluginImport[] getImports() {
		ArrayList<IDocumentElementNode> result = new ArrayList<>();
		IDocumentElementNode requiresNode = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (requiresNode != null) {
			IDocumentElementNode[] children = requiresNode.getChildNodes();
			for (IDocumentElementNode childNode : children) {
				if (childNode instanceof IPluginImport) {
					result.add(childNode);
				}
			}
		}

		return result.toArray(new IPluginImport[result.size()]);
	}

	@Override
	public String getProviderName() {
		return getXMLAttributeValue(P_PROVIDER);
	}

	@Override
	public String getVersion() {
		return getXMLAttributeValue(P_VERSION);
	}

	@Override
	public void remove(IPluginLibrary library) throws CoreException {
		IDocumentElementNode parent = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (parent != null) {
			parent.removeChildNode((IDocumentElementNode) library);
			library.setInTheModel(false);
			fireStructureChanged(library, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public void setProviderName(String providerName) throws CoreException {
		setXMLAttribute(P_PROVIDER, providerName);
	}

	@Override
	public void setVersion(String version) throws CoreException {
		setXMLAttribute(P_VERSION, version);
	}

	@Override
	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
		IDocumentElementNode node = getEnclosingElement("runtime", false); //$NON-NLS-1$
		if (node != null) {
			node.swap((IDocumentElementNode) l1, (IDocumentElementNode) l2);
			firePropertyChanged(node, P_LIBRARY_ORDER, l1, l2);
		}
	}

	@Override
	public String getSchemaVersion() {
		return fSchemaVersion;
	}

	@Override
	public void setSchemaVersion(String schemaVersion) throws CoreException {
		fSchemaVersion = schemaVersion;
	}

	@Override
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

	@Override
	public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
		if (extensionPoint instanceof PluginExtensionPointNode) {
			PluginExtensionPointNode node = (PluginExtensionPointNode) extensionPoint;
			node.setModel(getModel());
			extensionPoint.setInTheModel(true);
			node.setParentNode(this);
			IPluginExtensionPoint[] extPoints = getExtensionPoints();
			if (extPoints.length > 0) {
				addChildNode(node, indexOf((IDocumentElementNode) extPoints[extPoints.length - 1]) + 1);
			} else {
				IDocumentElementNode requires = getEnclosingElement("requires", false); //$NON-NLS-1$
				if (requires != null) {
					addChildNode(node, indexOf(requires) + 1);
				} else {
					IDocumentElementNode runtime = getEnclosingElement("runtime", false); //$NON-NLS-1$
					if (runtime != null) {
						addChildNode(node, indexOf(runtime) + 1);
					} else {
						addChildNode(node, 0);
					}
				}
			}
			fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public IPluginExtensionPoint[] getExtensionPoints() {
		ArrayList<IDocumentElementNode> result = new ArrayList<>();
		IDocumentElementNode[] children = getChildNodes();
		for (IDocumentElementNode childNode : children) {
			if (childNode instanceof IPluginExtensionPoint) {
				result.add(childNode);
			}
		}
		return result.toArray(new IPluginExtensionPoint[result.size()]);
	}

	@Override
	public IPluginExtension[] getExtensions() {
		ArrayList<IDocumentElementNode> result = new ArrayList<>();
		IDocumentElementNode[] children = getChildNodes();
		for (IDocumentElementNode childNode : children) {
			if (childNode instanceof IPluginExtension) {
				result.add(childNode);
			}
		}
		return result.toArray(new IPluginExtension[result.size()]);
	}

	@Override
	public int getIndexOf(IPluginExtension e) {
		IPluginExtension[] children = getExtensions();
		for (int i = 0; i < children.length; i++) {
			if (children[i].equals(e)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void remove(IPluginExtension extension) throws CoreException {
		if (extension instanceof IDocumentElementNode) {
			removeChildNode((IDocumentElementNode) extension);
			extension.setInTheModel(false);
			fireStructureChanged(extension, IModelChangedEvent.REMOVE);
		}
	}

	@Override
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

	@Override
	public void swap(IPluginExtension e1, IPluginExtension e2) throws CoreException {
		swap((IDocumentElementNode) e1, (IDocumentElementNode) e2);
		firePropertyChanged(this, P_EXTENSION_ORDER, e1, e2);
	}

	@Override
	public void swap(IPluginImport import1, IPluginImport import2) throws CoreException {
		IDocumentElementNode node = getEnclosingElement("requires", false); //$NON-NLS-1$
		if (node != null) {
			node.swap((IDocumentElementNode) import1, (IDocumentElementNode) import2);
			firePropertyChanged(node, P_IMPORT_ORDER, import1, import2);
		}
	}

	@Override
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}

	@Override
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}

	@Override
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}

	@Override
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}

	@Override
	public String write(boolean indent) {
		String newLine = getLineDelimiter();

		StringBuilder buffer = new StringBuilder();
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
		for (IPluginExtensionPoint extPoint : extPoints) {
			IDocumentElementNode extPointNode = (IDocumentElementNode) extPoint;
			extPointNode.setLineIndent(getLineIndent() + 3);
			buffer.append(extPointNode.write(true) + newLine);
		}

		IPluginExtension[] extensions = getExtensions();
		for (IPluginExtension extension : extensions) {
			IDocumentElementNode extensionNode = (IDocumentElementNode) extension;
			extensionNode.setLineIndent(getLineIndent() + 3);
			buffer.append(extensionNode.write(true) + newLine);
		}

		buffer.append("</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}

	@Override
	public String writeShallow(boolean terminate) {
		String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuilder buffer = new StringBuilder();
		buffer.append("<" + getXMLTagName()); //$NON-NLS-1$
		buffer.append(newLine);

		String id = getId();
		if (id != null && id.trim().length() > 0) {
			buffer.append("   " + P_ID + "=\"" + getWritableString(id) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String name = getName();
		if (name != null && name.trim().length() > 0) {
			buffer.append("   " + P_NAME + "=\"" + getWritableString(name) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String version = getVersion();
		if (version != null && version.trim().length() > 0) {
			buffer.append("   " + P_VERSION + "=\"" + getWritableString(version) + "\"" + newLine); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String provider = getProviderName();
		if (provider != null && provider.trim().length() > 0) {
			buffer.append("   " + P_PROVIDER + "=\"" + getWritableString(provider) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String[] specific = getSpecificAttributes();
		for (String element : specific) {
			buffer.append(newLine + element);
		}
		if (terminate) {
			buffer.append("/"); //$NON-NLS-1$
		}
		buffer.append(">"); //$NON-NLS-1$

		return buffer.toString();
	}

	protected abstract String[] getSpecificAttributes();

	@Override
	public boolean isRoot() {
		return true;
	}
}
