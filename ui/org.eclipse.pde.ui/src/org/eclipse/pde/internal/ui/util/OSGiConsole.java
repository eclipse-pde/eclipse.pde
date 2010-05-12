/*******************************************************************************
 * Copyright (c) 2010 Red Hat and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     IBM Corporation - ongoing enhancements and bug fixing
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.osgi.framework.BundleContext;

/**
 * OSGi console connected to the Host/Running Eclipse.
 * 
 * @since 3.6
 */
public class OSGiConsole extends IOConsole {

	public final static String TYPE = "osgiConsole"; //$NON-NLS-1$
	private final ConsoleSession session;

	public OSGiConsole(final OSGiConsoleFactory factory) {
		super(NLS.bind(PDEUIMessages.OSGiConsole_name, Platform.getInstallLocation().getURL().getPath()), TYPE, null, true);
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
		super.init();
		IOConsoleOutputStream info = newOutputStream(); // create a stream to write info message to
		try {
			info.write(PDEUIMessages.OSGiConsoleFactory_title);
		} catch (IOException e) {
		} finally {
			try {
				info.close();
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
		}

		BundleContext context = PDEPlugin.getDefault().getBundle().getBundleContext();
		context.registerService(ConsoleSession.class.getName(), session, null);
	}

	protected void dispose() {
		super.dispose();
	}

}
