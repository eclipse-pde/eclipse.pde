/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
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
				case IElementDescriptor.FIELD: {
					type = "Field"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.METHOD: {
					type = "Method"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.PACKAGE: {
					type = "Package"; //$NON-NLS-1$
					name = ((IPackageDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.TYPE: {
					type = "Type"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(type == null ? "Unknown" : type).append(" Node: ").append(name == null ? "Unknown Name" : name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("\nVisibility: ").append(VisibilityModifiers.getVisibilityName(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(RestrictionModifiers.getRestrictionText(restrictions)); //$NON-NLS-1$
			if(parent != null) {
				String pname = parent.element.getElementType() == IElementDescriptor.PACKAGE ? 
						((IPackageDescriptor)parent.element).getName() : ((IMemberDescriptor)parent.element).getName();
				buffer.append("\nParent: ").append(pname); //$NON-NLS-1$
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
		 * Returns if the given node has API visibility. If the given node has {@link ApiDescription#VISIBILITY_INHERITED} visibility
		 * this method recursively asks its' parent nodes if they have API visibility.
		 * @param node
		 * @return true if this node has API visibility false otherwise
		 */
		protected boolean hasApiVisibility(ManifestNode node) {
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				System.out.println("Checking node for API visibility:"+node); //$NON-NLS-1$
			}
			if(node != null) {
				if(VisibilityModifiers.isAPI(node.visibility)) {
					return true;
				}
				else if(node.visibility == VISIBILITY_INHERITED) {
					return hasApiVisibility(node.parent);
				}
			}
			return false;
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
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				System.out.println("Refreshing manifest node: "+this); //$NON-NLS-1$
			}
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
			if(RestrictionModifiers.isUnrestricted(this.restrictions)) { 
				return;
			}
			switch (element.getElementType()) {
				case IElementDescriptor.METHOD: {
					IMethodDescriptor md = (IMethodDescriptor) element;
					Element method = document.createElement(IApiXmlConstants.ELEMENT_METHOD);
					method.setAttribute(IApiXmlConstants.ATTR_NAME, md.getName());
					method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, md.getSignature());
					persistAnnotations(method);
					parent.appendChild(method);
					break;
				}
				case IElementDescriptor.FIELD: {
					IFieldDescriptor fd = (IFieldDescriptor) element;
					Element field = document.createElement(IApiXmlConstants.ELEMENT_FIELD);
					field.setAttribute(IApiXmlConstants.ATTR_NAME, fd.getName());
					persistAnnotations(field);
					parent.appendChild(field);
					break;
				}
			}
		}
		
		/**
		 * Adds visibility and restrictions to the XML element.
		 * 
		 * @param element XML element to annotate
		 * @param component the component the description is for or <code>null</code>
		 */
		void persistAnnotations(Element element) {
			element.setAttribute(IApiXmlConstants.ATTR_VISIBILITY, Integer.toString(this.visibility));
			element.setAttribute(IApiXmlConstants.ATTR_RESTRICTIONS, Integer.toString(this.restrictions));
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
	 * This map holds the mapping of special access kinds for packages and has the 
	 * form:
	 * <pre>
	 * HashMap<IPackageDescriptor(package), HashMap<IElementDescriptor(component), IApiAccess>>
	 * </pre>
	 */
	protected HashMap fAccessMap = new HashMap();
	
	private float fEmbeddedVersion = 0.0f;
	
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
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#accept(org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void accept(ApiDescriptionVisitor visitor, IProgressMonitor monitor) {
		visitChildren(visitor, fPackageMap, monitor);
	}
	/**
	 * Visits all children nodes in the given children map.
	 * 
	 * @param visitor visitor to visit
	 * @param childrenMap map of element name to manifest nodes
	 * @param monitor
	 */
	protected void visitChildren(ApiDescriptionVisitor visitor, Map childrenMap, IProgressMonitor monitor) {
		List elements = new ArrayList(childrenMap.keySet());
		Collections.sort(elements);
		Iterator iterator = elements.iterator();
		while (iterator.hasNext()) {
			Util.updateMonitor(monitor);
			IElementDescriptor element = (IElementDescriptor) iterator.next();
			ManifestNode node = (ManifestNode) childrenMap.get(element);
			visitNode(visitor, node);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#accept(org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean accept(ApiDescriptionVisitor visitor, IElementDescriptor element, IProgressMonitor monitor) {
		ManifestNode node = findNode(element, false);
		if (node != null) {
			visitNode(visitor, node);
			return true;
		}
		return false;
	}
	
	/**
	 * Compares the given version against the embedded version that has been 
	 * read from the API description
	 * @param version
	 * @return returns the same values as a compareTo call:
	 * <ul>
	 * <li> -1 if the given version is less than the embedded version</li>
	 * <li> 0 if the given version is equal to the embedded version</li>
	 * <li> 1 if the given version is greater than the embedded version</li>
	 * </ul>
	 */
	public int compareEmbeddedVersionTo(String version) {
		float lversion = Float.parseFloat(version);
		if(fEmbeddedVersion < lversion) {
			return 1;
		}
		if(fEmbeddedVersion == lversion) {
			return 0;
		}
		return -1;
	}
	
	/**
	 * Allows the embedded version of this API description to be set. If the 
	 * given version string cannot be parsed to a valid version, the embedded version
	 * will default to the current version, as specified in {@link IApiXmlConstants#API_DESCRIPTION_CURRENT_VERSION}
	 * 
	 * @param version the version to set on this description
	 */
	public void setEmbeddedVersion(String version) {
		try {
			fEmbeddedVersion = Float.parseFloat(version);
		}
		catch(NumberFormatException nfe) {
			fEmbeddedVersion = Float.parseFloat(IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION);
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
			visitChildren(visitor, node.children, null);
		}
		visitor.endVisitElement(node.element, desc);
	}
	
	/**
	 * Returns the node in the manifest for specified element and context, closest node, or <code>null</code>.
	 * Creates a new node with default visibility and no restrictions if insert is <code>true</code>
	 * and a node is not present. Default visibility for packages is API, and for types is inherited.
	 * 
	 * @param element element
	 * @param write <code>true</code> if setting a node, <code>false</code> if getting a node
	 * @return manifest node or <code>null</code>
	 */
	protected ManifestNode findNode(IElementDescriptor element, boolean write) {
		if(ApiPlugin.DEBUG_API_DESCRIPTION) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Looking up manifest node for element: "); //$NON-NLS-1$
			buffer.append(element);
			System.out.println(buffer.toString());
		}
		IElementDescriptor[] path = element.getPath();
		Map map = fPackageMap;
		ManifestNode parentNode = null;
		ManifestNode node = null;
		for (int i = 0 ; i < path.length; i++) {
			IElementDescriptor current = path[i];
			parentNode = node;
			node = (ManifestNode) map.get(current);
			if (node == null) {
				if (write || (isInsertOnResolve(current))) {
					node = createNode(parentNode, current);
					if (node != null) {
						map.put(current, node);
					} else {
						return null;
					}
				} else {
					if(ApiPlugin.DEBUG_API_DESCRIPTION) {
						StringBuffer buffer = new StringBuffer();
						buffer.append("Returning parent manifest node: "); //$NON-NLS-1$
						buffer.append(parentNode);
						buffer.append(" when looking for element"); //$NON-NLS-1$
						buffer.append(element);
						System.out.println(buffer.toString()); 
					}
					return parentNode;
				}
			}
			node = node.refresh();
			if (node != null) {
				map = node.children;
			}
		}
		if(ApiPlugin.DEBUG_API_DESCRIPTION) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Manifest node found: "); //$NON-NLS-1$
			buffer.append(node);
			buffer.append(" when looking for element"); //$NON-NLS-1$
			buffer.append(element);
			System.out.println(buffer.toString());
		}
		return node;
	}
 	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#resolveAPIDescription(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor)
	 */
	public IApiAnnotations resolveAnnotations(IElementDescriptor element) {
		ManifestNode node = findNode(element, false);
		if (node != null) {
			return resolveAnnotations(node, element);
		}
		else if(ApiPlugin.DEBUG_API_DESCRIPTION){
			StringBuffer buffer = new StringBuffer();
			buffer.append("Tried to resolve annotations for manifest node: "); //$NON-NLS-1$
			buffer.append(node);
			buffer.append(" but the node could not be found."); //$NON-NLS-1$
			System.out.println(buffer.toString());
		}
		return null;
	}
	
	/**
	 * Returns the visibility of the given node, walking up the tree if needed
	 * to resolve inherited visibility.
	 * 
	 * @param node
	 * @return visibility modifier
	 */
	protected int resolveVisibility(ManifestNode node) {
		ManifestNode visNode = node;
		int vis = visNode.visibility;
		while (vis == VISIBILITY_INHERITED) {
			visNode = visNode.parent;
			vis = visNode.visibility;
		}
		return vis;
	}
	
	/**
	 * Resolves annotations based on inheritance for the given node and element.
	 * 
	 * @param node manifest node
	 * @param element the element annotations are being resolved for
	 * @return annotations
	 */
	protected IApiAnnotations resolveAnnotations(ManifestNode node, IElementDescriptor element) {
		int vis = resolveVisibility(node);
		int res = RestrictionModifiers.NO_RESTRICTIONS;
		if (node.element.equals(element)) {
			res = node.restrictions;
		}
		if(ApiPlugin.DEBUG_API_DESCRIPTION) {
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("Resolved annotations for manifest node: "); //$NON-NLS-1$
			stringBuffer.append(node);
			stringBuffer.append(" to be: "); //$NON-NLS-1$
			stringBuffer.append(VisibilityModifiers.getVisibilityName(vis));
			stringBuffer.append(" "); //$NON-NLS-1$
			stringBuffer.append(RestrictionModifiers.getRestrictionText(res));
			System.out.println(stringBuffer.toString());
		}
		return new ApiAnnotations(vis, res);
	}
	
	/**
	 * Internal hook to clear the package map to remove stale data
	 */
	protected void clearPackages() {
		if(fPackageMap != null) {
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				System.out.println("Clearing package map"); //$NON-NLS-1$
			}
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
		if (element.getElementType() == IElementDescriptor.PACKAGE) {
			vis = VisibilityModifiers.API;
		}
		if(ApiPlugin.DEBUG_API_DESCRIPTION) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Creating new manifest node for element: "); //$NON-NLS-1$
			buffer.append(element);
			buffer.append(" and adding it to parent node: "); //$NON-NLS-1$
			buffer.append(parentNode);
			System.out.println(buffer.toString());
		}
		return new ManifestNode(parentNode, element, vis, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setRestrictions(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public IStatus setRestrictions(IElementDescriptor element, int restrictions) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("Setting restrictions for manifest node: "); //$NON-NLS-1$
				buffer.append(node);
				buffer.append(" to be "); //$NON-NLS-1$
				buffer.append(RestrictionModifiers.getRestrictionText(restrictions));
				System.out.println(buffer.toString());
			}
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
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("Setting visibility for manifest node: "); //$NON-NLS-1$
				buffer.append(node);
				buffer.append(" to be "); //$NON-NLS-1$
				buffer.append(VisibilityModifiers.getVisibilityName(visibility));
				System.out.println(buffer.toString());
			}
			modified();
			node.visibility = visibility;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API visibility: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}
	
	public IStatus setAddedProfile(IElementDescriptor element, int addedProfile) {
		return Status.OK_STATUS;
	}
	public IStatus setRemovedProfile(IElementDescriptor element,
			int removedProfile) {
		return Status.OK_STATUS;
	}
	public IStatus setSuperclass(IElementDescriptor element, String superclass) {
		return Status.OK_STATUS;
	}
	public IStatus setSuperinterfaces(IElementDescriptor element,
			String superinterfaces) {
		return Status.OK_STATUS;
	}
	public IStatus setInterface(IElementDescriptor element,
			boolean interfaceFlag) {
		return Status.OK_STATUS;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("API description for component: ").append(fOwningComponentId); //$NON-NLS-1$
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#resolveAccessLevel(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor)
	 */
	public IApiAccess resolveAccessLevel(IElementDescriptor element, IPackageDescriptor pelement) {
		if(fAccessMap != null) {
			HashMap map = (HashMap) fAccessMap.get(pelement);
			if(map != null) {
				return (IApiAccess) map.get(element);
			}
		} 
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#setAccessLevel(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor, int)
	 */
	public void setAccessLevel(IElementDescriptor element, IPackageDescriptor pelement, int access) {
		if(element != null && pelement != null && access != IApiAccess.NORMAL) {
			if(fAccessMap == null) {
				fAccessMap = new HashMap();
			}
			HashMap map = (HashMap) fAccessMap.get(pelement);
			if(map == null) {
				map = new HashMap();
				fAccessMap.put(pelement, map);
			}
			map.put(element, new ApiAccess(access));
		}
	}
}
