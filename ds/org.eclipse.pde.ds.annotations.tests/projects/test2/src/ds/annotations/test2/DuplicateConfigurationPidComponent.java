package ds.annotations.test2;

import org.osgi.service.component.annotations.Component;

@Component(configurationPid = {"ds.annotations.test2.DuplicateConfigurationPidComponent", "foo", "$"})
public class DuplicateConfigurationPidComponent {

}
