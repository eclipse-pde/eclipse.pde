/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/*
	 * @see IInformationControl#dispose()
	 */
	public void dispose() {
		fDisposed = true;
		super.dispose();
	}

	public boolean isDisposed() {
		return fDisposed;
	}

}
