/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.target.extension.model;

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
