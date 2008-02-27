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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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
	 * Constant used for controlling tracing in the plug-in workspace component
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the plug-in workspace component
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * The mapping of filters for this store.
	 * <pre>
	 * HashMap<IResource, HashSet<IApiProblemFilter>>
	 * </pre>
	 */
	private HashMap fFilterMap = null;
	
	private IJavaProject fProject = null;
	
	/**
	 * Constructor
	 * @param owningComponent the id of the component that owns this filter store
	 */
	public ApiFilterStore(IJavaProject project) {
		Assert.isNotNull(project);
		fProject = project;
	}
	
	/**
	 * Saves the .api_filters file for the component
	 * @throws IOException 
	 */
	private void persistApiFilters() {
		if(DEBUG) {
			System.out.println("persisting api filters for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			IProject project = fProject.getProject();
			if(!project.isAccessible()) {
				if(DEBUG) {
					System.out.println("project ["+fProject.getElementName()+"] is not accessible, saving termainated"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return;
			}
			String xml = getStoreAsXml();
			IFile file = project.getFile(new Path(".settings").append(BundleApiComponent.API_FILTERS_XML_NAME)); //$NON-NLS-1$
			if(xml == null) {
				// no filters - delete the file if it exists
				if (file.exists()) {
					file.delete(false, new NullProgressMonitor());
				}
				return;
			}
			InputStream xstream = Util.getInputStreamFromString(xml);
			if(xstream == null) {
				return;
			}
			if(!file.exists()) {
				file.create(xstream, true, new NullProgressMonitor());
			}
			else {
				file.setContents(xstream, true, false, new NullProgressMonitor());
			}
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFilters(org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter[])
	 */
	public synchronized void addFilters(IApiProblemFilter[] filters) {
		if(filters == null) {
			if(DEBUG) {
				System.out.println("null filters array, not adding filters"); //$NON-NLS-1$
			}
			return;
		}
		if(fFilterMap == null) {
			if(DEBUG) {
				System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
			}
			fFilterMap = new HashMap();
		}
		IResource res = null;
		HashSet pfilters = null;
		for(int i = 0; i < filters.length; i++) {
			res = filters[i].getUnderlyingProblem().getResource();
			pfilters = (HashSet) fFilterMap.get(res);
			if(pfilters == null) {
				pfilters = new HashSet();
				fFilterMap.put(res, pfilters);
			}
			pfilters.add(filters[i]);
		}
		persistApiFilters();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFilters(org.eclipse.pde.api.tools.internal.provisional.IApiProblem[])
	 */
	public synchronized void addFilters(IApiProblem[] problems) {
		if(problems == null) {
			if(DEBUG) {
				System.out.println("null problems array not addding filters"); //$NON-NLS-1$
			}
			return;
		}
		if(fFilterMap == null) {
			if(DEBUG) {
				System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
			}
			fFilterMap = new HashMap();
		}
		IApiProblemFilter filter = null;
		IResource res = null;
		HashSet filters = null;
		for(int i = 0; i < problems.length; i++) {
			filter = new ApiProblemFilter(fProject.getElementName(), problems[i]);
			res = problems[i].getResource();
			filters = (HashSet) fFilterMap.get(res);
			if(filters == null) {
				filters = new HashSet();
				fFilterMap.put(res, filters);
			}
			filters.add(filter);
		}
		persistApiFilters();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#getFilters(org.eclipse.core.resources.IResource)
	 */
	public synchronized IApiProblemFilter[] getFilters(IResource resource) {
		if(fFilterMap == null) {
			if(DEBUG) {
				System.out.println("null filter map, returning empty collection"); //$NON-NLS-1$
			}
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
			if(DEBUG) {
				System.out.println("null filter map return not filtered"); //$NON-NLS-1$
			}
			return false;
		}
		HashSet filters = (HashSet) fFilterMap.get(problem.getResource());
		if(filters == null) {
			if(DEBUG) {
				System.out.println("no filters defined for ["+problem.getResource().getName()+"] return nuot filtered"); //$NON-NLS-1$ //$NON-NLS-2$
			}
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
			if(DEBUG) {
				System.out.println("null filter map, return empty resources collection"); //$NON-NLS-1$
			}
			return new IResource[0];
		}
		Collection resources = fFilterMap.keySet();
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#removeFilters(org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter[])
	 */
	public synchronized boolean removeFilters(IApiProblemFilter[] filters) {
		if(filters == null) {
			if(DEBUG) {
				System.out.println("null filters array, not removing"); //$NON-NLS-1$
			}
			return false;
		}
		if(fFilterMap == null) {
			if(DEBUG) {
				System.out.println("null filter map, not removing"); //$NON-NLS-1$
			}
			return false;
		}
		boolean success = true;
		HashSet pfilters = null;
		IResource res = null;
		for(int i = 0; i < filters.length; i++) {
			res = filters[i].getUnderlyingProblem().getResource();
			pfilters = (HashSet) fFilterMap.get(res);
			if(pfilters != null && pfilters.remove(filters[i])) {
				if(DEBUG) {
					System.out.println("removed filter: ["+filters[i]+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				success &= true;
				if(pfilters.isEmpty()) {
					success &= fFilterMap.remove(res) != null;
				}
			}
			else {
				success &= false;
			}
		}
		persistApiFilters();
		return success;
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
		root.setAttribute(ApiDescriptionProcessor.ATTR_ID, fProject.getElementName());
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
		return "Api filter store for component: "+fProject.getElementName(); //$NON-NLS-1$
	}
}
