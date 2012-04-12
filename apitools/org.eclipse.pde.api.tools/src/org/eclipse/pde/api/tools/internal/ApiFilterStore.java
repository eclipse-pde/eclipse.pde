/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;

import org.eclipse.jdt.core.IJavaProject;

/**
 * Base implementation of a filter store for API components
 * 
 * @since 1.0.0
 */
public class ApiFilterStore implements IApiFilterStore, IResourceChangeListener {
	
	/**
	 * Constant representing the name of the .settings folder
	 */
	private static final String SETTINGS_FOLDER = ".settings"; //$NON-NLS-1$
	public static final String GLOBAL = "!global!"; //$NON-NLS-1$
	public static final int CURRENT_STORE_VERSION = 2;

	/**
	 * Represents no filters
	 */
	private static IApiProblemFilter[] NO_FILTERS = new IApiProblemFilter[0];
	
	/**
	 * The mapping of filters for this store.
	 * <pre>
	 * HashMap<IResource, HashSet<IApiProblemFilter>>
	 * </pre>
	 */
	private HashMap fFilterMap = null;
	
	/**
	 * Map used to collect unused {@link IApiProblemFilter}s
	 */
	private HashMap fUnusedFilters = null;
	
	/**
	 * The backing {@link IJavaProject}
	 */
	IJavaProject fProject = null;
	
