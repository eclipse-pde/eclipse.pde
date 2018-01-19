package rcp;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		System.out.println("Hello RCP World!");
		return IApplication.EXIT_OK;
	}

	public void stop() {
		// nothing to do
	}
}
