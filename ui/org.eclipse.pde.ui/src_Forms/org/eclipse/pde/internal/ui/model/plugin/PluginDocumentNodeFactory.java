package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class PluginDocumentNodeFactory {
	
	private PluginModelBase fModel;

	public PluginDocumentNodeFactory(PluginModelBase model) {
		fModel = model;
	}
	
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {
		if (parent == null)
			return createPluginBase(name);
		
		if (parent instanceof PluginBaseNode) {
			if ("extension".equals(name))
				return createExtension(parent);
			if ("extension-point".equals(name))
				return createExtensionPoint(parent);
		} else {
			if (name.equals("import") && parent instanceof PluginElementNode) {
				if (((PluginElementNode)parent).getName().equals("requires")) {
					IDocumentNode ancestor = parent.getParentNode();
					if (ancestor != null && ancestor instanceof PluginBaseNode) {
						return createImport(parent);
					}
				}
			} else if (name.equals("library") && parent instanceof PluginElementNode) {
				if (((PluginElementNode)parent).getName().equals("runtime")) {
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
		return (PluginBaseNode)fModel.createPluginBase(name.equals("fragment"));
		
	}
}
