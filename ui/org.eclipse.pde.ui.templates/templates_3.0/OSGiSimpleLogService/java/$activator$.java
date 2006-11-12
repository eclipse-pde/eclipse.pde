package $packageName$;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class $activator$ implements BundleActivator {

	private ServiceTracker simpleLogServiceTracker;
	private SimpleLogService simpleLogService;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		// register the service
		context.registerService(
				SimpleLogService.class.getName(), 
				new SimpleLogServiceImpl(), 
				new Hashtable());
		
		// create a tracker and track the log service
		simpleLogServiceTracker = 
			new ServiceTracker(context, SimpleLogService.class.getName(), null);
		simpleLogServiceTracker.open();
		
		// grab the service
		simpleLogService = (SimpleLogService) simpleLogServiceTracker.getService();

		if(simpleLogService != null)
			simpleLogService.log("$startLogMessage$");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if(simpleLogService != null)
			simpleLogService.log("$stopLogMessage$");
		
		// close the service tracker
		simpleLogServiceTracker.close();
		simpleLogServiceTracker = null;
		
		simpleLogService = null;
	}

}
