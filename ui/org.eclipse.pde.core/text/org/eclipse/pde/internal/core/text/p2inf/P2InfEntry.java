/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.p2inf;

/**
 * A single entry of a p2.inf file, identified by its key, together with the
 * offset in the document where the entry starts.
 */
public class P2InfEntry {

	private final String fName;
	private int fOffset = -1;

	public P2InfEntry(String name) {
		fName = name;
	}

	public String getName() {
		return fName;
	}

	/**
	 * @return the document offset of the first character of the key or -1 if
	 *         the entry could not be located in the document
	 */
	public int getOffset() {
		return fOffset;
	}

	void setOffset(int offset) {
		fOffset = offset;
	}
}
