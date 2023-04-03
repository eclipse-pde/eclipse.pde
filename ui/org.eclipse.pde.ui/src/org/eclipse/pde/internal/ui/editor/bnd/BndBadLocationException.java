/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import org.eclipse.jface.text.BadLocationException;

/**
 * this wraps the jface-text BadLocationException into a bnd-document one
 */
final class BndBadLocationException extends aQute.bnd.properties.BadLocationException {

	private static final long serialVersionUID = 1L;

	public BndBadLocationException(BadLocationException wrapped) {
		super(wrapped.getMessage());
		initCause(wrapped);
	}

}
