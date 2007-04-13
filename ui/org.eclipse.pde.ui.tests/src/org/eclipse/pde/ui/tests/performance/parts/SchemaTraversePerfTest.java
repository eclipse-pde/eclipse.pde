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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.internal.core.schema.EditableSchema;
import org.eclipse.pde.internal.core.schema.SchemaHandler;
import org.eclipse.test.performance.Dimension;
import org.xml.sax.SAXException;

/**
 * SchemaLoaderPerfTest
 *
 */
public class SchemaTraversePerfTest extends AbstractSchemaPerfTest {

	private SAXParser fParser;
	
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
		fRuns = 600;		
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.tests.performance.parts.AbstractSchemaPerfTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		setUpParser();
	}

	/**
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws FactoryConfigurationError
	 */
	private void setUpParser() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		fParser = SAXParserFactory.newInstance().newSAXParser();
	}
	
	/**
	 * @throws Exception
	 */
	public void testSchemaTraverse() throws Exception {
		tagAsGlobalSummary("Traversing Schema Model", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		executeTestRun();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.tests.performance.parts.AbstractSchemaPerfTest#executeTest()
	 */
	protected void executeTest() throws Exception {
		InputStream input = getInputStream();
		SchemaHandler handler = new SchemaHandler(false);
		fParser.parse(input, handler);
		if (input != null) {
			input.close();
		}		
		EditableSchema schema = new EditableSchema("pluginID", "pointID", "name", false);
		schema.traverseDocumentTree(handler.getDocumentElement());
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	private InputStream getInputStream() throws IOException {
		if ("file".equals(fXSDFile.toURL().getProtocol())) //$NON-NLS-1$
			return new FileInputStream(fXSDFile);
		return fXSDFile.toURL().openStream();
	}	
	
}
