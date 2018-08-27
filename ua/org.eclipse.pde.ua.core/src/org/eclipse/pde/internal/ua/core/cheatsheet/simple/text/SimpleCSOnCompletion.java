/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSOnCompletion;

public class SimpleCSOnCompletion extends SimpleCSObject implements
		ISimpleCSOnCompletion {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSOnCompletion(ISimpleCSModel model) {
		super(model, ELEMENT_ONCOMPLETION);
	}

	@Override
	public String getContent() {
		return getXMLContent();
	}

	@Override
	public void setContent(String content) {
		setXMLContent(content);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		return ELEMENT_ONCOMPLETION;
	}

	@Override
	public int getType() {
		return TYPE_ON_COMPLETION;
	}

	@Override
	public boolean isContentCollapsed() {
		return true;
	}

}