	boolean fNeedsSaving = false;
	boolean fTriggeredChange = false;
	
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
	public void persistApiFilters() {
		if(!fNeedsSaving) {
			return;
		}
		final Map filters = new HashMap(fFilterMap);
		WorkspaceJob job = new WorkspaceJob(Util.EMPTY_STRING) {
			public IStatus runInWorkspace(IProgressMonitor monitor)	throws CoreException {
				if(ApiPlugin.DEBUG_FILTER_STORE) {
					System.out.println("persisting api filters for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					SubMonitor localmonitor = SubMonitor.convert(monitor);
					IProject project = fProject.getProject();
					if(!project.isAccessible()) {
						if(ApiPlugin.DEBUG_FILTER_STORE) {
							System.out.println("project ["+fProject.getElementName()+"] is not accessible, saving terminated"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						return Status.CANCEL_STATUS;
					}
					String xml = getStoreAsXml(filters);
					IFile file = project.getFile(getFilterFilePath(false));
					if(xml == null) {
						if(ApiPlugin.DEBUG_FILTER_STORE) {
							System.out.println("no XML to persist for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						// no filters - delete the file if it exists
						if (file.isAccessible()) {
							IFolder folder = (IFolder) file.getParent();
							file.delete(true, localmonitor);
							if(folder.members().length == 0 && folder.isAccessible()) {
								folder.delete(true, localmonitor);
							}
							fTriggeredChange = true;
						}
						return Status.OK_STATUS;
					}
					String lineDelimiter= getLineDelimiterPreference(file);
					String lineSeparator= System.getProperty("line.separator"); //$NON-NLS-1$
					if (lineDelimiter != null && !lineDelimiter.equals(lineSeparator))
						xml= xml.replaceAll(lineSeparator, lineDelimiter);
					InputStream xstream = Util.getInputStreamFromString(xml);
					if(xstream == null) {
						return Status.CANCEL_STATUS;
					}
					try {
						if(file.getProject().isAccessible()) {
							if(!file.exists()) {
								IFolder folder = (IFolder) file.getParent();
								if(!folder.exists()) {
									folder.create(true, true, localmonitor);
								}
								file.create(xstream, true, localmonitor);
							}
							else {
								file.setContents(xstream, true, false, localmonitor);
							}
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
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFilters(org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter[])
	 */
	public synchronized void addFilters(IApiProblemFilter[] filters) {
		if(filters == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
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
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#addFiltersFor(org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem[])
	 */
	public synchronized void addFiltersFor(IApiProblem[] problems) {
		if(problems == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("null problems array: not addding filters"); //$NON-NLS-1$
			}
			return;
		}
		if(problems.length < 1) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("empty problem array: not addding filters"); //$NON-NLS-1$
			}
			return;
		}
		initializeApiFilters();
		internalAddFilters(problems, null, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#getFilters(org.eclipse.core.resources.IResource)
	 */
	public synchronized IApiProblemFilter[] getFilters(IResource resource) {
		initializeApiFilters();
		Map pTypeNames = (Map) fFilterMap.get(resource);
		if(pTypeNames == null) {
			return NO_FILTERS;
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
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("no resource exists: ["+resourcePath+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}
		IApiProblemFilter[] filters = this.getFilters(resource);
		if(filters == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("no filters defined for ["+resourcePath+"] return not filtered"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}
		IApiProblemFilter filter = null;
		for(int i = 0, max = filters.length; i < max; i++) {
			filter = filters[i];
			if(filter.getUnderlyingProblem().equals(problem)) {
				if(ApiPlugin.DEBUG_FILTER_STORE) {
					System.out.println("recording filter used: ["+filter.toString()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				recordFilterUsed(resource, filter);
				return true;
			}
		}
		if(ApiPlugin.DEBUG_FILTER_STORE) {
			System.out.println("no filter defined for problem: ["+problem.toString()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore#dispose()
	 */
	public void dispose() {
		//if the store is about to be disposed and has pending changes save them asynchronously
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=299319
		persistApiFilters();
		clearFilters();
		if(fUnusedFilters != null) {
			fUnusedFilters.clear();
			fUnusedFilters = null;
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
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("null filters array, not removing"); //$NON-NLS-1$
			}
			return false;
		}
		if(fFilterMap == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
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
				resource = fProject.getProject().getFile(resourcePath);
			}
			Map pTypeNames = (Map) fFilterMap.get(resource);
			if (pTypeNames == null) {
				continue;
			}
			String typeName = underlyingProblem.getTypeName();
			if (typeName == null) {
				typeName = GLOBAL;
			}
			Set pfilters = (Set) pTypeNames.get(typeName);
			if(pfilters != null && pfilters.remove(filters[i])) {
				if(ApiPlugin.DEBUG_FILTER_STORE) {
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
	 * Converts the information contained in the given map to an XML string
	 * 
	 * @param filtermap the mapping of filters to convert to XML
	 * @return an XML string representation of the given mapping of filters
	 * @throws CoreException
	 */
	synchronized String getStoreAsXml(Map filtermap) throws CoreException {
		if(filtermap == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("no filter map returning null XML for project ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
		if(filtermap.isEmpty()) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("empty filter map returning null XML for project ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}
		Document document = Util.newDocument();
		Element root = document.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
		document.appendChild(root);
		root.setAttribute(IApiXmlConstants.ATTR_ID, fProject.getElementName());
		root.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_FILTER_STORE_CURRENT_VERSION);
		Set allFiltersEntrySet = filtermap.entrySet();
		List allFiltersEntries = new ArrayList(allFiltersEntrySet.size());
		allFiltersEntries.addAll(allFiltersEntrySet);
		Collections.sort(allFiltersEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;
				String path1 = ((IResource) entry1.getKey()).getFullPath().toOSString();
				String path2 = ((IResource) entry2.getKey()).getFullPath().toOSString();
				return path1.compareTo(path2);
			}
		});
		for(Iterator iter = allFiltersEntries.iterator(); iter.hasNext();) {
			Map.Entry allFiltersEntry = (Map.Entry) iter.next();
			IResource resource = (IResource) allFiltersEntry.getKey();
			Map pTypeNames = (Map) allFiltersEntry.getValue();
			if(pTypeNames == null) {
				continue;
			}
			Set allTypeNamesEntriesSet = pTypeNames.entrySet();
			List allTypeNamesEntries = new ArrayList(allTypeNamesEntriesSet.size());
			allTypeNamesEntries.addAll(allTypeNamesEntriesSet);
			Collections.sort(allTypeNamesEntries, new Comparator() {
				public int compare(Object o1, Object o2) {
					Map.Entry entry1 = (Map.Entry) o1;
					Map.Entry entry2 = (Map.Entry) o2;
					String typeName1 = (String) entry1.getKey();
					String typeName2 = (String) entry2.getKey();
					return typeName1.compareTo(typeName2);
				}
			});
			for (Iterator iterator = allTypeNamesEntries.iterator(); iterator.hasNext(); ) {
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
				List filtersList = new ArrayList(filters.size());
				filtersList.addAll(filters);
				Collections.sort(filtersList, new Comparator(){
					public int compare(Object o1, Object o2) {
						IApiProblem p1 = ((IApiProblemFilter) o1).getUnderlyingProblem();
						IApiProblem p2 = ((IApiProblemFilter) o2).getUnderlyingProblem();
						int problem1Id = p1.getId();
						int problem2Id = p2.getId();
						int ids = problem1Id - problem2Id;
						if(ids == 0) {
							//if we have the same ids further sort by message args 
							//https://bugs.eclipse.org/bugs/show_bug.cgi?id=304509
							String[] args1 = p1.getMessageArguments();
							String[] args2 = p2.getMessageArguments();
							int length = (args1.length < args2.length ? args1.length : args2.length);
							for (int i = 0; i < length; i++) {
								int args = args1[i].compareTo(args2[i]);
								if(args != 0) {
									//return when they are not equal
									return args;
								}
							}
							return args1.length - args2.length;
						}
						return ids;
					}
				});
				for(Iterator iterator2 = filtersList.iterator(); iterator2.hasNext(); ) {
					IApiProblemFilter filter = (IApiProblemFilter) iterator2.next();
					IApiProblem problem = filter.getUnderlyingProblem();
					typeName = problem.getTypeName();
					Element filterElement = document.createElement(IApiXmlConstants.ELEMENT_FILTER);
					filterElement.setAttribute(IApiXmlConstants.ATTR_ID, Integer.toString(problem.getId()));
					String comment = filter.getComment();
					if(comment != null) {
						filterElement.setAttribute(IApiXmlConstants.ATTR_COMMENT, comment);
					}
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
	 * Initializes the backing filter map for this store from the .api_filters file. Does nothing if the filter store has already been
	 * initialized.
	 */
	private synchronized void initializeApiFilters() {
		if(fFilterMap != null) {
			return;
		}
		if(ApiPlugin.DEBUG_FILTER_STORE) {
			System.out.println("initializing api filter map for project ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fFilterMap = new HashMap(5);
		IPath filepath = getFilterFilePath(true);
		IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(filepath, true);
		if(file == null) {
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println(".api_filter file not found during initialization for project ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return;
		}
		String xml = null;
		InputStream contents = null;
		try {
			IFile filterFile = (IFile)file;
			if (filterFile.exists()) {
				contents = filterFile.getContents();
				xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
			}
		}
		catch(CoreException e) {
			ApiPlugin.log(e);
		}
		catch(IOException e) {
			ApiPlugin.log(e);
		}
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
		if (root == null) {
			return;
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
		ArrayList comments = new ArrayList();
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
				String comment = element.getAttribute(IApiXmlConstants.ATTR_COMMENT);
				comments.add((comment.length() < 1 ? null : comment));
				newfilters.add(ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
						typeName,
						messageargs, null, null, -1, -1, -1, id));
			}
		}
		internalAddFilters((IApiProblem[]) newfilters.toArray(new IApiProblem[newfilters.size()]), 
				(String[]) comments.toArray(new String[comments.size()]), 
				false);
		newfilters.clear();
	}
	
	/**
	 * Internal use method that allows auto-persisting of the filter file to be turned on or off
	 * @param problems the problems to add the the store
	 * @param comments the comments associated with each problem
	 * @param persist if the filters should be auto-persisted after they are added
	 */
	private synchronized void internalAddFilters(IApiProblem[] problems, String[] comments, boolean persist) {
		Set filters = null;
		for(int i = 0; i < problems.length; i++) {
			IApiProblem problem = problems[i];
			IApiProblemFilter filter = new ApiProblemFilter(fProject.getElementName(), problem, (comments == null ? null : comments[i]));
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
		else {
			//need to reset the flag during initialization if we are not going to persist
			//the filters, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=309635
			fNeedsSaving = false;
		}
	}
	
	/**
	 * Callback hook to tell the filter store it needs to be saved on the next cycle
	 * 
	 * @since 1.1
	 */
	public void needsSaving() {
		fNeedsSaving = true;
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
	IPath getFilterFilePath(boolean includeproject) {
		if(includeproject) {
			IPath path = fProject.getPath();
			return path.append(SETTINGS_FOLDER).append(IApiCoreConstants.API_FILTERS_XML_NAME);
		}
		return new Path(SETTINGS_FOLDER).append(IApiCoreConstants.API_FILTERS_XML_NAME);
	}

	static String getLineDelimiterPreference(IFile file) {
		IScopeContext[] scopeContext;
		if (file != null && file.getProject() != null) {
			// project preference
			scopeContext= new IScopeContext[] { new ProjectScope(file.getProject()) };
			String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineDelimiter != null)
				return lineDelimiter;
		}
		// workspace preference
		scopeContext= new IScopeContext[] { InstanceScope.INSTANCE };
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
	}

	/**
	 * Start recording filter usage for this store.
	 */
	public synchronized void recordFilterUsage() {
		initializeApiFilters();
		fUnusedFilters = new HashMap();
		IResource resource = null;
		Map types = null;
		Set values = null;
		for(Iterator iter = fFilterMap.keySet().iterator(); iter.hasNext();) {
			resource = (IResource) iter.next();
			types = (Map) fFilterMap.get(resource);
			values = new HashSet();
			fUnusedFilters.put(resource, values);
			for(Iterator iter2 = types.keySet().iterator(); iter2.hasNext();) {
				values.addAll((Set) types.get(iter2.next()));
			}
		}
	}
	
	/**
	 * records that the following filter has been used
	 * @param resource
	 * @param filter
	 */
	private void recordFilterUsed(IResource resource, IApiProblemFilter filter) {
		if(fUnusedFilters != null) {
			Set unused = (Set) fUnusedFilters.get(resource);
			if(unused != null) {
				unused.remove(filter);
				if(unused.isEmpty()) {
					fUnusedFilters.remove(resource);
				}
			}
		}
	}
	
	/**
	 * Returns all of the unused filters for this store at the moment in time this
	 * method is called.
	 * @param the resource the filter applies to
	 * @param typeName the name of the type the filter appears on
	 * @param categories the collection of {@link IApiProblem} categories to ignore 
	 * @see {@link IApiProblem#getCategory()}
	 * @return the listing of currently unused filters or an empty list, never <code>null</code>
	 */
	public IApiProblemFilter[] getUnusedFilters(IResource resource, String typeName, int[] categories) {
		if(fUnusedFilters != null) {
			Set unused = new HashSet();
			Set set = null;
			if(resource != null) {
				// add any unused filters for the resource
				set = (Set) fUnusedFilters.get(resource);
				if (set != null) {
					collectFilterFor(set, typeName, unused, categories);
				}
				if(Util.isManifest(resource.getProjectRelativePath())) {
					//we need to add any filters that are cached for resources
					//that no longer exist - deleted types
					//deleted types are only ever passed in with the manifest associated with them
					IResource res = null;
					for (Iterator iter = fUnusedFilters.keySet().iterator(); iter.hasNext();) {
						res = (IResource) iter.next();
						if(res.exists()) {
							continue;
						}
						if(!res.getProject().equals(resource.getProject())) {
							continue;
						}
						set = (Set)fUnusedFilters.get(res);
						collectFilterFor(set, typeName, unused, categories);
					}
				}
			}
			else {
				for(Iterator iter = fUnusedFilters.keySet().iterator(); iter.hasNext();) {
					set = (Set) fUnusedFilters.get(iter.next());
					if(set != null) {
						unused.addAll(set);
					}
				}
			}
			int size = unused.size();
			if (size == 0) {
				return NO_FILTERS;
			}
			return (IApiProblemFilter[]) unused.toArray(new IApiProblemFilter[size]);
		}
		return NO_FILTERS;
	}
	
	/**
	 * Collects the complete set of problem filters from the given set whose
	 * underlying problem categories do not match any from the given array and whose
	 * type name matches the underlying problem type name.
	 *   
	 * @param filters
	 * @param typename
	 * @param collector
	 * @param categories
	 */
	private void collectFilterFor(Set filters, String typename, Set collector, int[] categories) {
		ApiProblemFilter filter = null;
		for (Iterator iter = filters.iterator(); iter.hasNext();) {
			filter = (ApiProblemFilter) iter.next();
			IApiProblem underlyingProblem = filter.getUnderlyingProblem();
			if (underlyingProblem != null) {
				if(matchesCategory(underlyingProblem, categories)) {
					continue;
				}
				String underlyingTypeName = underlyingProblem.getTypeName();
				if (underlyingTypeName != null && (typename == null || underlyingTypeName.equals(typename))) {
					collector.add(filter);
				}
			}
		}
	}
	
	/**
	 * Returns if the category of the given problem matches one of the categories
	 * given in the collection. If the collection of categories is <code>null</code>
	 * the problem does not match.
	 * @param problem
	 * @param categories
	 * @return true if the given collection contains the given problems' category, false otherwise
	 * @since 1.1
	 */
	private boolean matchesCategory(IApiProblem problem, int[] categories) {
		if(categories != null) {
			int cat = problem.getCategory();
			for (int i = 0; i < categories.length; i++) {
				if(cat == categories[i]) {
					return true;
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "API filter store for component: "+fProject.getElementName(); //$NON-NLS-1$
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
			if(ApiPlugin.DEBUG_FILTER_STORE) {
				System.out.println("ignoring triggered change"); //$NON-NLS-1$
			}
			return;
		}
		if(event.getType() == IResourceChangeEvent.POST_CHANGE) {
			IPath path = getFilterFilePath(true);
			IResourceDelta leafdelta = event.getDelta().findMember(path);
			if(leafdelta == null) {
				return;
			}
			boolean needsbuild = false;
			if(leafdelta.getKind() == IResourceDelta.REMOVED) {
				if(ApiPlugin.DEBUG_FILTER_STORE) {
					System.out.println("processed REMOVED delta"); //$NON-NLS-1$
				}
				if(fFilterMap != null) {
					fFilterMap.clear();
					needsbuild = fProject.getProject().isAccessible();
				}
			}
			else if(leafdelta.getKind() == IResourceDelta.ADDED || 
					(leafdelta.getFlags() & IResourceDelta.CONTENT) != 0 || 
					(leafdelta.getFlags() & IResourceDelta.REPLACED) != 0) {
				if(ApiPlugin.DEBUG_FILTER_STORE) {
					System.out.println("processing ADDED or CONTENT or REPLACED"); //$NON-NLS-1$
				}
				IResource resource = leafdelta.getResource();
				if(resource != null && resource.getType() == IResource.FILE) {
					if(ApiPlugin.DEBUG_FILTER_STORE) {
						System.out.println("processed FILE delta for ["+resource.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					IFile file = (IFile) resource;
					if(file.isAccessible()) {
						try {
							clearFilters();
							initializeApiFilters();
						}
						finally {
							needsbuild = fProject.getProject().isAccessible();
						}
					}
				}
			}
			if(needsbuild && !ResourcesPlugin.getWorkspace().isAutoBuilding()) {
				Util.getBuildJob(new IProject[] {fProject.getProject()}, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
			}
		}
	}

	/**
	 * Clears out the filter map
	 */
	private synchronized void clearFilters() {
		if(fFilterMap != null) {
			fFilterMap.clear();
			fFilterMap = null; 
		}
	}
}
