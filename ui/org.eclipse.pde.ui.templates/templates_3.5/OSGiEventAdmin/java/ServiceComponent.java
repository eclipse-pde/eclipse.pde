package $packageName$;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

// referenced in component.xml
public class ServiceComponent implements EventHandler {

	public void handleEvent(Event event) {
		// TODO handle event - $eventTopic$
		System.out.println(event.getTopic());
	}
	
}