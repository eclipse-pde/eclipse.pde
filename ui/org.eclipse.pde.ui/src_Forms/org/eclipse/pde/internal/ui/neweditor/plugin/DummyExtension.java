/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DummyExtension extends DummyExtensionElement {
	private ISchema schema;
	
	public DummyExtension(String id, String name, String point) {
		super("extension");
		setProperty("id", id);
		setProperty("name", name);
		setProperty("point", point);
	}
	public ISchema getSchema() {
		if (schema == null) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			schema = registry.getSchema(getProperty("point"));
		} else if (schema.isDisposed()) {
			schema = null;
		}
		return schema;
	}	
	public String toString() {
		return getProperty("point");
	}
}