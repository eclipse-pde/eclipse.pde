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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	public Map<IApiBaseline, IApiProblem> mismatch = new ConcurrentHashMap<>();

	private static final IApiProblem NULL_PROBLEM = (IApiProblem) Proxy.newProxyInstance(
			IApiProblem.class.getClassLoader(), new Class<?>[] { IApiProblem.class },
			(InvocationHandler) (proxy, method, args) -> null);

	/**
	 * Constructor
	 */
	public WorkspaceBaseline() {
		super(ApiBaselineManager.WORKSPACE_API_BASELINE_ID);
	}

	@Override
	public void dispose() {
		throw new UnsupportedOperationException(
				"ApiBaselineManager.disposeWorkspaceBaseline() must be used to dispose the workspace baseline"); //$NON-NLS-1$
	}

	/**
	 * <b>Only public for technical reasons</b> do not call directly!
	 */
	public void disposeInternal() {
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
		IApiProblem problem = mismatch.get(b);
		return problem == NULL_PROBLEM ? null : problem;
	}

	public void putMismatchInfo(IApiBaseline baseline, IApiProblem problem) {
		if (problem == null) {
			mismatch.put(baseline, NULL_PROBLEM);
		} else {
			mismatch.put(baseline, problem);
		}
	}

	@Override
	public void addApiComponents(IApiComponent[] components) throws CoreException {
		if (isDisposed()) {
			return;
		}
		HashSet<String> ees = new HashSet<>();
		for (IApiComponent apiComponent : components) {
			BundleComponent component = (BundleComponent) apiComponent;
			if (component.isSourceComponent()) {
				continue;
			}
			addComponent(component);
			ees.addAll(component.getExecutionEnvironments());
		}
		resolveSystemLibrary(ees);
	}
}
