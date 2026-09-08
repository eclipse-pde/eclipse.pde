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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.text.edits.TextEdit;
import org.junit.Test;
import org.osgi.framework.Constants;

@SuppressWarnings("deprecation")
// This testcase tests a deprecated constant
public class ExecutionEnvironmentTestCase extends MultiLineHeaderTestCase {

	public ExecutionEnvironmentTestCase() {
		super(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

	// TODO: Check what's duplicated and what is really necessary.
	// Actually here we should only test the EEResolvers specific behaviour.
	// Everything else should be tested in the existing case.

	@Test
	public void testAddExecutionEnvironmentHeader() throws Exception {
		String text = """
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				""";
		fDocument.set(text);
		load(true);
		fModel.getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4");

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		assertNotNull(header);
		assertEquals("Bundle-RequiredExecutionEnvironment: J2SE-1.4\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(text + header.write(), fDocument.get());
	}

	@Test
	public void testRemoveExistingExecutionEnvironment() throws Exception {
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment: J2SE-1.4
				""");
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		assertNotNull(header);

		String env = header.getEnvironments().getFirst();
		header.removeExecutionEnvironment(env);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
	}

	@Test
	public void testAddExecutionEnvironment() throws Exception {
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment: J2SE-1.4
				""");
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

		assertEquals("""
				Bundle-RequiredExecutionEnvironment: J2SE-1.4,
				 J2SE-1.5
				""", fDocument.get(pos, length));
	}

	@Test
	public void testAddMulitplieExecutionEnvironmnets() throws Exception {
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment: J2SE-1.4
				""");
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
		assertEquals("""
				Bundle-RequiredExecutionEnvironment: CDC-1.1/Foundation-1.1,
				 OSGi/Minimum-1.1,
				 J2SE-1.4,
				 J2SE-1.5
				""", fDocument.get(pos, length));
	}

	@Test
	public void testRemoveExecutionEnvironment() throws Exception {
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment: J2SE-1.4,
				 CDC-1.1/Foundation-1.1,
				 OSGi/Minimum-1.1
				""");
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		header.removeExecutionEnvironment("CDC-1.1/Foundation-1.1");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		assertEquals("""
				Bundle-RequiredExecutionEnvironment: OSGi/Minimum-1.1,
				 J2SE-1.4
				""", fDocument.get(pos, length));
	}

	@Test
	public void testRemoveMultipleExecutionEnvironments() throws Exception {
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment: J2SE-1.4,
				 CDC-1.1/Foundation-1.1,
				 OSGi/Minimum-1.1
				""");
		load(true);
		RequiredExecutionEnvironmentHeader header = getRequiredExecutionEnvironmentHeader();
		header.removeExecutionEnvironment("CDC-1.1/Foundation-1.1");
		header.removeExecutionEnvironment("J2SE-1.4");

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
		fDocument.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymoblicName: com.example.xyz
				Bundle-RequiredExecutionEnvironment:\s
				 J2SE-1.4
				""");
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

		assertEquals("""
				Bundle-RequiredExecutionEnvironment:\s
				 OSGi/Minimum-1.1,
				 J2SE-1.4
				""", fDocument.get(pos, length));
	}

	@Test
	public void testManifestUtils_parseRequiredEEsFromFilter() throws Exception {
		{
			Set<String> ees = new HashSet<>();
			ManifestUtils.parseRequiredEEsFromFilter("(&(osgi.ee=JavaSE)(version=1.8))", ees::add);
			assertEquals(Set.of("JavaSE-1.8"), ees);
		}
		{
			Set<String> ees = new HashSet<>();
			ManifestUtils.parseRequiredEEsFromFilter("(& ( version=17 ) ( osgi.ee=JavaSE ) )", ees::add);
			assertEquals(Set.of("JavaSE-17"), ees);
		}
		{
			Set<String> ees = new HashSet<>();
			ManifestUtils.parseRequiredEEsFromFilter("(&(osgi.ee=JavaSE)(version>=1.6)(version<=1.8))", ees::add);
			assertEquals(Set.of("JavaSE-1.6", "JavaSE-1.7", "JavaSE-1.8"), ees);
		}
		{
			// Equivalent to
			// Bundle-RequiredExecutionEnvironment: JavaSE-17, JavaSE-21
			Set<String> ees = new HashSet<>();
			ManifestUtils.parseRequiredEEsFromFilter(
					"(| (&(version=17)(osgi.ee=JavaSE)) (&(osgi.ee=JavaSE)(version=21)) )", ees::add);
			assertEquals(Set.of("JavaSE-17", "JavaSE-21"), ees);
		}
	}

