/*******************************************************************************
 * Copyright (c) 2016, 2024 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 520004] autocomplete does not respect tag hierarchy
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Models the &lt;location&gt; nodes
 */
public class LocationNode extends Node {

	private List<String> repositoryLocations = new ArrayList<>();

	public List<String> getRepositoryLocations() {
		return repositoryLocations;
	}

	public void addRepositoryLocation(String repositoryLocation) {
		if (repositoryLocation != null) {
			this.repositoryLocations.add(repositoryLocation);
		}
	}

}
