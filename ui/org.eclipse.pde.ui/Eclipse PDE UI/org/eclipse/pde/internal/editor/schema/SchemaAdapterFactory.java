package org.eclipse.pde.internal.editor.schema;

import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.IAdapterFactory;
import java.util.*;

public class SchemaAdapterFactory implements IAdapterFactory {
	private ElementPropertySource elementPropertySource;
	private AttributePropertySource attributePropertySource;

public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adapterType.equals(IPropertySource.class)) return getProperties(adaptableObject);
	return null;
}
public java.lang.Class[] getAdapterList() {
	return new Class[] { IPropertySource.class };
}
protected AttributePropertySource getAttributeProperties(ISchemaAttribute att) {
	if (attributePropertySource == null)
		attributePropertySource = new AttributePropertySource(att);
	else
		attributePropertySource.setSourceObject(att);
	return attributePropertySource;
}
protected ElementPropertySource getElementProperties(ISchemaElement element) {
	if (elementPropertySource == null)
		elementPropertySource = new ElementPropertySource(element);
	else
		elementPropertySource.setSourceObject(element);
	return elementPropertySource;
}
private IPropertySource getProperties(Object object) {
	if (object instanceof ISchemaElement && !(object instanceof ISchemaObjectReference))
		return getElementProperties((ISchemaElement)object);
	if (object instanceof ISchemaAttribute)
		return getAttributeProperties((ISchemaAttribute)object);
	if (object instanceof ISchemaRepeatable)
		return getRepeatableProperties((ISchemaRepeatable)object);
	return null;
}
protected GrammarPropertySource getRepeatableProperties(ISchemaRepeatable obj) {
	if (obj instanceof ISchemaCompositor)
		return new CompositorPropertySource((ISchemaCompositor) obj);
	if (obj instanceof SchemaElementReference)
		return new GrammarPropertySource(obj);
	return null;
}
}
