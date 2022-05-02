package a;

import org.eclipse.osgi.service.runnable.ParameterizedRunnable;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements ParameterizedRunnable {

	public Object run(Object context) {
		System.out.println("Hello RCP World!");
		return null;
	}

	public void stop() {
		// nothing to do
	}
}
