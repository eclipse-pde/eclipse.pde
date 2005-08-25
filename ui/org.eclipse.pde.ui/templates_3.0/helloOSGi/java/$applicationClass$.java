package $packageName$;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class $applicationClass$ implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println("$startMessage$");
	}
	
	public void stop(BundleContext context) throws Exception {
		System.out.println("$stopMessage$");
	}

}
