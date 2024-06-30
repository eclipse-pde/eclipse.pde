/*******************************************************************************
 *  Copyright (c) 2005, 2024 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;

public class RequiredExecutionEnvironmentHeader extends CompositeManifestHeader {

	private static final long serialVersionUID = 1L;

	public RequiredExecutionEnvironmentHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	@Override
	protected PDEManifestElement createElement(ManifestElement element) {
		return new ExecutionEnvironment(this, element.getValue());
	}

	public void addExecutionEnvironment(IExecutionEnvironment env) {
		addManifestElement(new ExecutionEnvironment(this, env.getId()));
	}

	public void addExecutionEnvironment(ExecutionEnvironment environment, int index) {
		addManifestElement(environment, index, true);
	}

	public void addExecutionEnvironments(Object[] envs) {
		List<ExecutionEnvironment> list = new ArrayList<>(envs.length);
		for (Object envObject : envs) {
			ExecutionEnvironment env = null;
			if (envObject instanceof ExecutionEnvironment ee) {
				env = ee;
			} else if (envObject instanceof IExecutionEnvironment ee) {
				env = new ExecutionEnvironment(this, ee.getId());
			}
			if (env != null && !hasElement(env.getName())) {
				list.add(env);
			}
		}

		if (!list.isEmpty()) {
			addManifestElements(list.toArray(new ExecutionEnvironment[list.size()]));
		}
	}

	public ExecutionEnvironment removeExecutionEnvironment(ExecutionEnvironment env) {
		return (ExecutionEnvironment) removeManifestElement(env);
	}

	/**
	 * Remove operation performed using the actual object rather than its value
	 */
	public ExecutionEnvironment removeExecutionEnvironmentUnique(ExecutionEnvironment environment) {
		return (ExecutionEnvironment) removeManifestElement(environment, true);
	}

	public ExecutionEnvironment[] getEnvironments() {
		PDEManifestElement[] elements = getElements();
		ExecutionEnvironment[] result = new ExecutionEnvironment[elements.length];
		System.arraycopy(elements, 0, result, 0, elements.length);
		return result;
	}

}
