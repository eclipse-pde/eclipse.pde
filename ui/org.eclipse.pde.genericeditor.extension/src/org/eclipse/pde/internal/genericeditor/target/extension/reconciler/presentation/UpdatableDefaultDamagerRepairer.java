/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
