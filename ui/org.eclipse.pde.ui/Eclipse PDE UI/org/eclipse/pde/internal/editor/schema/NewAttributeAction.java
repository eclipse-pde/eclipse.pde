package org.eclipse.pde.internal.editor.schema;

import java.util.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.*;

public class NewAttributeAction extends Action {
	private SchemaElement element;
	private static final String NAME_COUNTER_KEY = "__schema_attribute_name";
	public static final String KEY_LABEL = "SchemaEditor.NewAttribute.label";
	public static final String KEY_TOOLTIP = "SchemaEditor.NewAttribute.tooltip";
	public static final String KEY_INITIAL_NAME = "SchemaEditor.NewAttribute.initialName";

public NewAttributeAction() {
	setText(PDEPlugin.getResourceString(KEY_LABEL));
	setImageDescriptor(PDEPluginImages.DESC_ATT_IMPL_OBJ);
	setToolTipText(PDEPlugin.getResourceString(KEY_TOOLTIP));
}
public org.eclipse.pde.internal.schema.SchemaElement getElement() {
	return element;
}
private String getInitialName() {
	Hashtable counters = PDEPlugin.getDefault().getDefaultNameCounters();
	Integer counter = (Integer)counters.get(NAME_COUNTER_KEY);
	if (counter==null) {
		counter = new Integer(1);
	}
	else {
		counter = new Integer(counter.intValue()+1);
	}
	counters.put(NAME_COUNTER_KEY, counter);
	return PDEPlugin.getFormattedMessage(KEY_INITIAL_NAME, counter.intValue()+"");
}
public void run() {
	String name = getInitialName();
	SchemaAttribute att = new SchemaAttribute(element, name);
	att.setType(new SchemaSimpleType(element.getSchema(), "string"));
	ISchemaType type = element.getType();
	SchemaComplexType complexType=null;
	if (!(type instanceof ISchemaComplexType)) {
		complexType = new SchemaComplexType(element.getSchema());
		element.setType(complexType);
	}
	else {
		complexType = (SchemaComplexType)type;
	}
	complexType.addAttribute(att);
}
public void setElement(org.eclipse.pde.internal.schema.SchemaElement newElement) {
	element = newElement;
}
}
