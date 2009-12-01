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
package org.eclipse.pde.api.tools.internal.model;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.internal.core.PDECore;


/**
 * Describes the workspace baseline. Tracks the PDE model for the workspace
 * 
 * @since 1.1
 */
public class WorkspaceBaseline extends ApiBaseline {
	
	/**
	 * Constructor
	 */
	public WorkspaceBaseline() {
		super(ApiBaselineManager.WORKSPACE_API_BASELINE_ID);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiBaseline#dispose()
	 */
	public void dispose() {
		doDispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiBaseline#getState()
	 */
	public State getState() {
		return PDECore.getDefault().getModelManager().getState().getState();
	}
	
	/* (non-Javadoc)
	 * @see IApiBaseline#addApiComponents(org.eclipse.pde.api.tools.model.component.IApiComponent[], boolean)
	 */
	public void addApiComponents(IApiComponent[] components) throws CoreException {
		HashSet ees = new HashSet();
		for (int i = 0; i < components.length; i++) {
			BundleComponent component = (BundleComponent) components[i];
			if (component.isSourceComponent()) {
				continue;
			}
			addComponent(component);
			ees.addAll(Arrays.asList(component.getExecutionEnvironments()));
		}
		resolveSystemLibrary(ees);
	}
}
