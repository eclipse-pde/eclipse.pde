package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;

public interface ISchemaTransformer {
	public void transform(URL schemaURL, InputStream schema, PrintWriter output, PluginErrorReporter reporter);
}
