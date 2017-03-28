package ds.annotations.test1;

import java.util.concurrent.Executor;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ExtendedReferenceMethodComponent {

	@Reference
	public void setExecutor(ComponentServiceObjects<Executor> executor) {

	}

	public void unsetExecutor(ComponentServiceObjects<Executor> executor) {

	}
}
