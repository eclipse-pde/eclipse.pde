package org.eclipse.pde.internal.core.schema;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ischema.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SchemaInclude extends SchemaObject implements ISchemaInclude {
	private String location;
	private ISchema includedSchema;
	
	public SchemaInclude(ISchemaObject parent, String location) {
		super(parent, location);
		this.location = location;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#getLocation()
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaInclude#setLocation(java.lang.String)
	 */
	public void setLocation(String location) throws CoreException {
		String oldValue = (String)this.location;
		this.location = location;
		includedSchema = null;
		getSchema().fireModelObjectChanged(this, P_LOCATION, oldValue, location);
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.println("<include schemaLocation=\""+location+"\"/>");
	}
	
	public ISchema getIncludedSchema() {
		if (includedSchema!=null && includedSchema.isDisposed())
			includedSchema = null;
		if (includedSchema==null) {
			// load it relative to the parent schema
		}
		return includedSchema;
	}
}