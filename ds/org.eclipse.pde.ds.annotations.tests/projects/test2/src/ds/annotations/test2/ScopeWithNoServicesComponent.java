package ds.annotations.test2;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope = ServiceScope.BUNDLE)
public class ScopeNoServicesComponent {

}
