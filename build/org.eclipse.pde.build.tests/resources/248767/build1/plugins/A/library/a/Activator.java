package a;

import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator {
	private static BundleContext myContext = null;
	private static Application myApplication = null;
	
	/**
	 * The constructor
	 */
	private Activator() {
	}

	public void start(BundleContext context) throws Exception {
		myContext = context;
		myApplication = new Application();
	}
	
	public static BundleContext getContext() {
		return myContext;
	}
	
	public static Application getApplication() {
		return myApplication;
	}

}
