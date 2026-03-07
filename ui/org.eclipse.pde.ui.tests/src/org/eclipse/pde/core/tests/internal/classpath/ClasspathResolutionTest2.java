/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Laeubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Comprehensive test for PDE's RequiredPluginsClasspathContainer validating
 * that OSGi module layer visibility is correctly mapped to JDT classpath
 * entries with appropriate access rules.
 * <p>
 * <b>Core principle under test:</b> The Java compiler needs complete type
 * hierarchies for compilation, including types from transitively required
 * bundles. PDE adds these transitive dependencies to the JDT classpath with
 * {@code K_NON_ACCESSIBLE} (forbidden) access rules, so the compiler can
 * resolve types for hierarchy/signature validation while preventing direct
 * usage via forbidden reference markers.
 * <p>
 * <b>Test bundle dependency graph:</b>
 *
 * <pre>
 * Bundle A:  Import-Package: b.api, g.api
 * Bundle B:  Export-Package: b.api
 *            Import-Package: d.api, f.api(optional)
 *            Require-Bundle: C, E(optional), G(reexport)
 * Bundle C:  Export-Package: c.api     (leaf)
 * Bundle D:  Export-Package: d.api     (leaf)
 * Bundle E:  Export-Package: e.api     (leaf)
 * Bundle F:  Export-Package: f.api     (leaf)
 * Bundle G:  Export-Package: g.api; Import-Package: h.api(optional)
 * Bundle H:  Export-Package: h.api     (leaf)
 * </pre>
 *
 * Each bundle also has an internal package (e.g. {@code b.internal}) that is
 * <b>not</b> listed in Export-Package.
 * <p>
 * <b>OSGi Core R8 spec references:</b>
 * <ul>
 * <li>§3.6.4 Import-Package: Wires specific packages, never re-exports.</li>
 * <li>§3.6.5 Export-Package: Only exported packages are visible to
 * importers.</li>
 * <li>§3.9.4 Class Loading: Import-Package (step 3) → Require-Bundle (step 4) →
 * bundle classpath (step 5). Packages not wired are not accessible at
 * <em>runtime</em>.</li>
 * <li>§3.13.1 Require-Bundle visibility directive: Default
 * {@code visibility:=private} does NOT re-export. Only
 * {@code visibility:=reexport} makes packages transitively visible.</li>
 * <li>§3.7.5 Optional dependencies: {@code resolution:=optional} does not
 * change visibility semantics.</li>
 * </ul>
 * <p>
 * <b>JDT access rules
 * (https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/concepts/concept-access-rules.htm):</b>
 * <ul>
 * <li>{@code K_ACCESSIBLE}: Exported packages from directly imported/required
 * bundles → no problem marker</li>
 * <li>{@code K_DISCOURAGED}: Packages marked with {@code x-internal:=true} or
 * restricted via {@code x-friends} → discouraged access warning</li>
 * <li>{@code K_NON_ACCESSIBLE}: Catch-all (**&#47;* with IGNORE_IF_BETTER) for
 * non-exported packages, or all-forbidden for transitive deps → forbidden
 * reference marker (error by default, configurable to warning via
 * {@code forbiddenReference=warning})</li>
 * </ul>
 * <p>
 * <b>PDE implementation details:</b>
 * <ul>
 * <li>{@code PDEClasspathContainer.EXCLUDE_ALL_RULE}: {@code **&#47;*} with
 * {@code K_NON_ACCESSIBLE | IGNORE_IF_BETTER} — catch-all forbidding
 * non-exported packages</li>
 * <li>{@code PDEClasspathContainer.getAccessRules(List)}: Creates
 * [ACCESSIBLE|DISCOURAGED..., EXCLUDE_ALL_RULE] arrays. For empty rules list,
 * returns just [EXCLUDE_ALL_RULE].</li>
 * <li>{@code RequiredPluginsClasspathContainer.addTransitiveDependenciesWithForbiddenAccess()}:
 * Uses {@code DependencyManager.findRequirementsClosure()} to find all
 * transitive deps, then adds each with {@code Map.of(desc, List.of())} (empty
 * rules) → single EXCLUDE_ALL_RULE → all types forbidden.</li>
 * <li>{@code RequiredPluginsClasspathContainer.retrieveVisiblePackagesFromState()}:
 * Queries Equinox {@code StateHelper.getVisiblePackages()} for packages
 * accessible to the bundle, translating to {@code Rule} objects.</li>
 * <li>Equinox {@code StateHelperImpl.getAccessCode()}: Returns
 * {@code ACCESS_DISCOURAGED} for {@code x-internal}/{@code x-friends}
 * restricted packages.</li>
 * </ul>
 *
 * @see <a href="https://github.com/eclipse-pde/eclipse.pde/pull/2218">PR #2218
 *      — Add transitive dependencies with forbidden access</a>
 * @see <a href=
 *      "https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html">OSGi
 *      Core R8 Module Layer</a>
 * @see <a href=
 *      "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/concepts/concept-access-rules.htm">
 *      JDT Access Rules</a>
 */
public class ClasspathResolutionTest2 {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private static IProject projectA;
	private static IProject projectAe;
	private static IClasspathEntry[] classpathEntriesA;

	/**
	 * Bundles providing packages directly imported by A via Import-Package.
	 * These appear on A's classpath with K_ACCESSIBLE rules for their exported
	 * packages and EXCLUDE_ALL_RULE as catch-all.
	 */
	static final List<String> DIRECTLY_IMPORTED_BUNDLES = List.of("B", "G");

	/**
	 * Bundles that are transitive dependencies of B or G. After PR #2218, these
	 * appear on A's classpath with a single {@code **&#47;*} K_NON_ACCESSIBLE
	 * rule (all types forbidden). They are needed for Java type hierarchy
	 * resolution but must not be directly referenced.
	 * <p>
	 * The single K_NON_ACCESSIBLE rule is the pattern used by
	 * {@code ClasspathContributorTest.isPdeDependency()} to identify transitive
	 * forbidden entries.
	 */
	static final List<String> TRANSITIVE_FORBIDDEN_BUNDLES = List.of("C", "D", "E", "F", "H");

	/** All test bundles other than A. */
	static final List<String> ALL_TEST_BUNDLES = List.of("B", "C", "D", "E", "F", "G", "H");

	@BeforeClass
	public static void setupBeforeClass() throws Exception {
		List<IProject> importedProjects = new ArrayList<>();

		// Import test bundles B-H (A's direct and transitive dependencies)
		for (String name : ALL_TEST_BUNDLES) {
			IProject project = ProjectUtils.importTestProject("tests/projects/" + name);
			importedProjects.add(project);
		}

		// Build leaf bundles first, complex bundles last
		for (IProject project : importedProjects.reversed()) {
			project.open(new NullProgressMonitor());
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		}
		TestUtils.processUIEvents(100);

		// Import and build project A last (Import-Package: b.api, g.api)
		// Project A has forbiddenReference=warning → forbidden refs are
		// warnings
		projectA = ProjectUtils.importTestProject("tests/projects/A");
		projectA.open(new NullProgressMonitor());
		projectA.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		// Import and build project Ae (same deps as A, but
		// forbiddenReference=error)
		// Project Ae uses the JDT default → forbidden refs are errors
		projectAe = ProjectUtils.importTestProject("tests/projects/Ae");
		projectAe.open(new NullProgressMonitor());
		projectAe.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		// Compute PDE classpath entries (only plugin dependencies, no
		// JRE/source)
		// See ClasspathComputer.computeClasspathEntries() which delegates to
		// RequiredPluginsClasspathContainer.computeEntries()
		IPluginModelBase modelA = PDECore.getDefault().getModelManager().findModel(projectA);
		assertNotNull("PDE model for project A must be available", modelA);
		classpathEntriesA = ClasspathComputer.computeClasspathEntries(modelA, projectA);

		TestUtils.processUIEvents(100);
	}

	// =========================================================================
	// Section 1: Classpath Entry Presence — directly imported bundles
	// =========================================================================

	/**
	 * Directly imported bundles must be on A's classpath.
	 * <p>
	 * OSGi §3.6.4: Import-Package wires to the exporting bundle. A imports
	 * b.api (exported by B) and g.api (exported by G), so both B and G must be
	 * present as classpath entries with appropriate access rules.
	 */
	@Test
	public void testDirectlyImportedBundlesOnClasspath() throws Exception {
		List<String> entryNames = getTestBundleNames(classpathEntriesA);

		assertThat(entryNames).as("B must be on A's classpath (A Import-Package: b.api → B exports b.api)")
		.contains("B");
		assertThat(entryNames).as("G must be on A's classpath (A Import-Package: g.api → G exports g.api)")
		.contains("G");
	}

	// =========================================================================
	// Section 2: Classpath Entry Presence — transitive forbidden dependencies
	// =========================================================================

	/**
	 * Transitive dependencies must also be on A's classpath, added with
	 * all-forbidden access rules by
	 * {@code RequiredPluginsClasspathContainer.addTransitiveDependenciesWithForbiddenAccess()}.
	 * <p>
	 * This is required because the Java compiler needs to resolve complete type
	 * hierarchies. Without these entries, the compiler would produce "The type
	 * X cannot be resolved. It is indirectly referenced from required type Y"
	 * errors — a well-known JDT compilation issue.
	 * <p>
	 * At <b>OSGi runtime</b> (§3.9.4), A's classloader cannot load types from
	 * these bundles. At <b>compile time</b>, they must be resolvable but
	 * forbidden.
	 */
	@Test
	public void testTransitiveDependenciesOnClasspath() throws Exception {
		List<String> entryNames = getTestBundleNames(classpathEntriesA);

		// C: Required by B (Require-Bundle: C, default visibility:=private)
		// §3.13.1: private visibility → not re-exported to A
		assertThat(entryNames).as("C must be on classpath as transitive forbidden dep "
				+ "(B Require-Bundle: C, visibility:=private §3.13.1)").contains("C");

		// D: Package imported by B (Import-Package: d.api)
		// §3.6.4: Import-Package never re-exports
		assertThat(entryNames).as("D must be on classpath as transitive forbidden dep "
				+ "(B Import-Package: d.api, never re-exports §3.6.4)").contains("D");

		// E: Optionally required by B (Require-Bundle: E;resolution:=optional)
		// §3.7.5 + §3.13.1: optional does not change visibility, still private
		assertThat(entryNames).as("E must be on classpath as transitive forbidden dep "
				+ "(B Require-Bundle: E;optional, visibility:=private §3.7.5)").contains("E");

		// F: Optionally imported by B (Import-Package:
		// f.api;resolution:=optional)
		// §3.7.5 + §3.6.4: optional import, still B's own import
		assertThat(entryNames)
		.as("F must be on classpath as transitive forbidden dep " + "(B Import-Package: f.api;optional §3.7.5)")
		.contains("F");

		// H: Optionally imported by G (Import-Package:
		// h.api;resolution:=optional)
		// §3.7.5: optional import, G's own import, not A's
		assertThat(entryNames)
		.as("H must be on classpath as transitive forbidden dep " + "(G Import-Package: h.api;optional §3.7.5)")
		.contains("H");
	}

	// =========================================================================
	// Section 3: Access Rules — directly imported bundles
	// =========================================================================

	/**
	 * Bundle B's access rules must reflect OSGi Export-Package visibility:
	 * <ol>
	 * <li>{@code b/api/*} → {@code K_ACCESSIBLE} (b.api is exported by B and
	 * imported by A — OSGi §3.6.5 + §3.6.4)</li>
	 * <li>{@code **&#47;*} → {@code K_NON_ACCESSIBLE} (catch-all
	 * EXCLUDE_ALL_RULE forbidding non-exported packages like b.internal)</li>
	 * </ol>
	 * <p>
	 * Created by {@code PDEClasspathContainer.getAccessRules()} using rules
	 * from
	 * {@code RequiredPluginsClasspathContainer.retrieveVisiblePackagesFromState()}.
	 * Equinox {@code StateHelperImpl.getAccessCode()} determines which packages
	 * are exported (ACCESSIBLE) vs internal (DISCOURAGED).
	 * <p>
	 * JDT processes rules in order, first match wins. The EXCLUDE_ALL_RULE's
	 * IGNORE_IF_BETTER flag allows more specific rules from other entries to
	 * override it.
	 */
	@Test
	public void testAccessRulesForBundle_B() throws Exception {
		IClasspathEntry entryB = findEntry("B");
		assertNotNull("Bundle B must be on the classpath", entryB);

		IAccessRule[] rules = entryB.getAccessRules();

		// Exactly 2 rules: exported package + EXCLUDE_ALL catch-all
		assertThat(rules).as("B must have exactly 2 access rules: " + "[b/api/* K_ACCESSIBLE, **/* K_NON_ACCESSIBLE]")
		.hasSize(2);

		// Rule 0: b/api/* → K_ACCESSIBLE (exported + imported)
		assertThat(rules[0].getPattern().toString()).as("First rule for B must match exported package pattern b/api/*")
		.isEqualTo("b/api/*");
		assertThat(rules[0].getKind())
		.as("b/api/* must be K_ACCESSIBLE " + "(B exports b.api per §3.6.5, A imports it per §3.6.4)")
		.isEqualTo(IAccessRule.K_ACCESSIBLE);

		// Rule 1: **/* → K_NON_ACCESSIBLE (EXCLUDE_ALL_RULE)
		// This catches b.internal and all other non-exported packages
		assertThat(rules[1].getPattern().toString()).as("Last rule for B must be the EXCLUDE_ALL catch-all **/*")
		.isEqualTo("**/*");
		assertThat(rules[1].getKind())
		.as("Catch-all must be K_NON_ACCESSIBLE " + "(non-exported packages not visible per §3.6.5)")
		.isEqualTo(IAccessRule.K_NON_ACCESSIBLE);
	}

	/**
	 * Bundle G's access rules — same structure as B.
	 * <ol>
	 * <li>{@code g/api/*} → {@code K_ACCESSIBLE}</li>
	 * <li>{@code **&#47;*} → {@code K_NON_ACCESSIBLE}</li>
	 * </ol>
	 * <p>
	 * Note: B Require-Bundle's G with {@code visibility:=reexport}, but since A
	 * uses Import-Package (not Require-Bundle) to access B, the reexport does
	 * not affect A (§3.13.1). A sees g.api through its own Import-Package:
	 * g.api declaration.
	 */
	@Test
	public void testAccessRulesForBundle_G() throws Exception {
		IClasspathEntry entryG = findEntry("G");
		assertNotNull("Bundle G must be on the classpath", entryG);

		IAccessRule[] rules = entryG.getAccessRules();

		assertThat(rules).as("G must have exactly 2 access rules: " + "[g/api/* K_ACCESSIBLE, **/* K_NON_ACCESSIBLE]")
		.hasSize(2);

		// Rule 0: g/api/* → K_ACCESSIBLE
		assertThat(rules[0].getPattern().toString()).isEqualTo("g/api/*");
		assertThat(rules[0].getKind())
		.as("g/api/* must be K_ACCESSIBLE " + "(G exports g.api per §3.6.5, A imports it per §3.6.4)")
		.isEqualTo(IAccessRule.K_ACCESSIBLE);

		// Rule 1: **/* → K_NON_ACCESSIBLE (EXCLUDE_ALL_RULE)
		assertThat(rules[1].getPattern().toString()).isEqualTo("**/*");
		assertThat(rules[1].getKind()).as("Catch-all must be K_NON_ACCESSIBLE").isEqualTo(IAccessRule.K_NON_ACCESSIBLE);
	}

	// =========================================================================
	// Section 4: Access Rules — transitive forbidden dependencies
	// =========================================================================

	/**
	 * Transitive dependencies must have exactly ONE access rule: the
	 * EXCLUDE_ALL_RULE ({@code **&#47;* K_NON_ACCESSIBLE}).
	 * <p>
	 * This is the pattern created by
	 * {@code RequiredPluginsClasspathContainer.addTransitiveDependenciesWithForbiddenAccess()}:
	 * it passes {@code Map.of(desc, List.of())} (empty rules list) to
	 * {@code addPlugin()}, which calls {@code getAccessRules(List.of())}
	 * returning just {@code [EXCLUDE_ALL_RULE]}.
	 * <p>
	 * This single K_NON_ACCESSIBLE rule pattern is also used by
	 * {@code ClasspathContributorTest.isPdeDependency()} to identify transitive
	 * forbidden entries (see
	 * {@code rules.length == 1 && rules[0].getKind() == K_NON_ACCESSIBLE}).
	 * <p>
	 * At <b>runtime</b>, these bundles' types would not be loadable by A's
	 * classloader (OSGi §3.9.4). At <b>compile time</b>, they are resolvable
	 * but any direct reference produces a forbidden reference marker.
	 */
	@Test
	public void testTransitiveDependenciesHaveAllForbiddenAccessRules() throws Exception {
		for (String bundleName : TRANSITIVE_FORBIDDEN_BUNDLES) {
			IClasspathEntry entry = findEntry(bundleName);
			assertNotNull("Transitive bundle " + bundleName + " must be on classpath", entry);

			IAccessRule[] rules = entry.getAccessRules();

			// Exactly 1 rule: **/* K_NON_ACCESSIBLE
			// (PDEClasspathContainer.getAccessRules(List.of()) →
			// [EXCLUDE_ALL_RULE])
			assertThat(rules)
			.as("Transitive bundle %s must have exactly 1 access rule " + "(the EXCLUDE_ALL_RULE)", bundleName)
			.hasSize(1);

			assertThat(rules[0].getPattern().toString())
			.as("The single rule for transitive bundle %s must be " + "the **/* catch-all pattern", bundleName)
			.isEqualTo("**/*");

			assertThat(rules[0].getKind())
			.as("Transitive bundle %s must be K_NON_ACCESSIBLE "
					+ "(all types forbidden — not directly wired per §3.9.4)", bundleName)
			.isEqualTo(IAccessRule.K_NON_ACCESSIBLE);
		}
	}

	// =========================================================================
	// Section 5: Compilation Markers — exact expected markers
	// =========================================================================

	/**
	 * Validates all compilation markers on project A (which has
	 * {@code forbiddenReference=warning} in its JDT compiler settings). All
	 * forbidden reference markers must be at {@code WARNING} severity.
	 *
	 * @see #assertExpectedCompilationMarkers(IProject, int)
	 */
	@Test
	public void testExpectedCompilationMarkers_forbiddenIsWarning() throws Exception {
		assertExpectedCompilationMarkers(projectA, IMarker.SEVERITY_WARNING);
	}

	/**
	 * Validates all compilation markers on project Ae (which has
	 * {@code forbiddenReference=error} in its JDT compiler settings — the JDT
	 * default).
	 * <p>
	 * All forbidden reference markers must be at {@code ERROR} severity. This
	 * validates the JDT default behavior: forbidden references are compilation
	 * errors unless explicitly downgraded to warnings.
	 *
	 * @see #assertExpectedCompilationMarkers(IProject, int)
	 */
	@Test
	public void testExpectedCompilationMarkers_forbiddenIsError() throws Exception {
		assertExpectedCompilationMarkers(projectAe, IMarker.SEVERITY_ERROR);
	}

	/**
	 * Validates ALL compilation markers on the given project against exact
	 * expected values. This verifies three things simultaneously:
	 * <ol>
	 * <li><b>No "cannot be resolved" errors:</b> All types are resolvable
	 * because transitive dependencies are on the classpath (PR #2218). Before
	 * PR #2218, transitive types caused "The type X cannot be resolved. It is
	 * indirectly referenced from required type Y" errors.</li>
	 * <li><b>No markers on accessible types:</b> b.api.MyObject (line 29) and
	 * g.api.MyObject (line 34) produce zero markers because they match
	 * {@code K_ACCESSIBLE} access rules for exported packages (§3.6.5).</li>
	 * <li><b>Forbidden reference markers on all non-accessible types:</b> Every
	 * type from non-exported packages of direct deps (b.internal, g.internal)
	 * and from transitive-only bundles (C, D, E, F, H) produces exactly 2
	 * forbidden reference markers (type + constructor). The severity depends on
	 * the project's {@code forbiddenReference} compiler setting.</li>
	 * </ol>
	 * <p>
	 * Each marker is verified for: line number, severity (parameterized),
	 * problem ID ({@link IProblem#ForbiddenReference}), marker type
	 * ({@link IJavaModelMarker#JAVA_MODEL_PROBLEM_MARKER}), and message pattern
	 * including the restricting project name.
	 *
	 * @param project
	 *            the project to verify (A with warning or Ae with error)
	 * @param expectedSeverity
	 *            {@link IMarker#SEVERITY_WARNING} or
	 *            {@link IMarker#SEVERITY_ERROR}
	 */
	private void assertExpectedCompilationMarkers(IProject project, int expectedSeverity) throws Exception {
		String severityLabel = expectedSeverity == IMarker.SEVERITY_WARNING ? "WARNING" : "ERROR";

		// All expected forbidden references in AClass.java. Each reference
		// produces exactly TWO JDT markers: one for the type access
		// ("The type 'MyObject' is not API") and one for the constructor
		// call ("The constructor 'MyObject()' is not API").
		//
		// Lines 29 (b.api.MyObject) and 34 (g.api.MyObject) are
		// K_ACCESSIBLE and must have NO markers. The total count assertion
		// below implicitly validates this — any extra markers would
		// increase the count.
		record ForbiddenRef(int line, String qualifiedType, String project) {
		}
		List<ForbiddenRef> expected = List.of(
				// Non-exported packages of directly imported bundles:
				// Caught by EXCLUDE_ALL_RULE (**/* K_NON_ACCESSIBLE) on
				// B/G entries. Non-exported packages are not returned by
				// StateHelper.getVisiblePackages() — they have no explicit
				// rule, so the catch-all EXCLUDE_ALL fires (§3.6.5).
				new ForbiddenRef(31, "b.internal.MyObject", "B"), new ForbiddenRef(36, "g.internal.MyObject", "G"),
				// Transitive forbidden: ALL packages forbidden via the
				// single **/* K_NON_ACCESSIBLE rule added by
				// addTransitiveDependenciesWithForbiddenAccess().
				// At OSGi runtime, A's classloader cannot load these
				// (§3.9.4).
				//
				// C: Required by B (Require-Bundle: C,
				// visibility:=private §3.13.1)
				new ForbiddenRef(45, "c.api.MyObject", "C"), new ForbiddenRef(46, "c.internal.MyObject", "C"),
				// D: Package imported by B (Import-Package: d.api §3.6.4
				// — never re-exports)
				new ForbiddenRef(49, "d.api.MyObject", "D"), new ForbiddenRef(50, "d.internal.MyObject", "D"),
				// E: Optionally required by B (Require-Bundle: E;optional
				// §3.7.5 + §3.13.1)
				new ForbiddenRef(53, "e.api.MyObject", "E"), new ForbiddenRef(54, "e.internal.MyObject", "E"),
				// F: Optionally imported by B (Import-Package:
				// f.api;optional §3.7.5 + §3.6.4)
				new ForbiddenRef(57, "f.api.MyObject", "F"), new ForbiddenRef(58, "f.internal.MyObject", "F"),
				// H: Optionally imported by G (Import-Package:
				// h.api;optional §3.7.5)
				new ForbiddenRef(61, "h.api.MyObject", "H"), new ForbiddenRef(62, "h.internal.MyObject", "H"));

		IMarker[] allMarkers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
				IResource.DEPTH_INFINITE);

		// Each forbidden reference produces exactly 2 markers (type +
		// constructor). The total count also implicitly asserts that NO
		// markers exist for K_ACCESSIBLE references on lines 29 and 34.
		assertThat(allMarkers).as(
				"Project %s must have exactly %d JDT markers: " + "%d forbidden references × 2 (type + "
						+ "constructor). Zero 'cannot be resolved' " + "errors, zero markers on accessible types.",
						project.getName(), expected.size() * 2, expected.size()).hasSize(expected.size() * 2);

		for (ForbiddenRef ref : expected) {
			List<IMarker> atLine = Arrays.stream(allMarkers)
					.filter(m -> m.getAttribute(IMarker.LINE_NUMBER, -1) == ref.line).toList();

			assertThat(atLine).as("Line %d (%s on project '%s'): expected exactly" + " 2 markers (type + constructor)",
					ref.line, ref.qualifiedType, ref.project).hasSize(2);

			for (IMarker m : atLine) {
				// Severity depends on project's forbiddenReference
				// setting:
				// A has forbiddenReference=warning → WARNING
				// Ae has forbiddenReference=error → ERROR
				assertThat(m.getAttribute(IMarker.SEVERITY, -1))
						.as("Line %d in project %s: must be %s " + "severity (forbiddenReference=%s in "
								+ "project settings)",
								ref.line, project.getName(), severityLabel, severityLabel.toLowerCase())
				.isEqualTo(expectedSeverity);

				// Problem ID must be ForbiddenReference because all
				// restrictions use K_NON_ACCESSIBLE (not K_DISCOURAGED)
				assertThat(m.getAttribute(IJavaModelMarker.ID, -1))
				.as("Line %d: must be " + "IProblem.ForbiddenReference " + "(K_NON_ACCESSIBLE access rule)",
						ref.line)
				.isEqualTo(IProblem.ForbiddenReference);

				// Message must reference the restricting project
				String msg = m.getAttribute(IMarker.MESSAGE, "");
				assertThat(msg)
				.as("Line %d: message must reference " + "restricting project '%s'", ref.line, ref.project)
				.contains("restriction on required project '" + ref.project + "'");
			}

			// One marker for the type access, one for the constructor
			List<String> messages = atLine.stream().map(m -> m.getAttribute(IMarker.MESSAGE, "")).toList();
			assertThat(messages).as("Line %d: must have type access marker for " + "%s", ref.line, ref.qualifiedType)
			.anyMatch(msg -> msg.contains("The type 'MyObject' is not API"));
			assertThat(messages).as("Line %d: must have constructor access " + "marker", ref.line)
			.anyMatch(msg -> msg.contains("The constructor 'MyObject()' is not " + "API"));
		}
	}

	// =========================================================================
	// Section 6: Cross-validation
	// =========================================================================

	/**
	 * All dependency bundles (B-H) must build without any compilation problems
	 * — no errors AND no warnings. Each bundle's own dependencies are properly
	 * declared in its manifest, and their source code does not reference any
	 * forbidden types.
	 */
	@Test
	public void testDependencyBundlesBuildClean() throws Exception {
		for (String bundleName : ALL_TEST_BUNDLES) {
			IProject project = getProject(bundleName);
			IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

			List<String> errors = Arrays.stream(markers)
					.filter(m -> m.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
					.map(m -> m.getAttribute(IMarker.MESSAGE, "")).toList();
			assertThat(errors).as("Bundle %s must build without errors", bundleName).isEmpty();

			List<String> warnings = Arrays.stream(markers)
					.filter(m -> m.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
					.map(m -> m.getAttribute(IMarker.MESSAGE, "")).toList();
			assertThat(warnings).as("Bundle %s must build without warnings", bundleName).isEmpty();
		}
	}

	/**
	 * Classpath computation must be deterministic — recomputing should produce
	 * the same set of test bundle entries.
	 */
	@Test
	public void testClasspathComputationIsDeterministic() throws Exception {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(projectA);
		IClasspathEntry[] freshEntries = ClasspathComputer.computeClasspathEntries(model, projectA);

		List<String> freshNames = getTestBundleNames(freshEntries);
		List<String> originalNames = getTestBundleNames(classpathEntriesA);

		assertThat(freshNames).as("ClasspathComputer must produce deterministic results")
		.containsExactlyInAnyOrderElementsOf(originalNames);
	}

	// =========================================================================
	// Utility methods
	// =========================================================================

	/**
	 * Returns test bundle names (B-H) found in the given classpath entries.
	 * Filters out non-test entries (platform bundles, implicit deps, etc.)
	 */
	private static List<String> getTestBundleNames(IClasspathEntry[] entries) {
		return Arrays.stream(entries).map(e -> e.getPath().lastSegment()).filter(ALL_TEST_BUNDLES::contains).sorted()
				.toList();
	}

	private static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	private static IClasspathEntry findEntry(String bundleName) {
		for (IClasspathEntry entry : classpathEntriesA) {
			if (entry.getPath().lastSegment().equals(bundleName)) {
				return entry;
			}
		}
		return null;
	}
}
