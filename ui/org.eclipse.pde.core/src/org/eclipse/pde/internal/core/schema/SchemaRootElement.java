package org.eclipse.pde.internal.core.schema;

import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;

public class SchemaRootElement extends SchemaElement implements
		ISchemaRootElement {

	private static final long serialVersionUID = 1L;
	public static final String P_DEP_REPLACEMENT = "replacement"; //$NON-NLS-1$
	private String fDeperecatedReplacement;
	
	public SchemaRootElement(ISchemaObject parent, String name) {
		super(parent, name);
	}

	public void setDeprecatedSuggestion(String value) {
		Object oldValue = fDeperecatedReplacement;
		fDeperecatedReplacement = value;
		getSchema().fireModelObjectChanged(this, P_DEP_REPLACEMENT, oldValue, fDeperecatedReplacement);
	}

	public String getDeprecatedSuggestion() {
		return fDeperecatedReplacement;
	}

	public String getExtendedAttributes() {
		if (fDeperecatedReplacement == null)
			return null;
		return Messages.getString("SchemaRootElement.1") + P_DEP_REPLACEMENT + Messages.getString("SchemaRootElement.2") + fDeperecatedReplacement + Messages.getString("SchemaRootElement.3"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
