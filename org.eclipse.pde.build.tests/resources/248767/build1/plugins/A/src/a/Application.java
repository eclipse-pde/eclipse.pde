package a;

import org.eclipse.osgi.service.runnable.ParameterizedRunnable;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements ParameterizedRunnable {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object run(Object context) {
		System.out.println("Hello RCP World!");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}
}
