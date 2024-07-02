/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.text.edits.TextEdit;
import org.junit.Test;
import org.osgi.framework.Constants;

@SuppressWarnings("deprecation")
// This testcase tests a deprecated constant
public class ExecutionEnvironmentTestCase extends MultiLineHeaderTestCase {

	public ExecutionEnvironmentTestCase() {
		super(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

	@Test
	public void testAddExecutionEnvironmentHeader() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4");

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		assertNotNull(header);
		assertEquals("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}

	@Test
	public void testRemoveExistingExecutionEnvironment() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		assertNotNull(header);

		String env = header.getEnvironments().get(0);
		header.removeExecutionEnvironment(env);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
	}

	@Test
	public void testAddExecutionEnvironment() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);

		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		header.addExecutionEnvironment("J2SE-1.5");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);

		StringBuilder expected = new StringBuilder("Bundle-RequiredExecutionEnvironment: J2SE-1.4,\n");
		expected.append(" J2SE-1.5\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	@Test
	public void testAddMulitplieExecutionEnvironmnets() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		header.addExecutionEnvironment("CDC-1.1/Foundation-1.1");
		header.addExecutionEnvironment("J2SE-1.5");
		header.addExecutionEnvironment("OSGi/Minimum-1.1");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(8, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(7));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(7) - fDocument.getLineOffset(3);
		StringBuilder expected = new StringBuilder("Bundle-RequiredExecutionEnvironment: J2SE-1.4,\n");
		expected.append(" CDC-1.1/Foundation-1.1,\n");
		expected.append(" J2SE-1.5,\n");
		expected.append(" OSGi/Minimum-1.1\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	@Test
	public void testRemoveExecutionEnvironment() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4,\n");
		buffer.append(" CDC-1.1/Foundation-1.1,\n");
		buffer.append(" OSGi/Minimum-1.1\n");
		fDocument.set(buffer.toString());
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		String env = header.getEnvironments().get(1);
		header.removeExecutionEnvironment(env);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuilder expected = new StringBuilder("Bundle-RequiredExecutionEnvironment: J2SE-1.4,\n");
		expected.append(" OSGi/Minimum-1.1\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	@Test
	public void testRemoveMultipleExecutionEnvironments() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4,\n");
		buffer.append(" CDC-1.1/Foundation-1.1,\n");
		buffer.append(" OSGi/Minimum-1.1\n");
		fDocument.set(buffer.toString());
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		List<String> envs = header.getEnvironments();
		header.removeExecutionEnvironment(envs.get(1));
		header.removeExecutionEnvironment(envs.get(0));

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals("Bundle-RequiredExecutionEnvironment: OSGi/Minimum-1.1\n", fDocument.get(pos, length));
	}

	@Test
	public void testPreserveSpacing() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: \n");
		buffer.append(" J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);

		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		header.addExecutionEnvironment("OSGi/Minimum-1.1");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(7, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(6));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(6) - fDocument.getLineOffset(3);

		StringBuilder expected = new StringBuilder("Bundle-RequiredExecutionEnvironment: \n");
		expected.append(" J2SE-1.4,\n");
		expected.append(" OSGi/Minimum-1.1\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	private RequiredExecutionEnvironmentHeader getRequiredExecutionEnvironmentHeader() {
		return (RequiredExecutionEnvironmentHeader) fModel.getBundle()
				.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

}
