package org.eclipse.pde.spy.preferences.parts;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String PreferenceSpyPart_Key;
	public static String PreferenceSpyPart_New_Value;
	public static String PreferenceSpyPart_Nodepath;
	public static String PreferenceSpyPart_Old_Value;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
