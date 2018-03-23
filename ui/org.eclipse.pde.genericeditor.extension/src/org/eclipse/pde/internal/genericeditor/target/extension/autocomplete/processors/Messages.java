package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors.messages"; //$NON-NLS-1$
	public static String AttributeValueCompletionProcessor_RepositoryRequired;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
