/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.tests.performance.parts;

import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.schema.EditableSchema;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.eclipse.pde.internal.core.util.SchemaUtil;
import org.eclipse.test.performance.Dimension;

/**
 * SchemaLoaderPerfTest
 *
 */
public class SchemaTraversePerfTest extends AbstractSchemaPerfTest {

	/**
	 * @return
	 */
	public static Test suite() {
		return new TestSuite(SchemaTraversePerfTest.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.tests.performance.parts.AbstractSchemaPerfTest#setUpIterations()
	 */
	protected void setUpIterations() {
		fTestIterations = 5;
		fWarmupIterations = 50;
		fRuns = 3000;
	}

	/**
	 * @throws Exception
	 */
	public void testSchemaTraverse() throws Exception {
		tagAsSummary("Show hover info in manifest editor", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		executeTestRun();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.tests.performance.parts.AbstractSchemaPerfTest#executeTest()
	 */
	protected void executeTest() throws Exception {
		InputStream input = null;
		input = SchemaUtil.getInputStream(fXSDFile.toURL());
		SAXParserWrapper parser = new SAXParserWrapper();
		XMLDefaultHandler handler = new XMLDefaultHandler(true);
		parser.parse(input, handler);
		if (input != null) {
			input.close();
		}
		EditableSchema schema = new EditableSchema("pluginID", "pointID", "name", true);
		schema.traverseDocumentTree(handler.getDocumentElement());
	}

}
