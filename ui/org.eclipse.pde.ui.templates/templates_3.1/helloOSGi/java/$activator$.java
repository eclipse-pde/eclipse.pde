package $packageName$;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class $activator$ implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("$startMessage$");
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("$stopMessage$");
	}

}
