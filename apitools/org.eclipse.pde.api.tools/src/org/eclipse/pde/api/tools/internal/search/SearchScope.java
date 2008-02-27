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
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.AbstractClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;

/**
 * Implementation of an API search scope.
 * 
 * @since 1.0.0
 */
public class SearchScope extends AbstractClassFileContainer implements IApiSearchScope {
		
	/**
	 * Map of components to collection of leaf element in that scope, or an empty
	 * collection for entire component.
	 */
	private Map fComponents = new HashMap();
	
	/**
	 * Map of component id's to components with that id, contained in this scope.
	 */
	private Map fComponentIds = new HashMap();
	
	/**
	 * Adds the entire component to this scope.
	 * 
	 * @param component component to add
	 */
	public void addComponent(IApiComponent component) {
		fComponents.put(component, new HashSet());
		Set components = (Set)fComponentIds.get(component.getId());
		if (components == null) {
			components = new HashSet();
			fComponentIds.put(component.getId(), components);
		}
		components.add(component);
	}
	
	/**
	 * Adds the given element to this scope.
	 * 
	 * @param component component the element is contained in
	 * @param element element in the component to add
	 */
	public void addElement(IApiComponent component, IElementDescriptor element) {
		Set parents = getParents(element);
		Set leaves = (Set) fComponents.get(component);
		if (leaves == null) {
			addComponent(component);
			leaves = (Set) fComponents.get(component);
		}
		// first check if a parent is already in the scope (i.e already contained)
		Iterator iterator = parents.iterator();
		while (iterator.hasNext()) {
			IElementDescriptor el = (IElementDescriptor) iterator.next();
			if (leaves.contains(el)) {
				// already contains a parent element
				return;
			}
		}
		// remove existing leaves that are children of the element being added
		iterator = leaves.iterator();
		while (iterator.hasNext()) {
			IElementDescriptor leaf = (IElementDescriptor) iterator.next();
			parents = getParents(leaf);
			if (parents.contains(element)) {
				iterator.remove();
			}
		}
		leaves.add(element);
	}

	/**
	 * Returns all parent elements of the given element in a set.
	 * 
	 * @param element
	 * @return parent elements
	 */
	private Set getParents(IElementDescriptor element) {
		Set parents = new HashSet();
		IElementDescriptor parent = element.getParent();
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParent();
		}
		return parents;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#createClassFileContainers()
	 */
	protected List createClassFileContainers() throws CoreException {
		List containers = new ArrayList(fComponents.size());
		Iterator iterator = fComponents.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			IApiComponent component = (IApiComponent) entry.getKey();
			if (component != null) {
				Set leaves = (Set) entry.getValue();
				if (leaves.isEmpty()) {
					containers.add(component);
				} else {
					containers.add(new ScopedClassFileContainer(component, (IElementDescriptor[])leaves.toArray(new IElementDescriptor[leaves.size()])));
				}
			}
		}
		return containers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchScope#encloses(java.lang.String, org.eclipse.pde.api.tools.descriptors.IElementDescriptor)
	 */
	public boolean encloses(String componentId, IElementDescriptor element) {
		Set set = (Set) fComponentIds.get(componentId);
		if (set != null) {
			Iterator componets = set.iterator();
			while (componets.hasNext()) {
				Set leaves = (Set) fComponents.get(componets.next());
				if (leaves != null) {
					if (leaves.isEmpty()) {
						// contains every thing in the component
						// TODO: check if the element's type really exists in the component?
						return true;
					}
					Iterator iterator = leaves.iterator();
					Set parents = getParents(element);
					while (iterator.hasNext()) {
						IElementDescriptor leaf = (IElementDescriptor) iterator.next();
						if (leaf.equals(element) || parents.contains(leaf)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public String getOrigin() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("*** Search Scope ***\n"); //$NON-NLS-1$
		IApiComponent key = null;
		HashSet entry = null;
		for(Iterator iter = fComponents.keySet().iterator(); iter.hasNext();){
			key = (IApiComponent) iter.next();
			entry = (HashSet) fComponents.get(key);
			buffer.append("Component: ").append(key.getId()).append('\n'); //$NON-NLS-1$
			buffer.append("Elements: ").append(entry.toString()).append('\n'); //$NON-NLS-1$
		}
		return buffer.toString();
	}
}
