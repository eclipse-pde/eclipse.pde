/*******************************************************************************
 * Copyright (c) 2010 Red Hat and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.ui.console.*;

public class OSGiConsoleFactory implements IConsoleFactory {
	private IConsoleManager fConsoleManager = null;

	public OSGiConsoleFactory() {
		fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
	 */
	public void openConsole() {
		openConsole(null);
	}

	public void openConsole(String initialText) {
		IOConsole fConsole = new OSGiConsole(this);
		fConsoleManager.addConsoles(new IConsole[] {fConsole});
		if (initialText != null) {
			fConsole.getDocument().set(initialText);
		}
		fConsoleManager.showConsoleView(fConsole);
	}

	void closeConsole(OSGiConsole console) {
		fConsoleManager.removeConsoles(new IConsole[] {console});
	}
}
