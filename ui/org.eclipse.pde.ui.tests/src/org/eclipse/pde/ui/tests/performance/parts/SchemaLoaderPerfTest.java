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

import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;

/**
 * SchemaLoaderPerfTest
 *
 */
public class SchemaLoaderPerfTest extends AbstractSchemaPerfTest {

	private boolean fAbbreviated;

	/**
	 * @throws Exception
	 */
	public void testSchemaUnabbreviated() throws Exception {
		fAbbreviated = false;
		fTestIterations = 5;
		fWarmupIterations = 50;
		fRuns = 600;
		executeTestRun();
	}

	/**
	 * @throws Exception
	 */
	public void testSchemaAbbreviated() throws Exception {
		fAbbreviated = true;
		fTestIterations = 5;
		fWarmupIterations = 50;
		fRuns = 3000;
		executeTestRun();
	}

	@Override
	protected void executeTest() throws Exception {
		SchemaDescriptor descriptor = new SchemaDescriptor(fXSDFile);
		Schema schema = (Schema) descriptor.getSchema(fAbbreviated);
		if (schema.getName() == null) {
			throw new Exception("ERROR: Extension point schema name missing"); //$NON-NLS-1$
		}
	}

}
