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
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
public class ApiFilterStore implements IApiFilterStore, IResourceChangeListener {
	
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
	
	/**
	 * The backing {@link IJavaProject}
	 */
	private IJavaProject fProject = null;
	
	private boolean fNeedsSaving = false;
	private boolean fLoading = false;
	
	/**
	 * Constructor
	 * @param owningComponent the id of the component that owns this filter store
	 */
	public ApiFilterStore(IJavaProject project) {
		Assert.isNotNull(project);
		fProject = project;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	/**
	 * Saves the .api_filters file for the component
	 * @throws IOException 
	 */
	private void persistApiFilters() {
		if(!fNeedsSaving || fLoading) {
			return;
		}
		WorkspaceJob job = new WorkspaceJob("") { //$NON-NLS-1$
			public IStatus runInWorkspace(IProgressMonitor monitor)	throws CoreException {
				if(DEBUG) {
					System.out.println("persisting api filters for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					IProject project = fProject.getProject();
					if(!project.isAccessible()) {
						if(DEBUG) {
							System.out.println("project ["+fProject.getElementName()+"] is not accessible, saving termainated"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						return Status.CANCEL_STATUS;
					}
					String xml = getStoreAsXml();
					IFile file = project.getFile(new Path(".settings").append(IApiCoreConstants.API_FILTERS_XML_NAME)); //$NON-NLS-1$
					if(xml == null) {
						// no filters - delete the file if it exists
						if (file.isAccessible()) {
							file.delete(false, new NullProgressMonitor());
						}
						return Status.OK_STATUS;
					}
					InputStream xstream = Util.getInputStreamFromString(xml);
					if(xstream == null) {
						return Status.CANCEL_STATUS;
					}
					if(!file.exists()) {
						file.create(xstream, true, new NullProgressMonitor());
					}
					else {
						file.setContents(xstream, true, false, new NullProgressMonitor());
					}
					fNeedsSaving = false;
				}
				catch(CoreException ce) {
					ApiPlugin.log(ce);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
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
			fNeedsSaving |= pfilters.add(filters[i]);
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
			fNeedsSaving |= filters.add(filter);
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
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#dispose()
	 */
	public void dispose() {
		if(fFilterMap != null) {
			fFilterMap.clear();
			fFilterMap = null;
		}
 		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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
				fNeedsSaving |= true;
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
		Element root = document.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
		document.appendChild(root);
		root.setAttribute(IApiXmlConstants.ATTR_ID, fProject.getElementName());
		root.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_FILTER_STORE_CURRENT_VERSION);
		HashSet filters = null;
		IResource resource = null;
		IApiProblem problem = null;
		Element relement = null, felement = null;
		for(Iterator iter = fFilterMap.keySet().iterator(); iter.hasNext();) {
			resource = (IResource) iter.next();
			relement = document.createElement(IApiXmlConstants.ELEMENT_RESOURCE);
			relement.setAttribute(IApiXmlConstants.ATTR_PATH, resource.getProjectRelativePath().toPortableString());
			root.appendChild(relement);
			filters = (HashSet) fFilterMap.get(resource);
			if(filters.isEmpty()) {
				continue;
			}
			for(Iterator iter2 = filters.iterator(); iter2.hasNext();) {
				problem = ((IApiProblemFilter) iter2.next()).getUnderlyingProblem();
				felement = document.createElement(IApiXmlConstants.ELEMENT_FILTER);
				felement.setAttribute(IApiXmlConstants.ATTR_CATEGORY, Integer.toString(problem.getCategory()));
				felement.setAttribute(IApiXmlConstants.ATTR_SEVERITY, Integer.toString(problem.getSeverity()));
				felement.setAttribute(IApiXmlConstants.ATTR_KIND, Integer.toString(problem.getKind()));
				felement.setAttribute(IApiXmlConstants.ATTR_FLAGS, Integer.toString(problem.getFlags()));
				felement.setAttribute(IApiXmlConstants.ATTR_MESSAGE, problem.getMessage());
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

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
			try {
				if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
					IPath path = fProject.getPath();
					path = path.append(".settings").append(IApiCoreConstants.API_FILTERS_XML_NAME); //$NON-NLS-1$
					IResourceDelta leafdelta = event.getDelta().findMember(path);
					if(leafdelta == null) {
						return;
					}
					boolean needsbuild = false;
					if(leafdelta.getKind() == IResourceDelta.REMOVED) {
						fFilterMap.clear();
						needsbuild = true;
					}
					else if(leafdelta.getKind() == IResourceDelta.ADDED || 
							(leafdelta.getFlags() & IResourceDelta.CONTENT) != 0 || 
							(leafdelta.getFlags() & IResourceDelta.REPLACED) != 0) {
						IResource resource = leafdelta.getResource();
						if(resource != null && resource.getType() == IResource.FILE) {
							IFile file = (IFile) resource;
							if(file.isAccessible()) {
								try {
									fLoading = true;
									fFilterMap.clear();
									String xml = new String(Util.getInputStreamAsCharArray(file.getContents(), -1, IApiCoreConstants.UTF_8)); 
									ApiDescriptionProcessor.annotateApiFilters(this, xml);
								}
								finally {
									fLoading = false;
									needsbuild = true;
								}
							}
						}
					}
					if(needsbuild) {
						Util.getBuildJob(fProject.getProject()).schedule();
					}
				}
			}
			catch(CoreException ce) {
				ApiPlugin.log(ce);
			}
			catch(IOException ioe) {
				ApiPlugin.log(ioe);
			}
		}
	}
}
