package org.eclipse.pde.internal.editor.schema;

import org.eclipse.pde.internal.schema.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.*;

public class CompositorPropertySource extends GrammarPropertySource {
	public static final String P_KIND = "kind";
	public static final String KEY_KIND = "SchemaEditor.CompositorPR.kind";

public CompositorPropertySource(ISchemaCompositor obj) {
	super(obj);
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = (Vector) super.getPropertyDescriptorsVector();
		PropertyDescriptor cdesc =
			createComboBoxPropertyDescriptor(P_KIND, PDEPlugin.getResourceString(KEY_KIND), ISchemaCompositor.kindTable);
		if (cdesc instanceof ComboBoxPropertyDescriptor)
			((ComboBoxPropertyDescriptor) cdesc).setLabelProvider(
				new ComboProvider(P_KIND, ISchemaCompositor.kindTable));
		descriptors.addElement(cdesc);
	}
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	ISchemaCompositor compositor = (ISchemaCompositor)getSourceObject();
	if (name.equals(P_KIND)) {
		return new Integer(compositor.getKind());
	}
	return super.getPropertyValue(name);
}
public void setPropertyValue(String name, Object value) {
	SchemaCompositor compositor = (SchemaCompositor)getSourceObject();
	
	if (name.equals(P_KIND)) {
		compositor.setKind(((Integer)value).intValue());
	}
	else super.setPropertyValue(name, value);
}
}
