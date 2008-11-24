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
package org.eclipse.pde.api.tools.internal.provisional.builder;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.builder.BuildState;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Description of an analyzer used by the API builder to find and report
 * problems with the current API state.
 * 
 * @see ApiAnalysisBuilder
 * @see IApiProblem
 * @since 1.0.0
 */
public interface IApiAnalyzer {

	/**
	 * Analyzes a given {@link IApiComponent} for API problems.
	 * <p>The component is guaranteed to not be <code>null</code> and to be 
	 * up-to-date in the API description it belongs to.</p>
	 * <p>If the baseline is null, no analysis is done.</p>
	 * <p>The given <code>preferences</code> are used when the platform is not running. When the platform is running,
	 * the references are retrieved using the preference store.</p>
	 *
	 * @param buildState the given build state or null if none
	 * @param filterStore the given filter store or null if none
	 * @param preferences the given preferences to be used for the analysis
	 * @param baseline the profile context to check the component against
	 * @param component the component to analyze
	 * @param typenames the context of type names to analyze within the given component
	 * @param changedtypes the context of type names for actual types that had changes in them (raw type names from a build delta)
	 * @param monitor to report progress
	 * @see PluginProjectApiComponent
	 * @see BundleApiComponent
	 */
	public void analyzeComponent(final BuildState buildState, final IApiFilterStore filterStore, final Properties preferences, final IApiBaseline baseline, final IApiComponent component, final String[] typenames, final String[] changedtypes, IProgressMonitor monitor);
	
	/**
	 * Returns the complete set of {@link IApiProblem}s found by this analyzer, or an empty
	 * array. This method must never return <code>null</code>
	 * 
	 * @return the complete set of problems found by this analyzer or an empty array.
	 */
	public IApiProblem[] getProblems();
	
	/**
	 * Cleans up and disposes this analyzer, freeing all held memory.
	 * Once the analyzer has been disposed it cannot be used again without 
	 * specifying a new reporter to use.
	 */
	public void dispose();
	
}
