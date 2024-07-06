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
import java.util.Arrays;
import java.util.List;

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

	public void addExecutionEnvironment(String eeId) {
		addManifestElement(new ExecutionEnvironment(this, eeId));
	}

	public void addExecutionEnvironment(ExecutionEnvironment environment, int index) {
		addManifestElement(environment, index, true);
	}

	public void addExecutionEnvironments(List<String> eeIDs) {
		List<ExecutionEnvironment> list = new ArrayList<>(eeIDs.size());
		for (String eeID : eeIDs) {
			if (!hasElement(eeID)) {
				list.add(new ExecutionEnvironment(this, eeID));
			}
		}
		if (!list.isEmpty()) {
			addManifestElements(list);
		}
	}

	public ExecutionEnvironment removeExecutionEnvironment(String eeId) {
		return (ExecutionEnvironment) removeManifestElement(eeId);
	}

	public List<String> getEnvironments() {
		PDEManifestElement[] elements = getElements();
		return Arrays.stream(elements).map(PDEManifestElement::getValue).toList();
	}

}
