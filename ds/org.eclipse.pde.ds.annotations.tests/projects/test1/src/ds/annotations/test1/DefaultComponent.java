package ds.annotations.test1;

import java.util.concurrent.Executor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DefaultComponent implements Runnable {

	@Reference
	public void setExecutor(Executor executor) {

	}

	public void unsetExecutor(Executor executor) {

	}

	@Override
	public void run() {

	}
}
