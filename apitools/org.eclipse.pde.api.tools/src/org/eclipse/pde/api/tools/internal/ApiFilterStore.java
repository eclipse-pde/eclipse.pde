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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Base implementation of a filter store for Api components
 * 
 * @since 1.0.0
 */
public class ApiFilterStore implements IApiFilterStore, IResourceChangeListener {
	private static final String GLOBAL = "!global!"; //$NON-NLS-1$
	public static final int CURRENT_STORE_VERSION = 2;
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
	private boolean fTriggeredChange = false;
	
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
		if(!fNeedsSaving) {
			return;
		}
		WorkspaceJob job = new WorkspaceJob("") { //$NON-NLS-1$
			public IStatus runInWorkspace(IProgressMonitor monitor)	throws CoreException {
				if(DEBUG) {
					System.out.println("persisting api filters for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					if(monitor == null) {
						monitor = new NullProgressMonitor();
					}
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
							IFolder folder = (IFolder) file.getParent();
							file.delete(true, monitor);
							if(folder.members().length == 0 && folder.isAccessible()) {
								folder.delete(true, monitor);
							}
							fTriggeredChange = true;
						}
						return Status.OK_STATUS;
					}
					InputStream xstream = Util.getInputStreamFromString(xml);
					if(xstream == null) {
						return Status.CANCEL_STATUS;
					}
					try {
						if(!file.exists()) {
							IFolder folder = (IFolder) file.getParent();
							if(!folder.exists()) {
								folder.create(true, true, monitor);
							}
							file.create(xstream, true, monitor);
						}
						else {
							file.setContents(xstream, true, false, monitor);
						}
					}
					finally {
						xstream.close();
					}
					fTriggeredChange = true;
					fNeedsSaving = false;
				}
				catch(CoreException ce) {
					ApiPlugin.log(ce);
				}
				catch (IOException ioe) {
					ApiPlugin.log(ioe);	
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
		initializeApiFilters();
		for(int i = 0; i < filters.length; i++) {
			IApiProblem problem = filters[i].getUnderlyingProblem();
			String resourcePath = problem.getResourcePath();
			if (resourcePath == null) {
				continue;
			}
			IResource resource = fProject.getProject().findMember(new Path(resourcePath));
			if(resource == null) {
				continue;
			}
			Map pTypeNames = (HashMap) fFilterMap.get(resource);
			String typeName = problem.getTypeName();
			if (typeName == null) {
				typeName = GLOBAL;
			}
			Set pfilters = null;
			if(pTypeNames == null) {
				pTypeNames = new HashMap();
				pfilters = new HashSet();
				pTypeNames.put(typeName, pfilters);
				fFilterMap.put(resource, pTypeNames);
			} else {
				pfilters = (Set) pTypeNames.get(typeName);
				if (pfilters == null) {
					pfilters = new HashSet();
					pTypeNames.put(typeName, pfilters);
				}
			}
			fNeedsSaving |= pfilters.add(filters[i]);
		}
		persistApiFilters();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFilters(org.eclipse.pde.api.tools.internal.provisional.IApiProblem[])
	 */
	public synchronized void addFilters(IApiProblem[] problems) {
		internalAddFilters(problems, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#getFilters(org.eclipse.core.resources.IResource)
	 */
	public synchronized IApiProblemFilter[] getFilters(IResource resource) {
		initializeApiFilters();
		Map pTypeNames = (Map) fFilterMap.get(resource);
		if(pTypeNames == null) {
			return new IApiProblemFilter[0];
		}
		List allFilters = new ArrayList();
		for (Iterator iterator = pTypeNames.values().iterator(); iterator.hasNext(); ) {
			Set values = (Set) iterator.next();
			allFilters.addAll(values);
		}
		return (IApiProblemFilter[]) allFilters.toArray(new IApiProblemFilter[allFilters.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#isFiltered(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public synchronized boolean isFiltered(IApiProblem problem) {
		initializeApiFilters();
		String resourcePath = problem.getResourcePath();
		if (resourcePath == null) {
			return false;
		}
		IResource resource = fProject.getProject().findMember(new Path(resourcePath));
		if(resource == null) {
			return false;
		}
		IApiProblemFilter[] filters = this.getFilters(resource);
		if(filters == null) {
			if(DEBUG) {
				System.out.println("no filters defined for ["+resourcePath+"] return not filtered"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}
		IApiProblemFilter filter = null;
		for(int i = 0, max = filters.length; i < max; i++) {
			filter = filters[i];
			IApiProblem underlyingProblem = filter.getUnderlyingProblem();
			if(underlyingProblem.equals(problem)) {
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
		initializeApiFilters();
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
		for(int i = 0; i < filters.length; i++) {
			IApiProblem underlyingProblem = filters[i].getUnderlyingProblem();
			String resourcePath = underlyingProblem.getResourcePath();
			if (resourcePath == null) {
				continue;
			}
			IResource resource = fProject.getProject().findMember(new Path(resourcePath));
			if(resource == null) {
				continue;
			}
			Map pTypeNames = (Map) fFilterMap.get(resource);
			if (pTypeNames == null) continue;
			String typeName = underlyingProblem.getTypeName();
			if (typeName == null) {
				typeName = GLOBAL;
			}
			Set pfilters = (Set) pTypeNames.get(typeName);
			if(pfilters != null && pfilters.remove(filters[i])) {
				if(DEBUG) {
					System.out.println("removed filter: ["+filters[i]+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				fNeedsSaving |= true;
				success &= true;
				if(pfilters.isEmpty()) {
					pTypeNames.remove(typeName);
					if (pTypeNames.isEmpty()) {
						success &= fFilterMap.remove(resource) != null;
					}
				}
			} else {
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
	private String getStoreAsXml() throws CoreException {
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
		for(Iterator iter = fFilterMap.keySet().iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			Map pTypeNames = (Map) fFilterMap.get(resource);
			if(pTypeNames == null) {
				continue;
			}
			for (Iterator iterator = pTypeNames.entrySet().iterator(); iterator.hasNext(); ) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String typeName = (String) entry.getKey();
				Set filters = (Set) entry.getValue();
				if(filters.isEmpty()) {
					continue;
				}
				Element relement = document.createElement(IApiXmlConstants.ELEMENT_RESOURCE);
				relement.setAttribute(IApiXmlConstants.ATTR_PATH, resource.getProjectRelativePath().toPortableString());
				boolean typeNameIsInitialized = false;
				if (typeName != GLOBAL) {
					relement.setAttribute(IApiXmlConstants.ATTR_TYPE, typeName);
					typeNameIsInitialized = true;
				}
				root.appendChild(relement);
				typeName = null;
				for(Iterator iterator2 = filters.iterator(); iterator2.hasNext(); ) {
					IApiProblem problem = ((IApiProblemFilter) iterator2.next()).getUnderlyingProblem();
					typeName = problem.getTypeName();
					Element filterElement = document.createElement(IApiXmlConstants.ELEMENT_FILTER);
					filterElement.setAttribute(IApiXmlConstants.ATTR_ID, Integer.toString(problem.getId()));
					String[] messageArguments = problem.getMessageArguments();
					int length = messageArguments.length;
					if(length > 0) {
						Element messageArgumentsElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
						for (int j = 0; j < length; j++) {
							Element messageArgumentElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
							messageArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(messageArguments[j]));
							messageArgumentsElement.appendChild(messageArgumentElement);
						}
						filterElement.appendChild(messageArgumentsElement);
					}
					relement.appendChild(filterElement);
				}
				if (typeName != null && !typeNameIsInitialized && typeName.length() != 0) {
					relement.setAttribute(IApiXmlConstants.ATTR_TYPE, typeName);
				}
			}
		}
		return Util.serializeDocument(document);
	}

	/**
	 * Initializes the backing filter map for this store from the .aip_filters file. Does nothing if the filter store has already been
	 * initialized.
	 */
	private void initializeApiFilters() {
		if(fFilterMap != null) {
			return;
		}
		if(DEBUG) {
			System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
		}
		fFilterMap = new HashMap(5);
		IPath filepath = getFilterFilePath();
		IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(filepath);
		if(file == null) {
			return;
		}
		String xml = null;
		InputStream contents = null;
		try {
			contents = ((IFile)file).getContents();
			xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
		}
		catch(CoreException ce) {}
		catch(IOException ioe) {}
		finally {
			if (contents != null) {
				try {
					contents.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
		if(xml == null) {
			return;
		}
		Element root = null;
		try {
			root = Util.parseDocument(xml);
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
			return;
		}
		String component = root.getAttribute(IApiXmlConstants.ATTR_ID);
		if(component.length() == 0) {
			return;
		}
		String versionValue = root.getAttribute(IApiXmlConstants.ATTR_VERSION);
		int currentVersion = Integer.parseInt(IApiXmlConstants.API_FILTER_STORE_CURRENT_VERSION);
		int version = 0;
		if(versionValue.length() != 0) {
			try {
				version = Integer.parseInt(versionValue);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		if (version != currentVersion) {
			// we discard all filters since there is no way to retrieve the type name
			fNeedsSaving = true;
			persistApiFilters();
			return;
		}
		NodeList resources = root.getElementsByTagName(IApiXmlConstants.ELEMENT_RESOURCE);
		ArrayList newfilters = new ArrayList();
		for(int i = 0; i < resources.getLength(); i++) {
			Element element = (Element) resources.item(i);
			String path = element.getAttribute(IApiXmlConstants.ATTR_PATH);
			if(path.length() == 0) {
				continue;
			}
			String typeName = element.getAttribute(IApiXmlConstants.ATTR_TYPE);
			if (typeName.length() == 0) {
				typeName = null;
			}
			IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(component);
			if(project == null) {
				continue;
			}
			IResource resource = project.findMember(new Path(path));
			if(resource == null) {
				continue;
			}
			NodeList filters = element.getElementsByTagName(IApiXmlConstants.ELEMENT_FILTER);
			for(int j = 0; j < filters.getLength(); j++) {
				element = (Element) filters.item(j);
				int id = loadIntegerAttribute(element, IApiXmlConstants.ATTR_ID);
				if(id <= 0) {
					continue;
				}
				String[] messageargs = null;
				NodeList elements = element.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
				if (elements.getLength() != 1) continue;
				Element messageArguments = (Element) elements.item(0);
				NodeList arguments = messageArguments.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
				int length = arguments.getLength();
				messageargs = new String[length];
				for (int k = 0; k < length; k++) {
					Element messageArgument = (Element) arguments.item(k);
					messageargs[k] = messageArgument.getAttribute(IApiXmlConstants.ATTR_VALUE);
				}
				newfilters.add(ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
						typeName,
						messageargs, null, null, -1, -1, -1, id));
			}
		}
		internalAddFilters((IApiProblem[]) newfilters.toArray(new IApiProblem[newfilters.size()]), false);
		newfilters.clear();
	}
	
	/**
	 * Internal use method that allows auto-persisting of the filter file to be turned on or off
	 * @param problems the problems to add the the store
	 * @param persist if the filters should be auto-persisted after they are added
	 */
	private void internalAddFilters(IApiProblem[] problems, boolean persist) {
		if(problems == null) {
			if(DEBUG) {
				System.out.println("null problems array not addding filters"); //$NON-NLS-1$
			}
			return;
		}
		initializeApiFilters();
		Set filters = null;
		for(int i = 0; i < problems.length; i++) {
			IApiProblem problem = problems[i];
			IApiProblemFilter filter = new ApiProblemFilter(fProject.getElementName(), problem);
			String resourcePath = problem.getResourcePath();
			if (resourcePath == null) {
				continue;
			}
			IResource resource = fProject.getProject().findMember(new Path(resourcePath));
			if(resource == null) {
				continue;
			}
			Map pTypeNames = (Map) fFilterMap.get(resource);
			String typeName = problem.getTypeName();
			if (typeName == null) {
				typeName = GLOBAL;
			}
			if(pTypeNames == null) {
				filters = new HashSet();
				pTypeNames = new HashMap();
				pTypeNames.put(typeName, filters);
				fFilterMap.put(resource, pTypeNames);
			} else {
				filters = (Set) pTypeNames.get(typeName);
				if (filters == null) {
					filters = new HashSet();
					pTypeNames.put(typeName, filters);
				}
			}
			fNeedsSaving |= filters.add(filter);
		}
		if(persist) {
			persistApiFilters();
		}
	}
	
	/**
	 * Loads the specified integer attribute from the given xml element
	 * @param element
	 * @param name
	 * @return
	 */
	private static int loadIntegerAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if(value.length() == 0) {
			return -1;
		}
		try {
			int number = Integer.parseInt(value);
			return number;
		}
		catch(NumberFormatException nfe) {}
		return -1;
	}
	
	/**
	 * @return the {@link IPath} to the filters file
	 */
	private IPath getFilterFilePath() {
		IPath path = fProject.getPath();
		path = path.append(".settings").append(IApiCoreConstants.API_FILTERS_XML_NAME); //$NON-NLS-1$
		return path;
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
		if(fTriggeredChange) {
			//eat the event if the deletion / addition / change occurred because we persisted the file
			//via the persistApiFilters(..) method
			//see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222442
			fTriggeredChange = false;
			return;
		}
		if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
			if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
				IPath path = getFilterFilePath();
				IResourceDelta leafdelta = event.getDelta().findMember(path);
				if(leafdelta == null) {
					return;
				}
				boolean needsbuild = false;
				if(leafdelta.getKind() == IResourceDelta.REMOVED) {
					if(fFilterMap != null) {
						fFilterMap.clear();
						needsbuild = true;
					}
				}
				else if(leafdelta.getKind() == IResourceDelta.ADDED || 
						(leafdelta.getFlags() & IResourceDelta.CONTENT) != 0 || 
						(leafdelta.getFlags() & IResourceDelta.REPLACED) != 0) {
					IResource resource = leafdelta.getResource();
					if(resource != null && resource.getType() == IResource.FILE) {
						IFile file = (IFile) resource;
						if(file.isAccessible()) {
							try {
								if(fFilterMap != null) {
									fFilterMap.clear();
									fFilterMap = null; 
								}
								initializeApiFilters();
							}
							finally {
								needsbuild = true;
							}
						}
					}
				}
				if(needsbuild) {
					Util.getBuildJob(new IProject[] {fProject.getProject()}).schedule();
				}
			}
		}
	}
}
