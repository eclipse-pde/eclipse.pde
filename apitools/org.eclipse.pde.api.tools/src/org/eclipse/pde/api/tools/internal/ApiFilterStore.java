/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Base implementation of a filter store for Api components
 * 
 * @since 1.0.0
 */
public class ApiFilterStore implements IApiFilterStore {
	
	/**
	 * A storage node that describes an {@link IApiProblemFilter}
	 */
	class FilterNode implements Comparable {
		private IApiProblemFilter fFilter = null;
		private FilterNode fParent = null;
		/**
		 * <pre>HashMap<IElementDescriptor, FilterNode></pre>
		 */
		private HashMap fChildren = new HashMap();
		
		public FilterNode(FilterNode parent, IApiProblemFilter filter) {
			fParent = parent;
			fFilter = filter;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if(o instanceof FilterNode) {
				return ((FilterNode) o).compareTo(this.fFilter.getElement());
			}
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public boolean equals(Object obj) {
			if(obj instanceof FilterNode) {
				IElementDescriptor desc1 = ((FilterNode)obj).fFilter.getElement(),
								   desc2 = this.fFilter.getElement();
				if(desc1.getElementType() == IElementDescriptor.T_METHOD && desc2.getElementType() == IElementDescriptor.T_METHOD) {
					IMethodDescriptor method1 = (IMethodDescriptor) desc1,
									  method2 = (IMethodDescriptor) desc2;
					if(method1.getName().equals(method2.getName())) {
						if(method1.getSignature().equals(method2.getSignature())) {
							return true;
						}
					}
				}
				return desc1.equals(desc2);
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public int hashCode() {
			IElementDescriptor desc = this.fFilter.getElement();
			if(desc.getElementType() == IElementDescriptor.T_METHOD) {
				IMethodDescriptor method = (IMethodDescriptor) desc;
				return method.getName().hashCode() + method.getEnclosingType().hashCode() + Util.dequalifySignature(method.getSignature()).hashCode();
			}
			return desc.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("FilterNode: filtering ["); //$NON-NLS-1$
			buffer.append(this.fFilter.toString());
			buffer.append("]"); //$NON-NLS-1$
			return buffer.toString();
		}
	}
	
	/**
	 * The mapping of filters for this store.
	 * <pre>
	 * HashMap<IElementDescriptor(package), FilterNode>
	 * </pre>
	 */
	private HashMap fFilterMap = new HashMap();
	
	/**
	 * The id of the component that owns this filter store
	 */
	private String fOwningComponent = null;
	
	/**
	 * Constructor
	 * @param owningComponent the id of the component that owns this filter store
	 */
	public ApiFilterStore(String owningComponent) {
		fOwningComponent = owningComponent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiFilterStore#addFilter(org.eclipse.pde.api.tools.IApiProblemFilter)
	 */
	public synchronized void addFilter(IApiProblemFilter filter) {
		if(filter != null) { 
			findNode(filter, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiFilterStore#getFilters()
	 */
	public synchronized IApiProblemFilter[] getFilters() {
		ArrayList filters = new ArrayList();
		collectFilters(fFilterMap, filters);
		return (IApiProblemFilter[]) filters.toArray(new IApiProblemFilter[filters.size()]);
	}

	/**
	 * recursively collects filters into the given list
	 * @param map
	 * @param filters
	 */
	private void collectFilters(Map map, List filters) {
		Collection values = map.values();
		FilterNode node = null;
		for(Iterator iter = values.iterator(); iter.hasNext();) {
			node = (FilterNode) iter.next();
			filters.add(node.fFilter);
			collectFilters(node.fChildren, filters);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiFilterStore#isFiltered(org.eclipse.pde.api.tools.descriptors.IElementDescriptor, java.lang.String, java.lang.String)
	 */
	public synchronized boolean isFiltered(IElementDescriptor element, String[] kinds) {
		if(kinds.length == 0) {
			return false;
		}
		// we need to de-qualify signatures
		//TODO remove once bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=217116 is addressed
		IElementDescriptor tmp = element;
		if(element.getElementType() == IElementDescriptor.T_METHOD) {
				IMethodDescriptor method = (IMethodDescriptor) element;
				tmp = Factory.methodDescriptor(method.getEnclosingType().getQualifiedName(), method.getName(), Util.dequalifySignature(method.getSignature()));
		}
		//We start from the root of the element descriptor path. 
		//The first element we encounter that matches the kinds 'trumps' child matches 
		IElementDescriptor[] paths = tmp.getPath();
		IElementDescriptor curr = null;
		FilterNode node = null;
		Map map = fFilterMap;
		for(int i = 0; i < paths.length; i++) {
			curr = paths[i];
			node = (FilterNode) map.get(curr);
			if(node == null) {
				return false;
			}
			if(Arrays.asList(node.fFilter.getKinds()).containsAll(Arrays.asList(kinds))) {
				return true;
			}
			map = node.fChildren;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiFilterStore#removeFilter(org.eclipse.pde.api.tools.IApiProblemFilter)
	 */
	public synchronized boolean removeFilter(IApiProblemFilter filter) {
		if(filter != null) {
			//if the filter has children set is kinds to empty
			FilterNode node = findNode(filter, false);
			if(node != null) {
				if(node.fChildren.size() > 0) {
					String[] kinds = node.fFilter.getKinds();
					for(int i = 0; i < kinds.length; i++) {
						node.fFilter.removeKind(kinds[i]);
					}
					return true;
				}
				else {
					if(node.fParent == null) {
						return fFilterMap.remove(filter.getElement()) != null;
					}
					else {
						return node.fParent.fChildren.remove(filter.getElement()) != null;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns the node for specified element and context, closest node, or <code>null</code>.
	 * Creates a new node if insert is <code>true</code> and a node is not present.
	 * 
	 * @param component component context (i.e. referenced from this component)
	 * @param element element
	 * @param insert whether to insert a new node
	 * @return filter node or <code>null</code>
	 */
	private FilterNode findNode(IApiProblemFilter filter, boolean insert) {
		IElementDescriptor[] path = filter.getElement().getPath();
		Map map = fFilterMap;
		FilterNode parentNode = null;
		FilterNode node = null;
		IElementDescriptor current = null;
		String[] kinds = {};
		for (int i = 0 ; i < path.length; i++) {
			current = path[i];
			parentNode = node;
			node = (FilterNode) map.get(current);
			if (node == null) {
				if (insert) {
					if(i == path.length-1) {
						kinds = filter.getKinds();
					}
					node = new FilterNode(parentNode, new ApiProblemFilter(filter.getComponentId(), current, kinds));
					map.put(current, node);
				}
				else {
					break;
				}
			}
			else if(node.fFilter.getElement().equals(filter.getElement())) {
				//if the node matches (elements match) add the kind to the current listing
				if(insert) {
					String[] newkinds = filter.getKinds();
					for(int j = 0; j < newkinds.length; j++) {
						node.fFilter.addKind(newkinds[j]);
					}
				}
			}
			map = node.fChildren;
		}
		return node;
	}
	
	/**
	 * Converts the information contained in this filter store to an xml string
	 * @return an xml string representation of this filter store
	 * @throws CoreException
	 */
	public String getStoreAsXml() throws CoreException {
		Document document = Util.newDocument();
		Element root = document.createElement(ApiDescriptionProcessor.ELEMENT_COMPONENT);
		document.appendChild(root);
		root.setAttribute(ApiDescriptionProcessor.ATTR_ID, fOwningComponent);
		addMapToDocument(document, root, fFilterMap);
		return Util.serializeDocument(document);
	}
	
	/**
	 * Depth-first recurses over the filter map adding the maps' contents to the provided document
	 * @param document the document to append to
	 * @param parent the parent element to add any children to
	 * @param elements the elements to add
	 */
	private void addMapToDocument(Document document, Element parent, Map elements) {
		ArrayList keys = new ArrayList(elements.keySet());
		Collections.sort(keys);
		IElementDescriptor element = null;
		FilterNode node = null;
		Element newnode = null;
		String name = null;
		for(Iterator iter = keys.iterator(); iter.hasNext();) {
			element = (IElementDescriptor) iter.next();
			node = (FilterNode) elements.get(element);
			switch(element.getElementType()) {
				case IElementDescriptor.T_PACKAGE: {
					newnode = document.createElement(ApiDescriptionProcessor.ELEMENT_PACKAGE);
					name = ((IPackageDescriptor)element).getName();
					break;
				}
				case IElementDescriptor.T_REFERENCE_TYPE: {
					IReferenceTypeDescriptor type = (IReferenceTypeDescriptor) element;
					newnode = document.createElement(ApiDescriptionProcessor.ELEMENT_TYPE);
					name = Util.getTypeName(type.getQualifiedName());
					break;
				}
				case IElementDescriptor.T_FIELD: {
					newnode = document.createElement(ApiDescriptionProcessor.ELEMENT_FIELD);
					name = ((IFieldDescriptor)element).getName();
					break;
				}
				case IElementDescriptor.T_METHOD: {
					newnode = document.createElement(ApiDescriptionProcessor.ELEMENT_METHOD);
					IMethodDescriptor method = (IMethodDescriptor) element;
					name = method.getName();
					newnode.setAttribute(ApiDescriptionProcessor.ATTR_SIGNATURE, method.getSignature());
					break;
				}
				case IElementDescriptor.T_RESOURCE: {
					newnode = document.createElement(ApiDescriptionProcessor.ELEMENT_RESOURCE);
					name = ((IResourceDescriptor)element).getName();
				}
			}
			if(newnode != null) {
				newnode.setAttribute(ApiDescriptionProcessor.ATTR_NAME, name);
				addKindAttribute(newnode, node.fFilter.getKinds());
				parent.appendChild(newnode);
				addMapToDocument(document, newnode, node.fChildren);
			}
		}
	}
	
	/**
	 * Builds and sets the kind attribute string on the given node
	 * @param node
	 * @param kinds
	 */
	private void addKindAttribute(Element node, String[] kinds) {
		StringBuffer buff = new StringBuffer();
		for(int i = 0; i < kinds.length; i++) {
			buff.append(kinds[i]);
			if(i < kinds.length-1) {
				buff.append(","); //$NON-NLS-1$
			}
		}
		node.setAttribute(ApiDescriptionProcessor.ATTR_KIND, buff.toString());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Api filter store for component: "+fOwningComponent; //$NON-NLS-1$
	}
}