	@Test
	public void testGetRequiredExecutionEnvironments_breeOnly() {
		Map<String, String> manifest = Map.of(
				"Bundle-RequiredExecutionEnvironment", "JavaSE-17, JavaSE-21"); //$NON-NLS-1$ //$NON-NLS-2$
		assertRequiredEEs(manifest, "JavaSE-17", "JavaSE-21");
	}

	@Test
	public void testGetRequiredExecutionEnvironments_requireCapabilityOnly() {
		Map<String, String> manifest = Map.of(
				"Require-Capability", //$NON-NLS-1$
				"osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\""); //$NON-NLS-1$
		assertRequiredEEs(manifest, "JavaSE-17");
	}


	@Test
	public void testGetRequiredExecutionEnvironments_bothHeaders() {
		Map<String, String> manifest = Map.of(
				"Bundle-RequiredExecutionEnvironment", "JavaSE-11", //$NON-NLS-1$ //$NON-NLS-2$
				"Require-Capability", //$NON-NLS-1$
				"osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\""); //$NON-NLS-1$
		assertRequiredEEs(manifest, "JavaSE-11", "JavaSE-17");
	}



	@Test
	public void testGetRequiredExecutionEnvironments_emptyManifest() {
		assertRequiredEEs(Map.of());
	}

	@Test
	public void testGetRequiredExecutionEnvironments_nonJavaSECapabilityIncluded() {
		// getRequiredExecutionEnvironments returns all EE IDs, including non-JavaSE ones.
		// Filtering to JavaSE happens downstream in eeIdToJavaVersion().
		Map<String, String> manifest = Map.of(
				"Require-Capability", //$NON-NLS-1$
				"osgi.ee;filter:=\"(&(osgi.ee=OSGi/Minimum)(version=1.2))\""); //$NON-NLS-1$
		List<String> ees = ManifestUtils.getRequiredExecutionEnvironments(manifest).toList();
		assertFalse("OSGi/Minimum EE should be present in the stream", ees.isEmpty()); //$NON-NLS-1$
		assertNull("eeIdToJavaVersion should return null for non-JavaSE EE", //$NON-NLS-1$
				ManifestUtils.javaVersionOfExecutionEnvironment(ees.getFirst()));
	}

	@Test
	public void testGetRequiredExecutionEnvironments_orFilterYieldsBothEEs() {
		Map<String, String> manifest = Map.of(
				"Require-Capability", //$NON-NLS-1$
				"osgi.ee;filter:=\"(|(&(osgi.ee=JavaSE)(version=17))(&(osgi.ee=JavaSE)(version=21)))\""); //$NON-NLS-1$
		assertRequiredEEs(manifest, "JavaSE-17", "JavaSE-21");
	}

	private void assertRequiredEEs(Map<String, String> manifest, String... expectedEEs) {
		Set<String> actualEEs = ManifestUtils.getRequiredExecutionEnvironments(manifest).collect(Collectors.toSet());
		assertEquals(Set.of(expectedEEs), actualEEs);
	}

	@Test
	public void testEeIdToJavaVersion_standardVersions() {
		assertEquals("17", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-17")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("21", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-21")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.8", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-1.8")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testEeIdToJavaVersion_compactProfiles() {
		// compact profiles always map to "1.8" regardless of compiler support
		assertEquals("1.8", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE/compact1-1.8")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.8", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE/compact2-1.8")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.8", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE/compact3-1.8")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testEeIdToJavaVersion_unknownReturnsNull() {
		assertNull(ManifestUtils.javaVersionOfExecutionEnvironment("garbage")); //$NON-NLS-1$
		assertNull(ManifestUtils.javaVersionOfExecutionEnvironment(null));
		assertNull(ManifestUtils.javaVersionOfExecutionEnvironment("")); //$NON-NLS-1$
		assertNull(ManifestUtils.javaVersionOfExecutionEnvironment("OSGi/Minimum-1.2")); //$NON-NLS-1$
		assertNull(ManifestUtils.javaVersionOfExecutionEnvironment("CDC-1.1/Foundation-1.1")); //$NON-NLS-1$
	}

	@Test
	public void testEeIdToJavaVersion_allSupportedVersions() {
		SortedSet<String> supported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler();
		for (String version : supported) {
			assertEquals("JavaSE-" + version + " should map to " + version, //$NON-NLS-1$ //$NON-NLS-2$
					version, ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-" + version)); //$NON-NLS-1$
		}
	}

	@Test
	public void testEeIdToJavaVersion_unsupportedVersionsStillReturnVersion() {
		// eeIdToJavaVersion extracts the version string without checking compiler support
		assertEquals("1.6", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-1.6")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.7", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-1.7")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("999", ManifestUtils.javaVersionOfExecutionEnvironment("JavaSE-999")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private RequiredExecutionEnvironmentHeader getRequiredExecutionEnvironmentHeader() {
		return (RequiredExecutionEnvironmentHeader) fModel.getBundle()
				.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

}
