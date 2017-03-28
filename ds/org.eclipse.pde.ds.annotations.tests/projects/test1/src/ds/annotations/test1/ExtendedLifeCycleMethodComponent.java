package ds.annotations.test1;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component
public class ExtendedLifeCycleMethodComponent {
	
	public @interface Config {
		
	}
	
	@Activate
	public void activate(Config config, Map<String, ?> properties) {

	}

	@Modified
	public void modified(Config config, Map<String, ?> properties) {

	}
	
	@Deactivate
	public void deactivate(Config config, Map<String, ?> properties, int reason) {

	}
}
