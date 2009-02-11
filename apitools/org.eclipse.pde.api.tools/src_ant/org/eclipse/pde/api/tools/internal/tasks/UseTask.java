/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.ApiUseSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Abstract class describing a use task
 */
public class UseTask extends CommonUtilsTask {
	
	protected static final String NO_API_DESCRIPTION = "no_description"; //$NON-NLS-1$
	protected static final String EXCLUDED = "excluded"; //$NON-NLS-1$
	protected static final String RESOLUTION_ERRORS = "resolution_errors"; //$NON-NLS-1$
	protected static final String SCOPE = "scope_baseline"; //$NON-NLS-1$
	
	/**
	 * The listing of component names to exclude from scanning
	 */
	protected Set excludeset = null;
	
	/**
	 * If api references should be considered in the search
	 */
	protected boolean considerapi = false;
	/**
	 * If internal references should be considered in the search
	 */
	protected boolean considerinternal = false;
	
	/**
	 * The location of the scope to search against
	 */
	protected String scopeLocation = null;
	
	/**
	 * If non- API enabled projects should be considered during the search
	 */
	protected boolean includenonapi = false;
	
	/**
	 * If system libraries should be included in the search scope and baseline
	 */
	protected boolean includesystemlibraries = false;
	
	/**
	 * If the scan should proceed if there are errors encountered
	 */
	protected boolean proceedonerror = false;
	
	/**
	 * Set of project names that were not searched
	 */
	protected TreeSet notsearched = null;
	
	/**
	 * Returns the search scope to use
	 * @param baseline
	 * @return the {@link IApiComponent} array to use for the search scope
	 * @throws CoreException
	 */
	protected IApiElement[] getScope(IApiBaseline baseline) throws CoreException {
		IApiComponent[] components = baseline.getApiComponents();
		TreeSet scope = new TreeSet(CommonUtilsTask.componentsorter);
		boolean isapibundle = false;
		boolean excluded = false;
		boolean resolveerrors = false;
		for(int i = 0; i < components.length; i++) {
			isapibundle = Util.isApiToolsComponent(components[i]);
			excluded = this.excludeset.contains(components[i].getId());
			resolveerrors = components[i].getErrors() != null;
			if((isapibundle || this.includenonapi) && !excluded && !components[i].isSystemComponent() && (!resolveerrors || this.proceedonerror)) {
				scope.add(components[i]);
			}
			else {
				notsearched.add(new SkippedComponent(components[i].getId(), !isapibundle, excluded, resolveerrors));
			}
		}
		return (IApiElement[]) scope.toArray(new IApiElement[scope.size()]);
	}
	
	/**
	 * Allows the raw list of components returned from the baseline to be altered as needed
	 * 
	 * @param baseline
	 * @return the accepted list of {@link IApiComponent}s from the given baseline
	 */
	protected Set getBaselineIds(IApiBaseline baseline) throws CoreException {
		IApiComponent[] components = baseline.getApiComponents();
		TreeSet comps = new TreeSet(componentsorter);
		for (int i = 0; i < components.length; i++) {
			if(!components[i].isSystemComponent()) {
				if(Util.isApiToolsComponent(components[i]) || this.includenonapi) {
					comps.add(components[i].getId());
				}
			}
			if(components[i].isSystemComponent() && this.includesystemlibraries) {
				comps.add(components[i].getId());
			}
		}
		return comps;
	}
	
	/**
	 * Returns the set of search flags to use for the {@link IApiSearchRequestor}
	 * 
	 * @return the set of flags to use
	 */
	protected int getSearchFlags() {
		int flags = (this.considerapi ? IApiSearchRequestor.INCLUDE_API : 0);
		flags |= (this.considerinternal ? IApiSearchRequestor.INCLUDE_INTERNAL : 0);
		flags |= (this.includenonapi ? IApiSearchRequestor.INCLUDE_NON_API_ENABLED_PROJECTS : 0);
		return flags;
	}
	
