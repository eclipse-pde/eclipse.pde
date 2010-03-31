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

import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.console.IOConsole;

public class OSGiConsole extends IOConsole {

	public final static String TYPE = "osgiConsole"; //$NON-NLS-1$
	private final ConsoleSession session;

	public OSGiConsole(final OSGiConsoleFactory factory) {
		super(PDEUIMessages.OSGiConsole_name, TYPE, null, true);
		session = new ConsoleSession() {

			public OutputStream getOutput() {
				return newOutputStream();
			}

			public InputStream getInput() {
				return getInputStream();
			}

			protected void doClose() {
				factory.closeConsole(OSGiConsole.this);
			}
		};
	}

	protected void init() {
		PDEPlugin.getDefault().getBundle().getBundleContext().registerService(ConsoleSession.class.getName(), session, null);
		super.init();
	}

	protected void dispose() {
		super.dispose();
	}

}
