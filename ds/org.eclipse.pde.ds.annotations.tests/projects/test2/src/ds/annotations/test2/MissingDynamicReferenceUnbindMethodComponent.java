package ds.annotations.test2;

import java.util.Timer;
import java.util.concurrent.Executor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component
public class MissingDynamicReferenceUnbindMethodComponent {

	@Reference
	void setStaticReference(Executor executor) {
		
	}
	
	void xunsetStaticReference(Executor executor) {
		
	}
	
	@Reference(policy = ReferencePolicy.DYNAMIC)
	void setDynamicReference(Timer timer) {
		
	}
	
	void xunsetDynamicReference(Timer timer) {
		
	}
}
