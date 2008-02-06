/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;

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
	private static final int VISIBILITY_INHERITED = 0;
	
	/**
	 * API component identifier of the API component that owns this
	 * description. All references within a component have no restrictions.
	 * We allow this to be null for testing purposes, but in general
	 * a component description should have a component id.
	 */
	private String fOwningComponentId = null;
	
	/**
	 * Represents a single node in the tree of mapped manifest items
	 */
	class ManifestNode implements Comparable {
		private IElementDescriptor element = null;
		private int visibility, restrictions;
		private ManifestNode parent = null;
		private HashMap overrides = new HashMap(),
						children = new HashMap();
		
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
			String vis = "Private"; //$NON-NLS-1$
			switch(visibility) {
				case VisibilityModifiers.API: {
					vis = "API"; //$NON-NLS-1$
					break;
				}
				case VisibilityModifiers.SPI: {
					vis = "SPI"; //$NON-NLS-1$
					break;
				}
				case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
					vis = "PRIVATE PERMISSIBLE"; //$NON-NLS-1$
					break;
				}
				case VISIBILITY_INHERITED: {
					vis = "INHERITED"; //$NON-NLS-1$
					break;
				}
			}
			String name = ""; //$NON-NLS-1$
			if (element instanceof IPackageDescriptor) {
				name = ((IPackageDescriptor) element).getName();
			} else {
				name = ((IMemberDescriptor) element).getName();
			}
			return "ManifestNode: " + name //$NON-NLS-1$
				+" with "+vis+" visibility\n\n"+ //$NON-NLS-1$ //$NON-NLS-2$
				(parent != null ? "Parent node: "+parent.toString() : ""); //$NON-NLS-1$ //$NON-NLS-2$
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
	}
		
	/**
	 * This is a map of component names to a map of package names to package node objects represented as:
	 * <pre>
	 * HashMap<IElementDescriptor(package), ManifestNode(package)>
	 * </pre>
	 */
	private HashMap fPackageMap = new HashMap();
	
	private boolean fContainsAnnotatedElements;

	/**
	 * Constructs an API description owned by the specified component.
	 * 
	 * @param owningComponentId API component identifier or <code>null</code> if there
	 * is no specific owner.
	 */
	public ApiDescription(String owningComponentId) {
		fOwningComponentId = owningComponentId;
		this.fContainsAnnotatedElements = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiManifest#visit(org.eclipse.pde.api.tools.ApiManifestVisitor)
	 */
	public void accept(ApiDescriptionVisitor visitor) {
		visitChildren(visitor, fPackageMap);
	}
	
	public boolean containsAnnotatedElements() {
		return this.fContainsAnnotatedElements;
	}
	/**
	 * Visits all children nodes in the given children map.
	 * 
	 * @param visitor visitor to visit
	 * @param childrenMap map of element name to manifest nodes
	 */
	private void visitChildren(ApiDescriptionVisitor visitor, Map childrenMap) {
		List elements = new ArrayList(childrenMap.keySet());
		Collections.sort(elements);
		Iterator iterator = elements.iterator();
		while (iterator.hasNext()) {
			IElementDescriptor element = (IElementDescriptor) iterator.next();
			ManifestNode node = (ManifestNode) childrenMap.get(element);
			visitNode(visitor, null, node);
		}
	}
	
	/**
	 * Visits a node and its children.
	 * 
	 * @param visitor visitor to visit
	 * @param component component context in which the node is being visited or <code>null</code> if none
	 * @param node node to visit
	 */
	private void visitNode(ApiDescriptionVisitor visitor, String component, ManifestNode node) {
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
		boolean visitChildren = visitor.visitElement(node.element, component, desc);
		if (visitChildren && !node.children.isEmpty()) {
			visitChildren(visitor, node.children);
		}
		visitor.endVisitElement(node.element, component, desc);
		if (!node.overrides.isEmpty()) {
			List overrides = new ArrayList(node.overrides.keySet());
			Collections.sort(overrides);
			Iterator iterator = overrides.iterator();
			while (iterator.hasNext()) {
				String context = (String) iterator.next();
				ManifestNode contextNode = (ManifestNode) node.overrides.get(context);
				visitNode(visitor, context, contextNode);
			}
		}		
	}
	
	/**
	 * Returns the node in the manifest for specified element and context, closest node, or <code>null</code>.
	 * Creates a new node with default visibility and no restrictions if insert is <code>true</code>
	 * and a node is not present. Default visibility for packages is API, and for types is inherited.
	 * 
	 * @param component component context (i.e. referenced from this component)
	 * @param element element
	 * @param insert whether to insert a new node
	 * @return manifest node or <code>null</code>
	 */
	private ManifestNode findNode(String component, IElementDescriptor element, boolean insert) {
		IElementDescriptor[] path = element.getPath();
		Map map = fPackageMap;
		int defaultVisibility = VisibilityModifiers.API;
		ManifestNode parentNode = null;
		ManifestNode node = null;
		for (int i = 0 ; i < path.length; i++) {
			IElementDescriptor current = path[i];
			parentNode = node;
			node = (ManifestNode) map.get(current);
			if (node == null) {
				if (insert) {
					node = new ManifestNode(parentNode, current, defaultVisibility, RestrictionModifiers.NO_RESTRICTIONS);
					map.put(current, node);
				} else {
					node = parentNode;
					break; // check for component override
				}
			}
			defaultVisibility = VISIBILITY_INHERITED;
			map = node.children;
		}
		if (component == null || node == null) {
			return node;
		}
		ManifestNode override = (ManifestNode) node.overrides.get(component);
		if (override == null) {
			if (insert) {
				override = new ManifestNode(parentNode, element, defaultVisibility, RestrictionModifiers.NO_RESTRICTIONS);
				node.overrides.put(component, override);
			} else {
				return node;
			}
		}
		return override;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#resolveAPIDescription(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor)
	 */
	public IApiAnnotations resolveAnnotations(String component, IElementDescriptor element) {
		if (component != null && fOwningComponentId != null) {
			if (fOwningComponentId.equals(component)) {
				return new ApiAnnotations(VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			}
		}
		ManifestNode node = findNode(component, element, false);
		if (node != null) {
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
		return null;
	}
	
	/**
	 * Internal hook to clear the package map to remove stale data
	 */
	protected void clearPackages() {
		if(fPackageMap != null) {
			fPackageMap.clear();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setRestrictions(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public void setRestrictions(String component, IElementDescriptor element, int restrictions) {
		ManifestNode node = findNode(component, element, true);
		if(node != null) {
			node.restrictions = restrictions;
			this.fContainsAnnotatedElements = true;
		}
		// TODO: if null > error
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setVisibility(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public void setVisibility(String component, IElementDescriptor element, int visibility) {
		ManifestNode node = findNode(component, element, true);
		if(node != null) {
			node.visibility = visibility;
		}
		// TODO: if null > error
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiDescription#removeElement(org.eclipse.pde.api.tools.descriptors.IElementDescriptor)
	 */
	public boolean removeElement(IElementDescriptor element) {
		ManifestNode node = findNode(null, element, false);
		if(node != null) {
			//packages have no parents
			if(node.parent == null) {
				return fPackageMap.remove(element) != null;
			}
			else {
				return node.parent.children.remove(element) != null;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Api description for component: "+fOwningComponentId; //$NON-NLS-1$
	}

}
