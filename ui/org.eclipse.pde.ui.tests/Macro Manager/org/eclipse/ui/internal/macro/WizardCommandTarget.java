/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.macro;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Widget;

public class WizardCommandTarget extends WindowCommandTarget {
	/**
	 * @param widget
	 * @param window
	 */
	public WizardCommandTarget(Widget widget, Window window) {
		super(widget, window);
	}

	public WizardDialog getWizardDialog() {
		return (WizardDialog)getWindow();
	}
}