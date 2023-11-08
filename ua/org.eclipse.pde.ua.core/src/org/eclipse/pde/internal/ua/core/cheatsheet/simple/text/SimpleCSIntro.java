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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;

public class SimpleCSIntro extends SimpleCSObject implements ISimpleCSIntro {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSIntro(ISimpleCSModel model) {
		super(model, ELEMENT_INTRO);
	}

	@Override
	public ISimpleCSDescription getDescription() {
		return (ISimpleCSDescription) getChildNode(ISimpleCSDescription.class);
	}

	@Override
	public void setDescription(ISimpleCSDescription description) {
		setChildNode(description,
				ISimpleCSDescription.class);
	}

	@Override
	public String getContextId() {
		return getXMLAttributeValue(ATTRIBUTE_CONTEXTID);
	}

	@Override
	public String getHref() {
		return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	@Override
	public void setContextId(String contextId) {
		setXMLAttribute(ATTRIBUTE_CONTEXTID, contextId);
	}

	@Override
	public void setHref(String href) {
		setXMLAttribute(ATTRIBUTE_HREF, href);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		return ELEMENT_INTRO;
	}

	@Override
	public int getType() {
		return TYPE_INTRO;
	}

}
