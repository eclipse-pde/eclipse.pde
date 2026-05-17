/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse PDE contributors
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.ui.tests.util.FreezeMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Regression tests for false-positive API compatibility warnings caused by ECJ
 * normalizing {@code List<? extends Object>} to {@code List<?>} in bytecode
 * signatures (JDT PR #5054). The two forms are semantically identical, so API
 * tools must not report a delta when comparing class files compiled with the
 * old and new ECJ.
 *
 * <p>
 * These tests use ASM to craft class files with specific generic signatures,
 * bypassing the compiler, so that both the old-style
 * {@code +Ljava/lang/Object;} (extends-Object wildcard) and the new-style
 * {@code *} (unbounded wildcard) can be tested independently of the compiler
 * version in use.
 *
 * @see <a href="https://github.com/eclipse-pde/eclipse.pde/issues/2332">eclipse.pde#2332</a>
 * @see <a href="https://github.com/eclipse-equinox/p2/pull/1072#issuecomment-4469690952">p2 PR #1072</a>
 * @see <a href="https://github.com/eclipse-jdt/eclipse.jdt.core/pull/5054">JDT ECJ PR #5054</a>
 */
public class WildcardCompatibilityTests {

	private static final String BUNDLE_NAME = "deltatest"; //$NON-NLS-1$

	/** Erased descriptor of the test method. */
	private static final String METHOD_DESCRIPTOR = "()Ljava/util/List;"; //$NON-NLS-1$

	/** Erased descriptor of the test field. */
	private static final String FIELD_DESCRIPTOR = "Ljava/util/List;"; //$NON-NLS-1$

	/**
	 * Generic signature for {@code List<? extends Object>} as emitted by old
	 * ECJ (before JDT PR #5054).
	 */
	private static final String SIG_EXTENDS_OBJECT = "+Ljava/lang/Object;"; //$NON-NLS-1$

	/**
	 * Generic signature for {@code List<?>} as emitted by new ECJ (after JDT
	 * PR #5054).
	 */
	private static final String SIG_UNBOUNDED = "*"; //$NON-NLS-1$

	private static final String METHOD_SIG_EXTENDS_OBJECT = "()Ljava/util/List<" + SIG_EXTENDS_OBJECT + ">;"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String METHOD_SIG_UNBOUNDED = "()Ljava/util/List<" + SIG_UNBOUNDED + ">;"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String FIELD_SIG_EXTENDS_OBJECT = "Ljava/util/List<" + SIG_EXTENDS_OBJECT + ">;"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String FIELD_SIG_UNBOUNDED = "Ljava/util/List<" + SIG_UNBOUNDED + ">;"; //$NON-NLS-1$ //$NON-NLS-2$

	private File beforeRoot;
	private File afterRoot;
	private IApiBaseline beforeBaseline;
	private IApiBaseline afterBaseline;

	@Before
	public void setUp() throws Exception {
		FreezeMonitor.expectCompletionInAMinute();
		beforeRoot = Files.createTempDirectory("api-delta-before").toFile(); //$NON-NLS-1$
		afterRoot = Files.createTempDirectory("api-delta-after").toFile(); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		ApiTestingEnvironment.dispose(beforeBaseline);
		ApiTestingEnvironment.dispose(afterBaseline);
		deleteRecursively(beforeRoot);
		deleteRecursively(afterRoot);
		FreezeMonitor.done();
	}

	/**
	 * Changing a method return type signature from
	 * {@code List<? extends Object>} (old ECJ bytecode) to {@code List<?>}
	 * (new ECJ bytecode) must not be reported as an API delta.
	 */
	@Test
	public void testMethodReturnWildcardExtendsObjectToUnbounded() throws Exception {
		byte[] beforeClass = generateClassWithMethod("p/X", METHOD_SIG_EXTENDS_OBJECT); //$NON-NLS-1$
		byte[] afterClass = generateClassWithMethod("p/X", METHOD_SIG_UNBOUNDED); //$NON-NLS-1$

		writeBundleDir(beforeRoot, beforeClass);
		writeBundleDir(afterRoot, afterClass);

		IDelta delta = compare();
		assertTrue("List<? extends Object> → List<?> method must be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * The reverse direction ({@code List<?>} → {@code List<? extends Object>})
	 * must also be reported as compatible.
	 */
	@Test
	public void testMethodReturnUnboundedToWildcardExtendsObject() throws Exception {
		byte[] beforeClass = generateClassWithMethod("p/X", METHOD_SIG_UNBOUNDED); //$NON-NLS-1$
		byte[] afterClass = generateClassWithMethod("p/X", METHOD_SIG_EXTENDS_OBJECT); //$NON-NLS-1$

		writeBundleDir(beforeRoot, beforeClass);
		writeBundleDir(afterRoot, afterClass);

		IDelta delta = compare();
		assertTrue("List<?> → List<? extends Object> method must be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * Changing a field's generic signature from {@code List<? extends Object>}
	 * (old ECJ bytecode) to {@code List<?>} (new ECJ bytecode) must not be
	 * reported as an API delta.
	 */
	@Test
	public void testFieldWildcardExtendsObjectToUnbounded() throws Exception {
		byte[] beforeClass = generateClassWithField("p/X", FIELD_SIG_EXTENDS_OBJECT); //$NON-NLS-1$
		byte[] afterClass = generateClassWithField("p/X", FIELD_SIG_UNBOUNDED); //$NON-NLS-1$

		writeBundleDir(beforeRoot, beforeClass);
		writeBundleDir(afterRoot, afterClass);

		IDelta delta = compare();
		assertTrue("List<? extends Object> → List<?> field must be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	/**
	 * The reverse direction for fields ({@code List<?>} →
	 * {@code List<? extends Object>}) must also be compatible.
	 */
	@Test
	public void testFieldUnboundedToWildcardExtendsObject() throws Exception {
		byte[] beforeClass = generateClassWithField("p/X", FIELD_SIG_UNBOUNDED); //$NON-NLS-1$
		byte[] afterClass = generateClassWithField("p/X", FIELD_SIG_EXTENDS_OBJECT); //$NON-NLS-1$

		writeBundleDir(beforeRoot, beforeClass);
		writeBundleDir(afterRoot, afterClass);

		IDelta delta = compare();
		assertTrue("List<?> → List<? extends Object> field must be NO_DELTA", delta == ApiComparator.NO_DELTA); //$NON-NLS-1$
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private IDelta compare() throws Exception {
		beforeBaseline = TestSuiteHelper.createBaseline("before", beforeRoot); //$NON-NLS-1$
		afterBaseline = TestSuiteHelper.createBaseline("after", afterRoot); //$NON-NLS-1$

		IApiComponent beforeComp = beforeBaseline.getApiComponent(BUNDLE_NAME);
		assertNotNull("before API component not found", beforeComp); //$NON-NLS-1$
		IApiComponent afterComp = afterBaseline.getApiComponent(BUNDLE_NAME);
		assertNotNull("after API component not found", afterComp); //$NON-NLS-1$

		IDelta delta = ApiComparator.compare(beforeComp, afterComp, beforeBaseline, afterBaseline,
				VisibilityModifiers.ALL_VISIBILITIES, null);
		assertNotNull("compare returned null", delta); //$NON-NLS-1$
		return delta;
	}

	/**
	 * Generates a {@code public abstract} class {@code p.X} containing a
	 * single {@code public abstract} method named {@code foo} with the given
	 * generic signature.
	 * <p>
	 * The erased method descriptor is always {@code ()Ljava/util/List;}.
	 */
	private static byte[] generateClassWithMethod(String internalName, String genericSignature) {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, internalName, null,
				"java/lang/Object", null); //$NON-NLS-1$
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "foo", //$NON-NLS-1$
				METHOD_DESCRIPTOR, genericSignature, null);
		mv.visitEnd();
		cw.visitEnd();
		return cw.toByteArray();
	}

	/**
	 * Generates a {@code public} class {@code p.X} containing a single
	 * {@code public} field named {@code items} with the given generic
	 * signature.
	 * <p>
	 * The erased field descriptor is always {@code Ljava/util/List;}.
	 */
	private static byte[] generateClassWithField(String internalName, String genericSignature) {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null); //$NON-NLS-1$
		FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, "items", //$NON-NLS-1$
				FIELD_DESCRIPTOR, genericSignature, null);
		fv.visitEnd();
		cw.visitEnd();
		return cw.toByteArray();
	}

	/**
	 * Writes a minimal OSGi bundle directory structure under
	 * {@code rootDir/deltatest/} containing the given class at
	 * {@code p/X.class}.
	 */
	private static void writeBundleDir(File rootDir, byte[] classBytes) throws IOException {
		File bundleDir = new File(rootDir, BUNDLE_NAME);
		File metaInf = new File(bundleDir, "META-INF"); //$NON-NLS-1$
		File packageDir = new File(bundleDir, "p"); //$NON-NLS-1$
		metaInf.mkdirs();
		packageDir.mkdirs();

		try (PrintWriter pw = new PrintWriter(new File(metaInf, "MANIFEST.MF"))) { //$NON-NLS-1$
			pw.println("Manifest-Version: 1.0"); //$NON-NLS-1$
			pw.println("Bundle-ManifestVersion: 2"); //$NON-NLS-1$
			pw.println("Bundle-Name: deltatest Plug-in"); //$NON-NLS-1$
			pw.println("Bundle-SymbolicName: deltatest"); //$NON-NLS-1$
			pw.println("Bundle-Version: 1.0.0"); //$NON-NLS-1$
			pw.println("Export-Package: p"); //$NON-NLS-1$
		}

		try (PrintWriter pw = new PrintWriter(new File(bundleDir, ".api_description"))) { //$NON-NLS-1$
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			pw.println("<component name=\"deltatest\">"); //$NON-NLS-1$
			pw.println("  <plugin id=\"deltatest\"/>"); //$NON-NLS-1$
			pw.println("  <package name=\"p\">"); //$NON-NLS-1$
			pw.println("    <type name=\"X\"/>"); //$NON-NLS-1$
			pw.println("  </package>"); //$NON-NLS-1$
			pw.println("</component>"); //$NON-NLS-1$
		}

		Files.write(new File(packageDir, "X.class").toPath(), classBytes); //$NON-NLS-1$
	}

	private static void deleteRecursively(File dir) {
		if (dir == null || !dir.exists()) {
			return;
		}
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteRecursively(f);
				} else {
					f.delete();
				}
			}
		}
		dir.delete();
	}
}
