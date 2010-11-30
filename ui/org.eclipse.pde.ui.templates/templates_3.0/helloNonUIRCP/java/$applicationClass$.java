package $packageName$;

import org.eclipse.core.runtime.IPlatformRunnable;

/**
 * This class controls all aspects of the application's execution
 */
public class $applicationClass$ implements IPlatformRunnable {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		System.out.println("$message$");
		return IPlatformRunnable.EXIT_OK;
	}
}
