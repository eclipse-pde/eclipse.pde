/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.parts;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tools.layout.spy.internal.dialogs.LayoutSpyDialog;

import jakarta.annotation.PostConstruct;

/**
 * Hosts the layout spy as a part in the PDE spy window. The standalone command
 * remains available so the spy can still be opened on top of blocking dialogs.
 */
public class LayoutSpyPart {

	@PostConstruct
	public void createControls(Composite parent) {
		new LayoutSpyDialog(parent);
	}
}
