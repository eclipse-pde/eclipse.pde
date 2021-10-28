package org.eclipse.pde.spy.preferences.preferencepage;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String TracePreferencePage_Settings_for_preference_spy;
	public static String TracePreferencePage_Trace_preference_values;
	public static String TracePreferencePage_Use_hierarchical_layout_in_the_tree;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
