package $packageName$;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class $activator$ implements BundleActivator {

	private HelloService service;
	private ServiceTracker helloServiceTracker;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		service = new HelloServiceImpl();
		
		// register the service
		context.registerService(HelloService.class.getName(), service, new Hashtable());
		
		// create a tracker and track the service
		helloServiceTracker = new ServiceTracker(context, HelloService.class.getName(), null);
		helloServiceTracker.open();
		
		// grab the service
		service = (HelloService) helloServiceTracker.getService();
		service.speak();
		service.yell();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// close the service tracker
		helloServiceTracker.close();
		helloServiceTracker = null;
		
		service = null;
	}

}
