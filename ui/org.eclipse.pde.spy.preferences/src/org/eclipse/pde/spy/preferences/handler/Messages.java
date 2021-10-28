package org.eclipse.pde.spy.preferences.handler;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String ToggleLayoutControl_Toggle_to_hierarchical_layout;
	public static String ToggleLayoutControl_Toggle_to_flat_layout;
	public static String TogglePreferenceTraceControl_Toggle_Preference_Trace;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
