package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

public interface ISchemaTransformer {
	public void transform(InputStream schema, StringBuffer output, PluginErrorReporter reporter);
}
