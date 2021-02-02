/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.internal.core.PDECore;

/**
 * Describes the workspace baseline. Tracks the PDE model for the workspace
 *
 * @since 1.1
 */
public class WorkspaceBaseline extends ApiBaseline {

	// If workspace is changed, then WorkspaceBaseline is disposed and a
	// new WorkspaceBaseline is created, hence mismatch problem can be stored
	// with a workspace baseline
	public HashMap<IApiBaseline, IApiProblem> mismatch = new HashMap<>();
	/**
	 * Constructor
	 */
	public WorkspaceBaseline() {
		super(ApiBaselineManager.WORKSPACE_API_BASELINE_ID);
	}

	@Override
	public void dispose() {
		doDispose();
		mismatch.clear();
	}

	@Override
	public State getState() {
		return PDECore.getDefault().getModelManager().getState().getState();
	}


	public boolean containsBaseline(IApiBaseline b) {
		return mismatch.containsKey(b);
	}

	// can be null showing no problem
	public IApiProblem getProblem(IApiBaseline b) {
		return mismatch.get(b);
	}

	public void putMismatchInfo(IApiBaseline baseline, IApiProblem problem) {
		mismatch.put(baseline, problem);

	}

	@Override
	public void addApiComponents(IApiComponent[] components) throws CoreException {
		HashSet<String> ees = new HashSet<>();
		for (IApiComponent apiComponent : components) {
			BundleComponent component = (BundleComponent) apiComponent;
			if (component.isSourceComponent()) {
				continue;
			}
			addComponent(component);
			ees.addAll(Arrays.asList(component.getExecutionEnvironments()));
		}
		resolveSystemLibrary(ees);
	}
}
