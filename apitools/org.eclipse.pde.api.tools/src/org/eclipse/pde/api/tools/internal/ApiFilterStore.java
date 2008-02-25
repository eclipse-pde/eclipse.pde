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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
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
	 * The mapping of filters for this store.
	 * <pre>
	 * HashMap<IResource, HashSet<IApiProblemFilter>>
	 * </pre>
	 */
	private HashMap fFilterMap = null;
	
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
		if(fFilterMap == null) {
			fFilterMap = new HashMap();
		}
		IResource res = filter.getUnderlyingProblem().getResource();
		HashSet filters = (HashSet) fFilterMap.get(res);
		if(filters == null) {
			filters = new HashSet();
			fFilterMap.put(res, filters);
		}
		filters.add(filter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFilter(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public synchronized void addFilter(IApiProblem problem) {
		if(problem == null) {
			return;
		}
		if(fFilterMap == null) {
			fFilterMap = new HashMap();
		}
		IApiProblemFilter filter = new ApiProblemFilter(fOwningComponent, problem);
		IResource res = problem.getResource();
		HashSet filters = (HashSet) fFilterMap.get(res);
		if(filters == null) {
			filters = new HashSet();
			fFilterMap.put(res, filters);
		}
		filters.add(filter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#getFilters(org.eclipse.core.resources.IResource)
	 */
	public synchronized IApiProblemFilter[] getFilters(IResource resource) {
		if(fFilterMap == null) {
			return new IApiProblemFilter[0];
		}
		HashSet filters = (HashSet) fFilterMap.get(resource);
		if(filters == null) {
			return new IApiProblemFilter[0];
		}
		return (IApiProblemFilter[]) filters.toArray(new IApiProblemFilter[filters.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#isFiltered(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public synchronized boolean isFiltered(IApiProblem problem) {
		if(fFilterMap == null) {
			return false;
		}
		HashSet filters = (HashSet) fFilterMap.get(problem.getResource());
		if(filters == null) {
			return false;
		}
		IApiProblemFilter filter = null;
		for(Iterator iter = filters.iterator(); iter.hasNext();) {
			filter = (IApiProblemFilter) iter.next();
			if(filter.getUnderlyingProblem().equals(problem)) {
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#getResources()
	 */
	public synchronized IResource[] getResources() {
		if(fFilterMap == null) {
			return new IResource[0];
		}
		Collection resources = fFilterMap.keySet();
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiFilterStore#removeFilter(org.eclipse.pde.api.tools.IApiProblemFilter)
	 */
	public synchronized boolean removeFilter(IApiProblemFilter filter) {
		if(fFilterMap == null) {
			return false;
		}
		HashSet filters = (HashSet) fFilterMap.get(filter.getUnderlyingProblem().getResource());
		if(filters == null) {
			return false;
		}
		if(filters.remove(filter)) {
			//if there are no filters left remove the key from the map
			if(filters.isEmpty()) {
				return fFilterMap.remove(filter.getUnderlyingProblem().getResource()) != null;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Converts the information contained in this filter store to an xml string
	 * @return an xml string representation of this filter store
	 * @throws CoreException
	 */
	public String getStoreAsXml() throws CoreException {
		if(fFilterMap == null) {
			return null;
		}
		if(fFilterMap.isEmpty()) {
			return null;
		}
		Document document = Util.newDocument();
		Element root = document.createElement(ApiDescriptionProcessor.ELEMENT_COMPONENT);
		document.appendChild(root);
		root.setAttribute(ApiDescriptionProcessor.ATTR_ID, fOwningComponent);
		HashSet filters = null;
		IResource resource = null;
		IApiProblem problem = null;
		Element relement = null, felement = null;
		for(Iterator iter = fFilterMap.keySet().iterator(); iter.hasNext();) {
			resource = (IResource) iter.next();
			relement = document.createElement(ApiDescriptionProcessor.ELEMENT_RESOURCE);
			relement.setAttribute(ApiDescriptionProcessor.ATTR_PATH, resource.getProjectRelativePath().toPortableString());
			root.appendChild(relement);
			filters = (HashSet) fFilterMap.get(resource);
			if(filters.isEmpty()) {
				continue;
			}
			for(Iterator iter2 = filters.iterator(); iter2.hasNext();) {
				problem = ((IApiProblemFilter) iter2.next()).getUnderlyingProblem();
				felement = document.createElement(ApiDescriptionProcessor.ELEMENT_FILTER);
				felement.setAttribute(ApiDescriptionProcessor.ATTR_CATEGORY, Integer.toString(problem.getCategory()));
				felement.setAttribute(ApiDescriptionProcessor.ATTR_SEVERITY, Integer.toString(problem.getSeverity()));
				felement.setAttribute(ApiDescriptionProcessor.ATTR_KIND, Integer.toString(problem.getKind()));
				felement.setAttribute(ApiDescriptionProcessor.ATTR_FLAGS, Integer.toString(problem.getFlags()));
				felement.setAttribute(ApiDescriptionProcessor.ATTR_MESSAGE, problem.getMessage());
				relement.appendChild(felement);
			}
		}
		return Util.serializeDocument(document);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Api filter store for component: "+fOwningComponent; //$NON-NLS-1$
	}
}
