/*******************************************************************************
 *  Copyright (c) 2023, 2024 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.bnd.ui;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Manages resources that are bound to the life-cycle of the bundle like images
 * or threads
 */
public class Resources implements BundleActivator {

	private static ImageRegistry imageRegistry;

	private static ScheduledExecutorService scheduler;

	@Override
	public void start(BundleContext context) throws Exception {

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		disposeResources();
	}

	public synchronized static ImageDescriptor getImageDescriptor(String key) {
		ImageRegistry registry = getImageRegistry();
		ImageDescriptor descriptor = registry.getDescriptor(key);
		if (descriptor == null) {
			ImageDescriptor fromURL = ImageDescriptor.createFromURL(getImageUrl(key));
			registry.put(key, fromURL);
			return fromURL;
		}
		return descriptor;
	}

	private static URL getImageUrl(String key) {
		URL resource = Resources.class.getResource(key);
		if (resource != null) {
			return resource;
		}
		resource = Resources.class.getResource("/icons/" + key);
		if (resource != null) {
			return resource;
		}
		resource = Resources.class.getResource("/icons/" + key + ".svg");
		if (resource != null) {
			return resource;
		}
		resource = Resources.class.getResource("/icons/" + key + ".gif");
		if (resource != null) {
			return resource;
		}
		resource = Resources.class.getResource("/icons/" + key + ".png");
		if (resource != null) {
			return resource;
		}
		return null;
	}

	public synchronized static Image getImage(String key) {
		if (key.startsWith("$")) {
			try {
				return PlatformUI.getWorkbench().getSharedImages().getImage(key);
			} catch (IllegalStateException e) {
				// don't care then...
			}
		}
		ImageRegistry registry = getImageRegistry();
		getImageDescriptor(key); // make sure the descriptor is added!
		return registry.get(key);
	}

	public synchronized static ScheduledExecutorService getScheduler() {
		if (scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(4);
		}
		return scheduler;
	}

	private synchronized static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry(Display.getCurrent());
		}
		return imageRegistry;
	}

	private synchronized static void disposeResources() {
		if (imageRegistry != null) {
			imageRegistry.dispose();
			imageRegistry = null;
		}
		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
	}

}
