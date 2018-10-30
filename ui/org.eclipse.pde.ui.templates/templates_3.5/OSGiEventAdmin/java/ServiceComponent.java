package $packageName$;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(
	property = {
		"event.topics=$eventTopic$"
	}
)
public class ServiceComponent implements EventHandler {

	public void handleEvent(Event event) {
		// TODO handle event - $eventTopic$
		System.out.println(event.getTopic());
	}

}