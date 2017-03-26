package ds.annotations.test2;

import java.util.EventListener;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(servicefactory = false, scope = ServiceScope.BUNDLE)
public class ServiceFactoryWithScopeComponent implements EventListener {

	@Component(servicefactory = true, scope = ServiceScope.BUNDLE)
	public static class ThisIsOkComponent implements EventListener {
	}

	@Component(servicefactory = false, scope = ServiceScope.SINGLETON)
	public static class AlsoOkComponent implements EventListener {
	}
}
