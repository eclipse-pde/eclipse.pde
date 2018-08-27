/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
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
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class modeling the &ltunit&gt tag in a target definition.
 */
public class UnitNode extends Node {

	private String id;
	private String version;
	private List<String> availableVersions = new ArrayList<>();
	private LocationNode parent;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public LocationNode getParent() {
		return parent;
	}

	public void setParent(LocationNode parent) {
		this.parent = parent;
	}

	public List<String> getAvailableVersions() {
		return availableVersions;
	}

	public void setAvailableVersions(List<String> availableVersions) {
		this.availableVersions = availableVersions;
	}

}
