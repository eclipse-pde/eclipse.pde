/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunObject;

public abstract class SimpleCSRunObject extends SimpleCSObject implements
		ISimpleCSRunObject {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param tagName
	 */
	public SimpleCSRunObject(ISimpleCSModel model, String tagName) {
		super(model, tagName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * getConfirm()
	 */
	public boolean getConfirm() {
		return getBooleanAttributeValue(ATTRIBUTE_CONFIRM, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * getRequired()
	 */
	public boolean getRequired() {
		return getBooleanAttributeValue(ATTRIBUTE_REQUIRED, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * getTranslate()
	 */
	public String getTranslate() {
		return getXMLAttributeValue(ATTRIBUTE_TRANSLATE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#getWhen
	 * ()
	 */
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		setBooleanAttributeValue(ATTRIBUTE_CONFIRM, confirm);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * setRequired(boolean)
	 */
	public void setRequired(boolean required) {
		setBooleanAttributeValue(ATTRIBUTE_REQUIRED, required);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#
	 * setTranslate(java.lang.String)
	 */
	public void setTranslate(String translate) {
		setXMLAttribute(ATTRIBUTE_TRANSLATE, translate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject#setWhen
	 * (java.lang.String)
	 */
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
	}

}
