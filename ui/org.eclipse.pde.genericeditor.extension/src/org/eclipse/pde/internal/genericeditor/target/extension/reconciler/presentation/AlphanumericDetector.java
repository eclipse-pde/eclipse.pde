/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
