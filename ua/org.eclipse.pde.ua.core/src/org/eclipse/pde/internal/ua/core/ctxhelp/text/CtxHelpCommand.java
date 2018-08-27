/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.core.ctxhelp.text;

/**
 * Represents a command entry in context help. Commands are used to launch
 * actions when clicked on by the user. Commands are leaf objects.
 *
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public class CtxHelpCommand extends CtxHelpObject {

	private static final long serialVersionUID = 1L;

	public CtxHelpCommand(CtxHelpModel model) {
		super(model, ELEMENT_COMMAND);
	}

	@Override
	public boolean canBeParent() {
		return false;
	}

	@Override
	public int getType() {
		return TYPE_COMMAND;
	}

	@Override
	public String getName() {
		return getLabel();
	}

	/**
	 * @return the value of the label attribute or <code>null</code> if the
	 *         attribute does not exist
	 */
	public String getLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	/**
	 * Set the label attribute, passing <code>null</code> will set the attribute
	 * to be empty.
	 *
	 * @param label
	 *            new value
	 */
	public void setLabel(String label) {
		setXMLAttribute(ATTRIBUTE_LABEL, label);
	}

	/**
	 * @return the value of the serialization attribute or <code>null</code> if
	 *         the attribute does not exist
	 */
	public String getSerialization() {
		return getXMLAttributeValue(ATTRIBUTE_SERIAL);
	}

	/**
	 * Set the serialization attribute, passing <code>null</code> will set the
	 * attribute to be empty.
	 *
	 * @param serialization
	 *            new value
	 */
	public void setSerialization(String serialization) {
		setXMLAttribute(ATTRIBUTE_SERIAL, serialization);
	}

	@Override
	public boolean canAddChild(int objectType) {
		return false;
	}

	@Override
	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_COMMAND || objectType == TYPE_TOPIC;
	}
}
