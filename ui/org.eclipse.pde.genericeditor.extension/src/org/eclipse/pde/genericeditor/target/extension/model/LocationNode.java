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
 * Models the &ltlocation&gt nodes
 *
 */
public class LocationNode extends Node {

	private List<UnitNode> units = new ArrayList<>();
	private String repositoryLocation;

	public String getRepositoryLocation() {
		return repositoryLocation;
	}

	public void setRepositoryLocation(String repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
	}

	public void addUnitNode(UnitNode unit) {
		unit.setParent(this);
		units.add(unit);
	}

	public void removeUnitNode(UnitNode unit) {
		units.remove(unit);
		unit.setParent(null);
	}

	public List<UnitNode> getUnits() {
		return units;
	}

}
