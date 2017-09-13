/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 520004] autocomplete does not respect tag hierarchy
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

/**
 * Models the &ltlocation&gt nodes
 *
 */
public class LocationNode extends Node {

	private String repositoryLocation;

	public String getRepositoryLocation() {
		return repositoryLocation;
	}

	public void setRepositoryLocation(String repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
	}

}
