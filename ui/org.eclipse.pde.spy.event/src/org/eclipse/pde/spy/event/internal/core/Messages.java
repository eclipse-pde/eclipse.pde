package org.eclipse.pde.spy.event.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String CapturedEventFilterMatcher_NoSupportedItem;
	public static String CapturedEventFilterMatcher_NoSupportedOperator;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
