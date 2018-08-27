/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;

public class UpdatableDefaultDamagerRepairer extends DefaultDamagerRepairer {

	public UpdatableDefaultDamagerRepairer(ITokenScanner scanner) {
		super(scanner);
	}

	public void updateTokenScanner(ITokenScanner scanner) {
		if(scanner != null) {
			fScanner = scanner;
		}
	}

}
