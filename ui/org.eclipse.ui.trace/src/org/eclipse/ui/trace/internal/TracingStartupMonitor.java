/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.osgi.framework.ServiceRegistration;

/**
 * A {@link StartupMonitor} implementation for initializing the tracing preferences after the application has started.
 */
public class TracingStartupMonitor implements StartupMonitor {

	private ServiceRegistration<StartupMonitor> registration;

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.runnable.StartupMonitor#update()
	 */
	public void update() {

		// empty implementation
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.runnable.StartupMonitor#applicationRunning()
	 */
	public void applicationRunning() {

		// bug 395632: The application is running now so it's safe to initialize the preferences
		TracingUIActivator.getDefault().initPreferences();
		// Unregister this service as its purpose is complete
		registration.unregister();

	}

	/**
	 * Set the service registration on this monitor so it can unregister itself after {@link #applicationRunning()}
	 * @param registration the service registration returned when registering a {@link StartupMonitor} service or <code>null</code>
	 */
	public void setRegistration(ServiceRegistration<StartupMonitor> registration) {
		this.registration = registration;
	}
}