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
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

public class PluginDocumentNodeFactory implements IPluginModelFactory, IDocumentNodeFactory {

	private PluginModelBase fModel;

	public PluginDocumentNodeFactory(PluginModelBase model) {
		fModel = model;
	}

	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent) {
		if (parent == null)
			return createPluginBase(name);

		if (parent instanceof PluginBaseNode) {
			if ("extension".equals(name)) //$NON-NLS-1$
				return (IDocumentElementNode) createExtension();
			if ("extension-point".equals(name)) //$NON-NLS-1$
				return (IDocumentElementNode) createExtensionPoint();
		} else {
			if (name.equals("import") && parent instanceof PluginElementNode) { //$NON-NLS-1$
				if (((PluginElementNode) parent).getName().equals("requires")) { //$NON-NLS-1$
					IDocumentElementNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof PluginBaseNode) {
						return (IDocumentElementNode) createImport();
					}
				}
			} else if (name.equals("library") && parent instanceof PluginElementNode) { //$NON-NLS-1$
				if (((PluginElementNode) parent).getName().equals("runtime")) { //$NON-NLS-1$
					IDocumentElementNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof PluginBaseNode) {
						return (IDocumentElementNode) createLibrary();
					}
				}
			}
		}
		IDocumentElementNode node = (IDocumentElementNode) createElement((IPluginObject) parent);
		node.setXMLTagName(name);
		return node;
	}

	public IDocumentAttributeNode createAttribute(String name, String value, IDocumentElementNode enclosingElement) {
		PluginAttribute attribute = new PluginAttribute();
		try {
			attribute.setName(name);
			attribute.setValue(value);
		} catch (CoreException e) {
		}
		attribute.setEnclosingElement(enclosingElement);
		attribute.setModel(fModel);
		attribute.setInTheModel(true);
		return attribute;
	}

	private PluginBaseNode createPluginBase(String name) {
		return (PluginBaseNode) fModel.createPluginBase(name.equals("fragment")); //$NON-NLS-1$

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelFactory#createImport()
	 */
	public IPluginImport createImport() {
		PluginImportNode node = new PluginImportNode();
		node.setModel(fModel);
		node.setXMLTagName("import"); //$NON-NLS-1$
		return node;
	}

	public IPluginImport createImport(String pluginId) {
		PluginImportNode node = new PluginImportNode(pluginId);
		node.setModel(fModel);
		node.setXMLTagName("import"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelFactory#createLibrary()
	 */
	public IPluginLibrary createLibrary() {
		PluginLibraryNode node = new PluginLibraryNode();
		node.setModel(fModel);
		node.setXMLTagName("library"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createAttribute(org.eclipse.pde.core.plugin.IPluginElement)
	 */
	public IPluginAttribute createAttribute(IPluginElement element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createElement(org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public IPluginElement createElement(IPluginObject parent) {
		PluginElementNode node = new PluginElementNode();
		node.setModel(fModel);
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createExtension()
	 */
	public IPluginExtension createExtension() {
		PluginExtensionNode node = new PluginExtensionNode();
		node.setModel(fModel);
		node.setXMLTagName("extension"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensionsModelFactory#createExtensionPoint()
	 */
	public IPluginExtensionPoint createExtensionPoint() {
		PluginExtensionPointNode node = new PluginExtensionPointNode();
		node.setModel(fModel);
		node.setXMLTagName("extension-point"); //$NON-NLS-1$
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentTextNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentTextNode createDocumentTextNode(String content, IDocumentElementNode parent) {
		DocumentTextNode textNode = new DocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content.trim());
		return textNode;
	}
}
