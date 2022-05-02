package b;

import org.osgi.framework.BundleContext;

import a.Application;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator {
	/**
	 * The constructor
	 */
	public Activator() {
		BundleContext context = a.Activator.getContext();
		context.getBundle();
		Application app = new Application();
		app.run(null);
	}
}