	/**
	 * Ensures that required task parameters are present
	 * @throws BuildException
	 */
	protected void assertParameters() throws BuildException {
		if (this.currentBaselineLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(Messages.bind(
					Messages.ApiUseTask_missing_baseline_argument, 
					new String[] {this.currentBaselineLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		//stop if we don't want to see anything
		if(!considerapi && !considerinternal) {
			throw new BuildException(Messages.UseTask_no_scan_both_types_not_searched_for);
		}
	}
	
	/**
	 * Returns the {@link IApiBaseline} to use for the search.
	 * Debugging information is written out if the debug flag is set.
	 * 
	 * @return the target baseline
	 */
	protected IApiBaseline getBaseline() {
		//extract the baseline to examine
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Preparing baseline installation..."); //$NON-NLS-1$
		}
		File baselineInstallDir = extractSDK(CURRENT, this.currentBaselineLocation);
		if (this.debug) {
			System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		//create the baseline to examine
		if(this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Creating API baseline..."); //$NON-NLS-1$
		}
		IApiBaseline baseline = createBaseline(CURRENT_PROFILE_NAME, getInstallDir(baselineInstallDir), this.eeFileLocation);
		if (this.debug) {
			System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		return baseline;
	}
	
	/**
	 * Returns the search scope {@link IApiBaseline} to use for the search
	 * Debugging information is written out if the debug flag is set.
	 * 
	 * @return the scope baseline
	 */
	protected IApiBaseline getScope() {
		//extract the scope to examine
		File scopeInstallDir = null;
		long time = 0;
		if(this.scopeLocation != null) {
			if (this.debug) {
				time = System.currentTimeMillis();
				System.out.println("Preparing scope..."); //$NON-NLS-1$
			}
			scopeInstallDir = extractSDK(SCOPE, this.scopeLocation);
			if (this.debug) {
				System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
		}
		//create the scope baseline
		IApiBaseline scopebaseline = null;
		if(scopeInstallDir != null) {
			if(this.debug) {
				time = System.currentTimeMillis();
				System.out.println("Creating scope baseline..."); //$NON-NLS-1$
			}
			scopebaseline = createBaseline("scope_baseline", getInstallDir(scopeInstallDir), this.eeFileLocation); //$NON-NLS-1$
			if (this.debug) {
				System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
		}
		return scopebaseline;
	}
	
	/**
	 * Performs the search
	 * @param baseline the baseline target
	 * @param scope the scope to search
	 * @param reporter the reporter to report to
	 * @throws CoreException
	 */
	protected void doSearch(IApiBaseline baseline, IApiBaseline scope, IApiSearchReporter reporter) throws CoreException {
		ApiSearchEngine engine = new ApiSearchEngine();
		IApiSearchRequestor requestor = new ApiUseSearchRequestor(
				getBaselineIds(baseline),
				getScope(scope == null ? baseline : scope), 
				getSearchFlags(), 
				(String[]) this.excludeset.toArray(new String[this.excludeset.size()]));
		ApiSearchEngine.setDebug(this.debug);
		engine.search(baseline, requestor, reporter, null);
	}
	
	/**
	 * Initializes the exclude set
	 * @param baseline
	 */
	protected void initializeExcludeSet(IApiBaseline baseline) {
		//initialize the exclude list
		this.excludeset = CommonUtilsTask.initializeRegexExcludeList(this.excludeListLocation, baseline);
		this.notsearched = new TreeSet(componentsorter);
		if(this.excludeset != null) {
			for(Iterator iter = this.excludeset.iterator(); iter.hasNext();) {
				this.notsearched.add(new SkippedComponent((String) iter.next(), false, true, false));
			}
		}
	}
	
	/**
	 * Cleans the report location specified by the parameter {@link CommonUtilsTask#reportLocation}
	 */
	protected void cleanReportLocation() {
		if(this.reportLocation == null) {
			return;
		}
		long time = 0;
		if(this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Cleaning report location..."); //$NON-NLS-1$
		}
		File file = new File(this.reportLocation);
		if(file.exists()) {
			scrubReportLocation(file);
		}
		if(this.debug) {
			System.out.println("done in: "+ (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Writes a general header of debug information iff the debug flag is set to true
	 */
	protected void writeDebugHeader() {
		if (this.debug) {
			System.out.println("Baseline to collect references to : " + this.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("Report location : " + this.reportLocation); //$NON-NLS-1$
			System.out.println("Searching for API references : " + this.considerapi); //$NON-NLS-1$
			System.out.println("Searching for internal references : " + this.considerinternal); //$NON-NLS-1$
			if(this.scopeLocation == null) {
				System.out.println("No scope specified : baseline will act as scope"); //$NON-NLS-1$
			}
			else {
				System.out.println("Scope to search against : " + this.scopeLocation); //$NON-NLS-1$
			}
			if (this.excludeListLocation != null) {
				System.out.println("Exclude list location : " + this.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
			if(this.eeFileLocation != null) {
				System.out.println("EE file location : " + this.eeFileLocation); //$NON-NLS-1$
			}
			else {
				System.out.println("No EE file location given: using default"); //$NON-NLS-1$
			}
			System.out.println("-----------------------------------------------------------------------------------------------------"); //$NON-NLS-1$
		}
	}
}
