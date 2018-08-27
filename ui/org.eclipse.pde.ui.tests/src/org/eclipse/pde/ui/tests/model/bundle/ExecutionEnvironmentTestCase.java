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

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

@SuppressWarnings("deprecation")
// This testcase tests a deprecated constant
public class ExecutionEnvironmentTestCase extends MultiLineHeaderTestCase {

	public ExecutionEnvironmentTestCase() {
		super(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

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

	public void testRemoveExistingExecutionEnvironment() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		assertNotNull(header);

		ExecutionEnvironment env = ((RequiredExecutionEnvironmentHeader) header).getEnvironments()[0];
		((RequiredExecutionEnvironmentHeader) header).removeExecutionEnvironment(env);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
	}

	public void testAddExecutionEnvironment() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.5");
		((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(env);
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

	public void testAddMulitplieExecutionEnvironmnets() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment env1 = manager.getEnvironment("CDC-1.1/Foundation-1.1");
		IExecutionEnvironment env2 = manager.getEnvironment("J2SE-1.5");
		IExecutionEnvironment env3 = manager.getEnvironment("OSGi/Minimum-1.1");
		((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(env1);
		((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(env2);
		((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(env3);

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
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		ExecutionEnvironment env = ((RequiredExecutionEnvironmentHeader) header).getEnvironments()[1];
		((RequiredExecutionEnvironmentHeader) header).removeExecutionEnvironment(env);

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
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		ExecutionEnvironment[] envs = ((RequiredExecutionEnvironmentHeader) header).getEnvironments();
		((RequiredExecutionEnvironmentHeader) header).removeExecutionEnvironment(envs[1]);
		((RequiredExecutionEnvironmentHeader) header).removeExecutionEnvironment(envs[0]);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals("Bundle-RequiredExecutionEnvironment: OSGi/Minimum-1.1\n", fDocument.get(pos, length));
	}

	public void testPreserveSpacing() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-RequiredExecutionEnvironment: \n");
		buffer.append(" J2SE-1.4\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("OSGi/Minimum-1.1");
		((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(env);
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

}
