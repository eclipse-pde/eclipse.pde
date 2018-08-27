/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		fCloseAction = new CloseConsoleAction(console);
		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, fCloseAction);
	}

	@Override
	public void dispose() {
		fCloseAction = null;

	}

	@Override
	public void activated() {
	}

	@Override
	public void deactivated() {
	}

}
