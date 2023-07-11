/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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

import org.eclipse.core.internal.runtime.XmlProcessorFactory;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.schema.EditableSchema;
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
		URLConnection connection = SchemaUtil.getURLConnection(fXSDFile.toURI().toURL());
		try (InputStream input = connection.getInputStream()) {
			XMLDefaultHandler handler = new XMLDefaultHandler(true);
			XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE().parse(input, handler);
			EditableSchema schema = new EditableSchema("pluginID", "pointID", "name", true);
			schema.traverseDocumentTree(handler.getDocumentElement());
		} finally {
			if (connection instanceof JarURLConnection jarConnection) {
				jarConnection.getJarFile().close();
			}
		}
	}

}
