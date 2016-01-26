package $packageName$;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * This class controls all aspects of the application's execution
 */
public class $applicationClass$ implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		System.out.println("$message$");
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
