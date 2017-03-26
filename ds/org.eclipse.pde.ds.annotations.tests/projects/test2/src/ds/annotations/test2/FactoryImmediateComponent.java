package ds.annotations.test2;

import org.osgi.service.component.annotations.Component;

@Component(factory = "foo", immediate = true)
public class FactoryImmediateComponent {

}
