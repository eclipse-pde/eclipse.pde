package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.core.runtime.*;
import java.util.*;

public class ExtensionAdapter extends ParentAdapter {


public ExtensionAdapter(Object object) {
	super(object);
}
protected Object[] createChildren() {
	IExtension extension = (IExtension)getObject();

	IConfigurationElement [] elements = extension.getConfigurationElements();
	Object [] result = new ConfigurationElementAdapter[elements.length];
	for (int i=0; i<elements.length; i++) {
		IConfigurationElement config = elements[i];
		result[i]=new ConfigurationElementAdapter(config);
	}
	return result;
}
}
