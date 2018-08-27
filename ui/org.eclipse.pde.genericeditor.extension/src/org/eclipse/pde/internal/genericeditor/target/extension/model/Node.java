/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
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
import java.util.stream.Collectors;

/**
 * Base class for model nodes in a target definition.
 */
public class Node {

	private int offsetStart;
	private int offsetEnd;
	private String nodeTag;
	private List<Node> childNodes;
	private Node parentNode;

	public int getOffsetStart() {
		return offsetStart;
	}

	public void setOffsetStart(int offsetStart) {
		this.offsetStart = offsetStart;
	}

	public int getOffsetEnd() {
		return offsetEnd;
	}

	public void setOffsetEnd(int offsetEnd) {
		this.offsetEnd = offsetEnd;
	}

	public String getNodeTag() {
		return nodeTag;
	}

	public void setNodeTag(String nodeTag) {
		this.nodeTag = nodeTag;
	}

	public List<Node> getChildNodes() {
		return childNodes;
	}

	public List<Node> getChildNodesByTag(String nodeTag) {
		return childNodes.stream().filter(n -> n.getNodeTag().equals(nodeTag)).collect(Collectors.toList());
	}

	public void addChildNode(Node child) {
		if (childNodes == null) {
			childNodes = new ArrayList<>();
		}
		childNodes.add(child);
		child.setParentNode(this);
	}

	public Node getParentNode() {
		return parentNode;
	}

	private void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}
}
