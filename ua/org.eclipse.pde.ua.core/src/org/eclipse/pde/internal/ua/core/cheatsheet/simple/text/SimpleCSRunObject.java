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

	@Override
	public boolean getConfirm() {
		return getBooleanAttributeValue(ATTRIBUTE_CONFIRM, false);
	}

	@Override
	public boolean getRequired() {
		return getBooleanAttributeValue(ATTRIBUTE_REQUIRED, true);
	}

	@Override
	public String getTranslate() {
		return getXMLAttributeValue(ATTRIBUTE_TRANSLATE);
	}

	@Override
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	@Override
	public void setConfirm(boolean confirm) {
		setBooleanAttributeValue(ATTRIBUTE_CONFIRM, confirm);
	}

	@Override
	public void setRequired(boolean required) {
		setBooleanAttributeValue(ATTRIBUTE_REQUIRED, required);
	}

	@Override
	public void setTranslate(String translate) {
		setXMLAttribute(ATTRIBUTE_TRANSLATE, translate);
	}

	@Override
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
	}

}
