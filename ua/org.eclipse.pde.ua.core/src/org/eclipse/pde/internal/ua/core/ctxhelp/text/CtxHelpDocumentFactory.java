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

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.ctxhelp.ICtxHelpConstants;

/**
 * Handles the creation of document nodes representing the types of elements
 * that can exist in a context help xml file.
 *
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentHandler
 */
public class CtxHelpDocumentFactory extends DocumentNodeFactory {
	private CtxHelpModel fModel;

	public CtxHelpDocumentFactory(CtxHelpModel model) {
		fModel = model;
	}

	@Override
	public IDocumentElementNode createDocumentNode(String name,
			IDocumentElementNode parent) {
		if (isRoot(name)) { // Root
			return createRoot();
		}
		if (isContext(name)) { // Context
			return createContext();
		}
		if (isDescription(name)) { // Link
			return createDescription();
		}
		if (isTopic(name)) { // Anchor
			return createTopic();
		}
		if (isCommand(name)) { // Anchor
			return createCommand();
		}
		return super.createDocumentNode(name, parent);
	}

	private boolean isRoot(String name) {
		return name.equals(ICtxHelpConstants.ELEMENT_ROOT);
	}

	private boolean isContext(String name) {
		return name.equals(ICtxHelpConstants.ELEMENT_CONTEXT);
	}

	private boolean isDescription(String name) {
		return name.equals(ICtxHelpConstants.ELEMENT_DESCRIPTION);
	}

	private boolean isTopic(String name) {
		return name.equals(ICtxHelpConstants.ELEMENT_TOPIC);
	}

	private boolean isCommand(String name) {
		return name.equals(ICtxHelpConstants.ELEMENT_COMMAND);
	}

	/**
	 * @return a new root object based on the current model
	 */
	public CtxHelpRoot createRoot() {
		return new CtxHelpRoot(fModel);
	}

	/**
	 * @return a new context object based on the current model
	 */
	public CtxHelpContext createContext() {
		return new CtxHelpContext(fModel);
	}

	/**
	 * @return a new description object based on the current model
	 */
	public CtxHelpDescription createDescription() {
		return new CtxHelpDescription(fModel);
	}

	/**
	 * @return a new topic object based on the current model
	 */
	public CtxHelpTopic createTopic() {
		return new CtxHelpTopic(fModel);
	}

	/**
	 * @return a new command object based on the current model
	 */
	public CtxHelpCommand createCommand() {
		return new CtxHelpCommand(fModel);
	}

}
