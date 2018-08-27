/*******************************************************************************
 * Copyright (c) 2010, 2015 Red Hat and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     IBM Corporation - ongoing enhancements and bug fixes
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.ui.console.*;

/**
 * Console factory extension used to create a "Host OSGi Console".
 *
 * @since 3.6
 */
public class OSGiConsoleFactory implements IConsoleFactory {
	private final IConsoleManager fConsoleManager;
	private IOConsole fConsole = null;

	public OSGiConsoleFactory() {
		fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
	}

	@Override
	public void openConsole() {
		IOConsole console = getConsole();

		IConsole[] existing = fConsoleManager.getConsoles();
		boolean exists = false;
		for (IConsole existingConsole : existing) {
			if (console == existingConsole)
				exists = true;
		}
		if (!exists)
			fConsoleManager.addConsoles(new IConsole[] {console});
		fConsoleManager.showConsoleView(console);
	}

	private synchronized IOConsole getConsole() {
		if (fConsole != null)
			return fConsole;
		fConsole = new OSGiConsole(this);
		return fConsole;
	}

	void closeConsole(OSGiConsole console) {
		synchronized (this) {
			if (console != fConsole)
				throw new IllegalArgumentException("Wrong console instance!"); //$NON-NLS-1$
			fConsole = null;
		}
		fConsoleManager.removeConsoles(new IConsole[] {console});
	}
}
