/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * This filter store is only used to filter problem using existing filters. It
 * doesn't add or remove any filters.
 */
public class AntFilterStore extends FilterStore {

	String fComponentId = null;
	String fFiltersRoot = null;
	public static ArrayList<IApiProblem> filteredAPIProblems = new ArrayList<>();
	/**
	 * Constructor
	 *
	 * @param debug
	 * @param filtersRoot
	 * @param componentID
	 */
	public AntFilterStore(String filtersRoot, String componentID) {
		fComponentId = componentID;
		fFiltersRoot = filtersRoot;
	}

	@Override
	public boolean isFiltered(IApiProblem problem) {
		boolean isFiltered = super.isFiltered(problem);
		if(isFiltered){
			filteredAPIProblems.add(problem);
		}
		return isFiltered;
	}

	@Override
	protected synchronized void initializeApiFilters() {
		if (fFilterMap != null) {
			return;
		}
		fFilterMap = new HashMap<>(5);
		InputStream contents = null;
		File filterFileParent = new File(fFiltersRoot, fComponentId);
		try {
			if (!filterFileParent.exists()) {
				IApiBaseline base = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
				if (base != null) {
					IApiComponent apiComponent = base.getApiComponent(fComponentId);
					if (apiComponent != null && apiComponent.isFragment()) {
						filterFileParent = new File(fFiltersRoot, apiComponent.getHost().getSymbolicName());
					}
				}
				if (!filterFileParent.exists()) {
					return;
				}
			}
			contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
			readFilterFile(contents);
		} catch (IOException | CoreException ioe) {
			ApiPlugin.log("Failed to read filter file " + filterFileParent, ioe); //$NON-NLS-1$
		} finally {
			if (contents != null) {
				try {
					contents.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Internal use method that allows auto-persisting of the filter file to be
	 * turned on or off
	 *
	 * @param problems the problems to add the the store
	 * @param persist if the filters should be auto-persisted after they are
	 *            added
	 */
	@Override
	protected void internalAddFilters(IApiProblem[] problems, String[] comments) {
		if (problems == null || problems.length == 0) {
			return;
		}
		// This filter store doesn't handle resources so all filters are added
		// to GLOBAL
		Set<IApiProblemFilter> globalFilters = fFilterMap.get(GLOBAL);
		if (globalFilters == null) {
			globalFilters = new HashSet<>();
			fFilterMap.put(GLOBAL, globalFilters);
		}

		for (int i = 0; i < problems.length; i++) {
			IApiProblem problem = problems[i];
			String comment = comments != null ? comments[i] : null;
			IApiProblemFilter filter = new ApiProblemFilter(fComponentId, problem, comment);
			globalFilters.add(filter);
		}
	}

}