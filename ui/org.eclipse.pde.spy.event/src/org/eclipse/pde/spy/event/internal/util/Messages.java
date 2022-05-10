package org.eclipse.pde.spy.event.internal.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String JDTUtils_0;
	public static String JDTUtils_ClassNotFoundInBundleClasspath;
	public static String PluginUtils_CannotFindBundleForClass;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
