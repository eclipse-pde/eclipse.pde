package org.eclipse.pde.internal.builders;

import java.io.*;

public interface ISchemaTransformer {
	public void transform(InputStream schema, StringBuffer output, PluginErrorReporter reporter);
}
