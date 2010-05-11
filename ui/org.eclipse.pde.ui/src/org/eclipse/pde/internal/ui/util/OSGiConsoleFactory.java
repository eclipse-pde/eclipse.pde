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

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.console.*;

public class OSGiConsoleFactory implements IConsoleFactory {
	private IConsoleManager fConsoleManager = null;
	private IOConsole fConsole = null;

	public OSGiConsoleFactory() {
		fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
		fConsole = new OSGiConsole(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
	 */
	public void openConsole() {
		openConsole(PDEUIMessages.OSGiConsoleFactory_title);
	}

	public void openConsole(String initialText) {
		if (fConsole != null) {
			IConsole[] existing = fConsoleManager.getConsoles();
			boolean exists = false;
			for (int i = 0; i < existing.length; i++) {
				if (fConsole == existing[i])
					exists = true;
			}
			if (!exists)
				fConsoleManager.addConsoles(new IConsole[] {fConsole});
			fConsoleManager.showConsoleView(fConsole);
			fConsole.getDocument().set(initialText);
		}
		fConsoleManager.addConsoles(new IConsole[] {fConsole});
	}

	void closeConsole(OSGiConsole console) {
		fConsoleManager.removeConsoles(new IConsole[] {console});
	}
}
