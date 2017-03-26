package ds.annotations.test1;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
		configurationPid = { "test.configurationPid", "$" },
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		enabled = false,
		factory = "test.componentFactory",
		immediate = false,
		name = "test.fullComponent",
		properties = {
				"/fullComponent1.properties",
				"/fullComponent2.properties",
		},
		property = {
				"implicitStringProperty=implicitStringValue",
				"explicitStringProperty:String=explicitStringValue",
				"integerProperty:Integer=1",
				"longProperty:Long=2",
				"shortProperty:Short=3",
				"byteProperty:Byte=4",
				"characterProperty:Character=5",
				"floatProperty:Float=6.7",
				"doubleProperty:Double=8.9",
				"implicitStringArrayProperty=implicitStringArrayValue1",
				"implicitStringArrayProperty=implicitStringArrayValue2",
				"explicitStringArrayProperty:String=explicitStringArrayValue1",
				"explicitStringArrayProperty:String=explicitStringArrayValue2",
				"explicitStringArrayProperty:String=explicitStringArrayValue3",
		},
		reference = @Reference(
				name = "comparator", 
				service = Comparator.class, 
				cardinality = ReferenceCardinality.OPTIONAL, 
				policy = ReferencePolicy.DYNAMIC,
				field = "comparator"),
		service = Map.class,
		scope = ServiceScope.SINGLETON)
public class FullComponent extends AbstractMap<String, Object> {
	
	@interface Config {
		
		String stringProperty() default "stringValue";
		
		int integerProperty() default -1;
		
		String nonDefaultProperty();
	}

	private volatile Comparator<String> comparator;
	
	@Reference
	private volatile List<EventListener> listeners;
	
	private volatile Set<Map.Entry<String, Object>> entrySet = new HashSet<>();

	@Override
	@Activate
	@Modified
	public void putAll(Map<? extends String, ? extends Object> m) {
		super.putAll(m);
	}

	@Deactivate
	public void clear(Config config) {
		clear();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		return entrySet;
	}

	@Reference(
			cardinality = ReferenceCardinality.OPTIONAL,
			name = "Entries",
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY,
			target = "(!(component.name=test.fullComponent))",
			updated = "updateEntrySet",
			unbind = "unassignEntrySet")
	public void assignEntrySet(Set<Map.Entry<String, Object>> entrySet) {
		this.entrySet = entrySet;
	}

	public void updateEntrySet(ServiceReference<Set<Map.Entry<String, Object>>> ref) {
		// do nothing
	}

	public void unassignEntrySet(Set<Map.Entry<String, Object>> entrySet) {
		if (this.entrySet == entrySet) {
			this.entrySet = new HashSet<>();
		}
	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = Map.Entry.class)
	public void addEntry(Object entry) {
		if (entry instanceof Map.Entry) {
			Map.Entry<?, ?> actualEntry = (Map.Entry<?, ?>) entry;
			put(actualEntry.getKey() == null ? null : actualEntry.getKey().toString(), actualEntry.getValue());
		}
	}

	public void removeEntry(Object entry) {
		if (entry instanceof Map.Entry) {
			Map.Entry<?, ?> actualEntry = (Map.Entry<?, ?>) entry;
			remove(actualEntry.getKey() == null ? null : actualEntry.getKey().toString(), actualEntry.getValue());
		}
	}
}
