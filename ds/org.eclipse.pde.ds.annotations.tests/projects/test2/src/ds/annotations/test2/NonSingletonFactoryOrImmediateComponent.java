package ds.annotations.test2;

import java.util.EventListener;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope = ServiceScope.BUNDLE, immediate = true)
public class NonSingletonFactoryOrImmediateComponent implements EventListener {

	@Component(scope = ServiceScope.PROTOTYPE, factory = "foo")
	public static class NonSingletonFactoryComponent implements EventListener {
		
	}
}
