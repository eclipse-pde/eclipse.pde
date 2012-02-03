package org.eclipse.pde.api.tools.generator;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class EEDescriptionGenerator implements IApplication {

	/**
	 * Runs the application to generate the EE description
	 */
	public Object start(IApplicationContext context) throws Exception {
		Object arguments = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		if (arguments instanceof String[]) {
			String[] args = (String[]) arguments;
			EEGenerator.main(args);
		}
		return IApplication.EXIT_OK;
	}
	public void stop() {
		// do nothing
	}
}
