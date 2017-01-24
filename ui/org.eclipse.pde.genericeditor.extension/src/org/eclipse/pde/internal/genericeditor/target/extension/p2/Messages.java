package org.eclipse.pde.internal.genericeditor.target.extension.p2;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.genericeditor.target.extension.p2.messages"; //$NON-NLS-1$
	public static String UpdateJob_P2DataFetch;
	public static String UpdateJob_ErrorMessage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
