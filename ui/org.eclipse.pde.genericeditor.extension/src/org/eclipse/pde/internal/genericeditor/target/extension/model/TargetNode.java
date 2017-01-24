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
package org.eclipse.pde.internal.genericeditor.target.extension.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root node model of the target definition. Matches the &lttarget&gt tag.
 */
public class TargetNode extends Node {

	private List<LocationNode> nodes = new ArrayList<LocationNode>();

	/**
	 * Convenience method used to get the model {@link UnitNode} enclosing a
	 * given offset. Useful when computing e.g. available versions.
	 *
	 * @param offset
	 * @return
	 */
	public UnitNode getEnclosingUnit(int offset) {

		for (LocationNode node : nodes) {
			List<UnitNode> units = node.getUnits();
			for (UnitNode unit : units) {
				if ((offset >= unit.getOffsetStart()) && (offset < unit.getOffsetEnd())) {
					return unit;
				}
			}
		}
		return null;
	}

	public List<LocationNode> getNodes() {
		return nodes;
	}

}
