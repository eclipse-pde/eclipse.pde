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

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Abstract class describing a use task
 */
public class UseTask extends CommonUtilsTask {
	
	protected static final String NO_API_DESCRIPTION = "no_description"; //$NON-NLS-1$
	protected static final String EXCLUDED = "excluded"; //$NON-NLS-1$
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
	 * if non- API enabled projects should be allowed in the search scope
	 */
	protected boolean includenonapi = false;
	
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
		for(int i = 0; i < components.length; i++) {
			isapibundle = Util.isApiToolsComponent(components[i]);
			excluded = this.excludeset.contains(components[i].getId());
			if((isapibundle || this.includenonapi) && !excluded) {
				scope.add(components[i]);
			}
			else {
				notsearched.add(new SkippedComponent(components[i].getId(), !isapibundle, excluded));
			}
		}
		return (IApiElement[]) scope.toArray(new IApiElement[scope.size()]);
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
}
