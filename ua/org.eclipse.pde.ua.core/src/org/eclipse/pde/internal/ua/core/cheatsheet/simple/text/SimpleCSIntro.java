/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSIntro#
	 * getDescription()
	 */
	public ISimpleCSDescription getDescription() {
		return (ISimpleCSDescription) getChildNode(ISimpleCSDescription.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSIntro#
	 * setDescription
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSDescription
	 * )
	 */
	public void setDescription(ISimpleCSDescription description) {
		setChildNode((IDocumentElementNode) description,
				ISimpleCSDescription.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * getContextId()
	 */
	public String getContextId() {
		return getXMLAttributeValue(ATTRIBUTE_CONTEXTID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * getHref()
	 */
	public String getHref() {
		return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		setXMLAttribute(ATTRIBUTE_CONTEXTID, contextId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * setHref(java.lang.String)
	 */
	public void setHref(String href) {
		setXMLAttribute(ATTRIBUTE_HREF, href);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject
	 * #getName ()
	 */
	public String getName() {
		return ELEMENT_INTRO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject
	 * #getType ()
	 */
	public int getType() {
		return TYPE_INTRO;
	}

}
