/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.swt.widgets.Shell;

public class PDEDefaultInformationControl extends DefaultInformationControl {

	private boolean fDisposed = false;

	public PDEDefaultInformationControl(Shell parent, String tooltipAffordanceString) {
		super(parent, tooltipAffordanceString);
	}

	@Override
	public void dispose() {
		fDisposed = true;
		super.dispose();
	}

	public boolean isDisposed() {
		return fDisposed;
	}

}
