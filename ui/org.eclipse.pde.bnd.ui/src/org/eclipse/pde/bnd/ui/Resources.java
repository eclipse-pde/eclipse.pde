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

	public synchronized static Image getImage(String key) {
		ImageRegistry registry = getImageRegistry();
		if (registry.getDescriptor(key) == null) {
			registry.put(key, ImageDescriptor.createFromURL(Resources.class.getResource(key)));
		}
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
