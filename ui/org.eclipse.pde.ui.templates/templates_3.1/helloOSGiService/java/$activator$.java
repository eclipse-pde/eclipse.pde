package $packageName$;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class $activator$ implements BundleActivator, ServiceListener {

	private DictionaryService service;
	private ServiceTracker dictionaryServiceTracker;
	private BundleContext fContext;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		fContext = context;
		service = new DictionaryServiceImpl();

		Hashtable props = new Hashtable();
		// register the service
		context.registerService(DictionaryService.class.getName(), service, props);

		// create a tracker and track the service
		dictionaryServiceTracker = new ServiceTracker(context, DictionaryService.class.getName(), null);
		dictionaryServiceTracker.open();

		// have a service listener to implement the whiteboard pattern
	    fContext.addServiceListener(this, "(objectclass=" + Dictionary.class.getName() + ")");
		
		// grab the service
		service = (DictionaryService) dictionaryServiceTracker.getService();
		
		// register the dictionary
		service.registerDictionary(new DictionaryImpl());
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// close the service tracker
		dictionaryServiceTracker.close();
		dictionaryServiceTracker = null;

		service = null;
		fContext = null;
	}

	public void serviceChanged(ServiceEvent ev) {
		ServiceReference sr = ev.getServiceReference();
		switch(ev.getType()) {
			case ServiceEvent.REGISTERED:
			{
				Dictionary dictionary = (Dictionary) fContext.getService(sr);
				service.registerDictionary(dictionary);
			}
			break;
			case ServiceEvent.UNREGISTERING:
			{
				Dictionary dictionary = (Dictionary) fContext.getService(sr);
				service.unregisterDictionary(dictionary);
			}
			break;
		}
	}


}
