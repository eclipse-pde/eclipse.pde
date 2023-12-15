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
