package $packageName$;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class $activator$ implements BundleActivator, ServiceTrackerCustomizer<SimpleLogService, SimpleLogService> {

	private ServiceTracker<SimpleLogService, SimpleLogService> simpleLogServiceTracker;
	private BundleContext bundleContext;
	
	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		
		// create a tracker and track the log service
		simpleLogServiceTracker = 
			new ServiceTracker<SimpleLogService, SimpleLogService>(context, SimpleLogService.class.getName(), this);
		simpleLogServiceTracker.open();
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		// close the service tracker
		simpleLogServiceTracker.close();
		simpleLogServiceTracker = null;
	}
	
	@Override
	public SimpleLogService addingService(ServiceReference<SimpleLogService> reference) {
		// grab the service
		SimpleLogService simpleLogService = bundleContext.getService(reference);

		if (simpleLogService != null)
			simpleLogService.log("$startLogMessage$");
	
		return simpleLogService;
	}

	@Override
	public void modifiedService(ServiceReference<SimpleLogService> reference, SimpleLogService service) {
		// do nothing
	}

	@Override
	public void removedService(ServiceReference<SimpleLogService> reference, SimpleLogService simpleLogService) {
		if (simpleLogService != null)
			simpleLogService.log("$stopLogMessage$");
		
		bundleContext.ungetService(reference);
	}
}
