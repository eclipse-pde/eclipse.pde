/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Resources implements BundleActivator {

	private static ImageRegistry imageRegistry;

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
			ImageDescriptor fromURL = ImageDescriptor.createFromURL(Resources.class.getResource(key));
			registry.put(key, fromURL);
			return fromURL;
		}
		return descriptor;
	}

	public synchronized static Image getImage(String key) {
		ImageRegistry registry = getImageRegistry();
		getImageDescriptor(key); // make sure the descriptor is added!
		return registry.get(key);
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
	}

}
