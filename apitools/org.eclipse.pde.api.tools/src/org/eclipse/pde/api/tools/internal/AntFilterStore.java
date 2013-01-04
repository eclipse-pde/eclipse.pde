/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;


/**
 * This filter store is only used to filter problem using existing filters.
 * It doesn't add or remove any filters.
 */
public class AntFilterStore extends FilterStore {

	String fComponentId = null;
	String fFiltersRoot = null;
	
	/**
	 * Constructor
	 * @param debug
	 * @param filtersRoot
	 * @param componentID
	 */
	public AntFilterStore(String filtersRoot, String componentID) {
		fComponentId = componentID;
		fFiltersRoot = filtersRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.FilterStore#initializeApiFilters()
	 */
	protected synchronized void initializeApiFilters() {
		if(fFilterMap != null) {
			return;
		}
		fFilterMap = new HashMap(5);
		InputStream contents = null;
		try {
			File filterFileParent = new File(fFiltersRoot, fComponentId);
			if (!filterFileParent.exists()) {
				return;
			}
			contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
			readFilterFile(contents);
		}
		catch(IOException ioe) {
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
	}
	
	/**
	 * Internal use method that allows auto-persisting of the filter file to be turned on or off
	 * @param problems the problems to add the the store
	 * @param persist if the filters should be auto-persisted after they are added
	 */
	protected void internalAddFilters(IApiProblem[] problems, String[] comments) {
		if(problems == null || problems.length == 0) {
			return;
		}
		// This filter store doesn't handle resources so all filters are added to GLOBAL
		Set globalFilters = (Set) fFilterMap.get(GLOBAL);
		if(globalFilters == null) {
			globalFilters = new HashSet();
			fFilterMap.put(GLOBAL, globalFilters);
		}
		
		for(int i = 0; i < problems.length; i++) {
			IApiProblem problem = problems[i];
			String comment = comments != null ? comments[i] : null;
			IApiProblemFilter filter = new ApiProblemFilter(fComponentId, problem, comment);
			globalFilters.add(filter);
		}
	}
	
}