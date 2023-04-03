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

import org.eclipse.jface.text.IRegion;

/**
 * Wraps a JFace-text region as a bnd-document-region
 */
final class BndRegion implements aQute.bnd.properties.IRegion {

	private final IRegion textRegion;

	public BndRegion(IRegion region) {
		this.textRegion = region;
	}

	@Override
	public int getLength() {
		return textRegion.getLength();
	}

	@Override
	public int getOffset() {
		return textRegion.getOffset();
	}
}