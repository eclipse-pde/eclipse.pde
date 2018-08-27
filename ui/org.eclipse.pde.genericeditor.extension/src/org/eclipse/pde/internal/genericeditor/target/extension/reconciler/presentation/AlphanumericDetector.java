/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import org.eclipse.jface.text.rules.IWordDetector;

public class AlphanumericDetector implements IWordDetector {

	@Override
	public boolean isWordStart(char c) {
		return Character.isAlphabetic(c);
	}

	@Override
	public boolean isWordPart(char c) {
		return Character.isAlphabetic(c);
	}

}
