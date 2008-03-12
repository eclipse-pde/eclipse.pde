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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemReporter;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import com.ibm.icu.text.MessageFormat;

/**
 * Class for collecting {@link IApiProblem}s and creating {@link IMarker}s (if running in the framework)
 * 
 * @since 1.0.0
 */
public class ApiProblemReporter implements IApiProblemReporter {

	/**
	 * The current listing of {@link IApiProblem}s
	 */
	private HashMap fProblems = null;
	/**
	 * The singleton instance
	 */
	private static ApiProblemReporter fInstance = null;
	private static IProject fProject = null;

	/**
	 * Constructor
	 */
	private ApiProblemReporter() {}
	
	/**
	 * Returns an {@link ApiProblemReporter} 
	 * @return an {@link ApiProblemReporter}
	 */
	public static ApiProblemReporter reporter(IProject project) {
		Assert.isNotNull(project);
		Assert.isTrue(project.isAccessible());
		if(fInstance == null) {
			fInstance = new ApiProblemReporter();
		}
		fProject = project;
		return fInstance;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemReporter#addProblem(org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem)
	 */
	public boolean addProblem(IApiProblem problem) {
		if(problem == null) {
			return false;
		}
		if(isProblemFiltered(problem)) {
			return false;
		}
		if(fProblems == null) {
			fProblems = new HashMap();
		}
		String problemtype = getProblemTypeFromCategory(problem.getCategory());
		HashSet problems = (HashSet) fProblems.get(problemtype);
		if(problems == null) {
			problems = new HashSet();
			fProblems.put(problemtype, problems);
		}
		return problems.add(problem);
	}
	
	/**
	 * Returns the {@link IApiMarkerConstants} problem type given the 
	 * problem category
	 * @param category
	 * @return the problem type or <code>null</code>
	 */
	private String getProblemTypeFromCategory(int category) {
		switch(category) {
			case IApiProblem.CATEGORY_API_PROFILE: {
				return IApiMarkerConstants.DEFAULT_API_PROFILE_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_BINARY: {
				return IApiMarkerConstants.BINARY_COMPATIBILITY_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_SINCETAGS: {
				return IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_USAGE: {
				return IApiMarkerConstants.API_USAGE_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_VERSION: {
				return IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER;
			}
		}
		return null;
	}
	
	/**
	 * Creates new markers are for the listing of problems added to this reporter.
	 * If no problems have been added to this reporter, or we are not running in the framework,
	 * no work is done
	 */
	public void createMarkers(IProgressMonitor monitor) {
		if(fProblems == null || !ApiPlugin.isRunningInFramework()) {
			return;
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.ApiProblemReporter_creating_problem_markers, fProblems.size());
		try {
			SubMonitor child = null;
			HashSet problems = null;
			String type = null;
			IApiProblem problem = null;
			for(Iterator iter = fProblems.keySet().iterator(); iter.hasNext();) {
				type = (String) iter.next();
				try {
					fProject.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
				}
				catch(CoreException ce) {
					ApiPlugin.log(ce);
					continue;
				}
				problems = (HashSet) fProblems.get(type);
				if(problems != null && problems.size() > 0) {
					try {
						child = localmonitor.newChild(problems.size());
						for(Iterator iter2 = problems.iterator(); iter2.hasNext();) {
							problem = (IApiProblem) iter2.next();
							child.setTaskName(MessageFormat.format(BuilderMessages.ApiProblemReporter_creating_problem_markers_on_0, new String[] {problem.getResourcePath()}));
							createMarkerForProblem(type, problem);
							child.worked(1);
						}
					}
					finally {
						if(child != null && !child.isCanceled()) {
							child.done();
						}
					}
				}
				localmonitor.worked(1);
			}
		}
		finally {
			if(!localmonitor.isCanceled()) {
				localmonitor.done();
			}
		}
	}
	
	/**
	 * Creates an {@link IMarker} on the resource specified
	 * in the problem (via its path) with the given problem
	 * attributes
	 * @param problem the problem to create a marker from
	 */
	private void createMarkerForProblem(String type, IApiProblem problem) {
		IResource resource = resolveResource(problem);
		if(resource == null) {
			return;
		}
		try {
			IMarker marker = resource.createMarker(type);
			marker.setAttributes(
					new String[] {IMarker.MESSAGE, 
							IMarker.SEVERITY, 
							IMarker.LINE_NUMBER, 
							IMarker.CHAR_START, 
							IMarker.CHAR_END,
							IMarker.SOURCE_ID,
							IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID},
					new Object[] {problem.getMessage(),	
							new Integer(problem.getSeverity()),	
							new Integer(problem.getLineNumber()),
							new Integer(problem.getCharStart()),
							new Integer(problem.getCharEnd()),
							ApiAnalysisBuilder.SOURCE,
							new Integer(problem.getId()),
					}
				);
			//add message arguments, if any
			String[] args = problem.getMessageArguments();
			if(args.length > 0) {
				marker.setAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, createArgAttribute(args));
			}
			//add all other extra arguments, if any
			if(problem.getExtraMarkerAttributeIds().length > 0) {
				marker.setAttributes(problem.getExtraMarkerAttributeIds(), problem.getExtraMarkerAttributeValues());
			}
			
		} catch (CoreException e) {
			//ignore and continue
			return;
		}
	}
	
	/**
	 * Creates a single string attribute from an array of strings. Uses the '#' char as 
	 * a delimiter
	 * @param args
	 * @return a single string attribute from an array or arguments
	 */
	private String createArgAttribute(String[] args) {
		StringBuffer buff = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			buff.append(args[i]);
			if(i < args.length-1) {
				buff.append("#"); //$NON-NLS-1$
			}
		}
		return buff.toString();
	}
	
	/**
	 * Resolves the resource from the path in the problem, returns <code>null</code> in 
	 * the following cases: 
	 * <ul>
	 * <li>The resource is not found in the parent project (findMember() returns null)</li>
	 * <li>The resource is not accessible (isAccessible() returns false</li>
	 * </ul>
	 * @param problem the problem to get the resource for
	 * @return the resource or <code>null</code>
	 */
	private IResource resolveResource(IApiProblem problem) {
		IResource resource = fProject.findMember(new Path(problem.getResourcePath()));
		if(resource == null) {
			return null;
		}
		if(!resource.isAccessible()) {
			return null;
		}
		return resource;
	}
	
	/**
	 * Returns if the given {@link IApiProblem} should be filtered from having a problem marker created for it
	 * 
	 * @param problem the problem that may or may not be filtered
	 * @return true if the {@link IApiProblem} should not have a marker created, false otherwise
	 */
	private boolean isProblemFiltered(IApiProblem problem) {
		IApiComponent component = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile().getApiComponent(fProject.getName());
		if(component != null) {
			try {
				return component.getFilterStore().isFiltered(problem);
			}
			catch(CoreException e) {
				//ignore, return false
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemReporter#dispose()
	 */
	public void dispose() {
		if(fProblems != null) {
			fProblems.clear();
			fProblems = null;
		}
		fProject = null;
	}
	
}
