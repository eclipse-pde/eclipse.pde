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
import org.eclipse.swt.widgets.Widget;

public class WindowCommandTarget extends CommandTarget {
	/**
	 * @param widget
	 * @param context
	 */
	public WindowCommandTarget(Widget widget, Window window) {
		super(widget, window);
	}
	
	Window getWindow() {
		return (Window)getContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.CommandTarget#ensureVisible()
	 */
	public void ensureVisible() {
		Window window = getWindow();
		window.getShell().setActive();
	}
}