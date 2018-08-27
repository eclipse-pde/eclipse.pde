/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.tests.performance.parts;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URLConnection;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.schema.EditableSchema;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.eclipse.pde.internal.core.util.SchemaUtil;

public class SchemaTraversePerfTest extends AbstractSchemaPerfTest {

	@Override
	protected void setUpIterations() {
		fTestIterations = 5;
		fWarmupIterations = 50;
		fRuns = 3000;
	}

	/**
	 * @throws Exception
	 */
	public void testSchemaTraverse() throws Exception {
		executeTestRun();
	}

	@Override
	protected void executeTest() throws Exception {
		URLConnection connection = SchemaUtil.getURLConnection(fXSDFile.toURL());
		try (InputStream input = connection.getInputStream()) {
			SAXParserWrapper parser = new SAXParserWrapper();
			XMLDefaultHandler handler = new XMLDefaultHandler(true);
			parser.parse(input, handler);
			EditableSchema schema = new EditableSchema("pluginID", "pointID", "name", true);
			schema.traverseDocumentTree(handler.getDocumentElement());
		} finally {
			if (connection instanceof JarURLConnection){
				((JarURLConnection)connection).getJarFile().close();
			}
		}
	}

}
