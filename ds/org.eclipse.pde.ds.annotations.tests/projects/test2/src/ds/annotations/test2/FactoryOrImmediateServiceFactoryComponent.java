package ds.annotations.test2;

import java.util.EventListener;

import org.osgi.service.component.annotations.Component;

@Component(servicefactory = true, immediate = true)
public class FactoryOrImmediateServiceFactoryComponent implements EventListener {

	@Component(servicefactory = true, factory = "foo")
	public static class FactoryServiceFactoryComponent implements EventListener {
		
	}

	@Component(servicefactory = true)
	public static class ThisIsOkComponent implements EventListener {
		
	}
}
