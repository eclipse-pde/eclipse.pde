/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.*;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Page participant extension for an OSGi console. Contributes a close action.
 * 
 * @since 3.6
 */
public class OSGiConsolePageParticipant implements IConsolePageParticipant {

	private CloseConsoleAction fCloseAction;

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsolePageParticipant#init(org.eclipse.ui.part.IPageBookViewPage, org.eclipse.ui.console.IConsole)
	 */
	public void init(IPageBookViewPage page, IConsole console) {
		fCloseAction = new CloseConsoleAction(console);
		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, fCloseAction);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsolePageParticipant#dispose()
	 */
	public void dispose() {
		fCloseAction = null;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsolePageParticipant#activated()
	 */
	public void activated() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsolePageParticipant#deactivated()
	 */
	public void deactivated() {
	}

}
