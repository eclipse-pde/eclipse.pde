/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.icu.text.MessageFormat;

/**
 * Implementation of an API description.
 * <p>
 * Note, the implementation is not thread safe.
 * </p>
 * @see IApiDescription
 * @since 1.0.0
 */
public class ApiDescription implements IApiDescription {
	
	// flag to indicate visibility should be inherited from parent node
	protected static final int VISIBILITY_INHERITED = 0;
	
	/**
	 * Debug flag
	 */
	protected static final boolean DEBUG = Util.DEBUG;
	
	/**
	 * API component identifier of the API component that owns this
	 * description. All references within a component have no restrictions.
	 * We allow this to be null for testing purposes, but in general
	 * a component description should have a component id.
	 */
	protected String fOwningComponentId = null;
	
	/**
	 * Whether this description needs saving
	 */
	private boolean fModified = false;
	
	/**
	 * Represents a single node in the tree of mapped manifest items
	 */
	class ManifestNode implements Comparable {
		protected IElementDescriptor element = null;
		protected int visibility, restrictions;
		protected ManifestNode parent = null;
		protected HashMap children = new HashMap(1);
		
		public ManifestNode(ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			this.element = element;
			this.visibility = visibility;
			this.restrictions = restrictions;
			this.parent = parent;
		}
	
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public boolean equals(Object obj) {
			if(obj instanceof ManifestNode) {
				return ((ManifestNode)obj).element.equals(element);
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public int hashCode() {
			return element.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			String type = null;
			String name = null;
			switch(element.getElementType()) {
				case IElementDescriptor.T_FIELD: {
					type = "Field"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.T_METHOD: {
					type = "Method"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.T_PACKAGE: {
					type = "Package"; //$NON-NLS-1$
					name = ((IPackageDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.T_REFERENCE_TYPE: {
					type = "Type"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(type == null ? "Unknown" : type).append(" Node: ").append(name == null ? "Unknown Name" : name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("\nVisibility: ").append(Util.getVisibilityKind(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(Util.getRestrictionKind(restrictions)); //$NON-NLS-1$
			if(parent != null) {
				String pname = parent.element.getElementType() == IElementDescriptor.T_PACKAGE ? 
						((IPackageDescriptor)parent.element).getName() : ((IMemberDescriptor)parent.element).getName();
				buffer.append("\nParent: ").append(parent == null ? null : pname); //$NON-NLS-1$
			}
			return buffer.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof ManifestNode) {
				ManifestNode node = (ManifestNode) o;
				return ((ElementDescriptorImpl)element).compareTo(node.element);
			}
			return -1;
		}
		
		/**
		 * Ensure this node is up to date. Default implementation does
		 * nothing. Subclasses should override as required.
		 * 
		 * Returns the resulting node if the node is valid, or <code>null</code>
		 * if the node no longer exists.
		 * 
		 * @return up to date node, or <code>null</code> if no longer exists
		 */
		protected ManifestNode refresh() {
			return this;
		}
		
		/**
		 * Persists this node as a child of the given element.
		 * 
		 * @param document XML document
		 * @param parent parent element in the document
		 * @param component component the description is for or <code>null</code>
		 */
		void persistXML(Document document, Element parent) {
			if(restrictions == RestrictionModifiers.NO_RESTRICTIONS) { 
				return;
			}
			switch (element.getElementType()) {
			case IElementDescriptor.T_METHOD:
				IMethodDescriptor md = (IMethodDescriptor) element;
				Element method = document.createElement(IApiXmlConstants.ELEMENT_METHOD);
				method.setAttribute(IApiXmlConstants.ATTR_NAME, md.getName());
				method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, md.getSignature());
				persistAnnotations(method);
				parent.appendChild(method);
				break;
			case IElementDescriptor.T_FIELD:
				IFieldDescriptor fd = (IFieldDescriptor) element;
				Element field = document.createElement(IApiXmlConstants.ELEMENT_FIELD);
				field.setAttribute(IApiXmlConstants.ATTR_NAME, fd.getName());
				persistAnnotations(field);
				parent.appendChild(field);
				break;
			}
		}
		
		/**
		 * Adds visibility and restrictions to the XML element.
		 * 
		 * @param element XML element to annotate
		 * @param component the component the description is for or <code>null</code>
		 */
		void persistAnnotations(Element element) {
			element.setAttribute(IApiXmlConstants.ATTR_VISIBILITY, Integer.toString(visibility));
			element.setAttribute(IApiXmlConstants.ATTR_RESTRICTIONS, Integer.toString(restrictions));
		}
	}
		
	/**
	 * This is a map of component names to a map of package names to package node objects represented as:
	 * <pre>
	 * HashMap<IElementDescriptor(package), ManifestNode(package)>
	 * </pre>
	 */
	protected HashMap fPackageMap = new HashMap();
	
	/**
	 * Map of package visibility overrides per component id.
	 */
	protected HashMap fOverrides = new HashMap();

	/**
	 * Constructs an API description owned by the specified component.
	 * 
	 * @param owningComponentId API component identifier or <code>null</code> if there
	 * is no specific owner.
	 */
	public ApiDescription(String owningComponentId) {
		fOwningComponentId = owningComponentId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiManifest#visit(org.eclipse.pde.api.tools.ApiManifestVisitor)
	 */
	public void accept(ApiDescriptionVisitor visitor) {
		visitChildren(visitor, fPackageMap);
	}
	/**
	 * Visits all children nodes in the given children map.
	 * 
	 * @param visitor visitor to visit
	 * @param childrenMap map of element name to manifest nodes
	 */
	protected void visitChildren(ApiDescriptionVisitor visitor, Map childrenMap) {
		List elements = new ArrayList(childrenMap.keySet());
		Collections.sort(elements);
		Iterator iterator = elements.iterator();
		while (iterator.hasNext()) {
			IElementDescriptor element = (IElementDescriptor) iterator.next();
			ManifestNode node = (ManifestNode) childrenMap.get(element);
			visitNode(visitor, node);
		}
	}
	
	/**
	 * Visits a node and its children.
	 * 
	 * @param visitor visitor to visit
	 * @param node node to visit
	 */
	private void visitNode(ApiDescriptionVisitor visitor, ManifestNode node) {
		int vis = node.visibility;
		ManifestNode tmp = node;
		while (tmp != null) {
			vis = tmp.visibility;
			if(tmp.visibility == VISIBILITY_INHERITED) {
				tmp = tmp.parent;
			}
			else {
				tmp = null;
			}
		}
		IApiAnnotations desc = new ApiAnnotations(vis, node.restrictions);
		boolean visitChildren = visitor.visitElement(node.element, desc);
		if (visitChildren && !node.children.isEmpty()) {
			visitChildren(visitor, node.children);
		}
		visitor.endVisitElement(node.element, desc);	
	}
	
	/**
	 * Returns the node in the manifest for specified element and context, closest node, or <code>null</code>.
	 * Creates a new node with default visibility and no restrictions if insert is <code>true</code>
	 * and a node is not present. Default visibility for packages is API, and for types is inherited.
	 * 
	 * @param element element
	 * @param insert whether to insert a new node
	 * @return manifest node or <code>null</code>
	 */
	protected ManifestNode findNode(IElementDescriptor element, boolean insert) {
		IElementDescriptor[] path = element.getPath();
		Map map = fPackageMap;
		ManifestNode parentNode = null;
		ManifestNode node = null;
		for (int i = 0 ; i < path.length; i++) {
			IElementDescriptor current = path[i];
			parentNode = node;
			node = (ManifestNode) map.get(current);
			if (node == null) {
				if (insert) {
					node = createNode(parentNode, current);
					if (node != null) {
						map.put(current, node);
					} else {
						return null;
					}
				} else {
					return parentNode;
				}
			}
			node = node.refresh();
			if (node != null) {
				map = node.children;
			}
		}
		return node;
	}
 	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#resolveAPIDescription(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor)
	 */
	public IApiAnnotations resolveAnnotations(IElementDescriptor element) {
		ManifestNode node = findNode(element, isInsertOnResolve(element));
		if (node != null) {
			return resolveAnnotations(node, element);
		}
		return null;
	}
	
	/**
	 * Resolves annotations based on inheritance for the given node and element.
	 * 
	 * @param node manifest node
	 * @param element the element annotations are being resolved for
	 * @return annotations
	 */
	protected IApiAnnotations resolveAnnotations(ManifestNode node, IElementDescriptor element) {
		ManifestNode visNode = node;
		int vis = visNode.visibility;
		while (vis == VISIBILITY_INHERITED) {
			visNode = visNode.parent;
			vis = visNode.visibility;
		}
		int res = RestrictionModifiers.NO_RESTRICTIONS;
		if (node.element.equals(element)) {
			res = node.restrictions;
		}
		return new ApiAnnotations(vis, res);
	}
	
	/**
	 * Internal hook to clear the package map to remove stale data
	 */
	protected void clearPackages() {
		if(fPackageMap != null) {
			fPackageMap.clear();
		}
	}
	
	/**
	 * Creates and returns a new manifest node to be inserted into the tree
	 * or <code>null</code> if the node does not exist.
	 * 
	 * <p>
	 * Subclasses should override this method as required.
	 * </p>
	 * @param parentNode parent node
	 * @param element element the node is to be created for
	 * @return new manifest node or <code>null</code> if none
	 */
	protected ManifestNode createNode(ManifestNode parentNode, IElementDescriptor element) {
		int vis = VISIBILITY_INHERITED;
		if (element.getElementType() == IElementDescriptor.T_PACKAGE) {
			vis = VisibilityModifiers.API;
		}
		return new ManifestNode(parentNode, element, vis, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setRestrictions(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public IStatus setRestrictions(IElementDescriptor element, int restrictions) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.restrictions = restrictions;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API restriction: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setVisibility(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public IStatus setVisibility(IElementDescriptor element, int visibility) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.visibility = visibility;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API visibility: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api description for component: ").append(fOwningComponentId); //$NON-NLS-1$
		return buffer.toString();
	}

	/**
	 * Returns whether a new node should be inserted into the API description
	 * when resolving the annotations for an element if a node is not already
	 * present, in the context of the given component.
	 * <p>
	 * Default implementation returns <code>false</code>. Subclasses should
	 * override this method as required.
	 * </p>
	 * @param elementDescriptor
	 * @return whether a new node should be inserted into the API description
	 * when resolving the annotations for an element if a node is not already
	 * present
	 */
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		return false;
	}
	
	/**
	 * Marks the description as modified
	 */
	protected synchronized void modified() {
		fModified = true;
	}
	
	/**
	 * Returns whether this description has been modified.
	 * 
	 * @return
	 */
	protected synchronized boolean isModified() {
		return fModified;
	}
}
