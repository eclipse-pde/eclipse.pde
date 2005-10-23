/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;

public class PluginDocumentNodeFactory implements IPluginModelFactory {
	
	private PluginModelBase fModel;

	public PluginDocumentNodeFactory(PluginModelBase model) {
		fModel = model;
	}
	
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {
		if (parent == null)
			return createPluginBase(name);
		
		if (parent instanceof PluginBaseNode) {
			if ("extension".equals(name)) //$NON-NLS-1$
				return createExtension(parent);
			if ("extension-point".equals(name)) //$NON-NLS-1$
				return createExtensionPoint(parent);
		} else {
			if (name.equals("import") && parent instanceof PluginElementNode) { //$NON-NLS-1$
				if (((PluginElementNode)parent).getName().equals("requires")) { //$NON-NLS-1$
					IDocumentNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof PluginBaseNode) {
						return createImport(parent);
					}
				}
			} else if (name.equals("library") && parent instanceof PluginElementNode) { //$NON-NLS-1$
				if (((PluginElementNode)parent).getName().equals("runtime")) { //$NON-NLS-1$
					IDocumentNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof PluginBaseNode) {
						return createLibrary(parent);
					}
				}				
			}
			
			
		}
		return createElement(name, parent);
	}
	
	/**
	 * @param parent
	 * @return
	 */
	private IDocumentNode createLibrary(IDocumentNode parent) {
		PluginLibraryNode node = new PluginLibraryNode();
		node.setParentNode(parent);
		node.setModel(fModel);
		node.setInTheModel(true);
		return node;
	}

	/**
	 * @param parent
	 */
	private IDocumentNode createImport(IDocumentNode parent) {
		PluginImportNode node = new PluginImportNode();
		node.setParentNode(parent);
		node.setModel(fModel);
		node.setInTheModel(true);
		return node;
	}

	/**
	 * @param name
	 * @param parent
	 * @return
	 */
	private IDocumentNode createElement(String name, IDocumentNode parent) {
		PluginElementNode node = new PluginElementNode();
		try {
			node.setName(name);
			node.setParentNode(parent);
			node.setModel(fModel);
			node.setInTheModel(true);
		} catch (CoreException e) {
		}
		return node;
	}

	/**
	 * @param name
	 * @param parent
	 * @return
	 */
	private PluginExtensionPointNode createExtensionPoint(IDocumentNode parent) {
		PluginExtensionPointNode node = new PluginExtensionPointNode();
		node.setParentNode(parent);
		node.setModel(fModel);
		node.setInTheModel(true);
		return node;
	}

	/**
	 * @param name
	 * @return
	 */
	private PluginExtensionNode createExtension(IDocumentNode parent) {
		PluginExtensionNode node = new PluginExtensionNode();
		node.setParentNode(parent);
		node.setModel(fModel);
		node.setInTheModel(true);
		return node;
	}

	public IDocumentAttribute createAttribute(String name, String value, IDocumentNode enclosingElement) {
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
		return (PluginBaseNode)fModel.createPluginBase(name.equals("fragment")); //$NON-NLS-1$
		
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
		node.setParentNode((IDocumentNode)parent);
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
}
