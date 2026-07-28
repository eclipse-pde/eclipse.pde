/*******************************************************************************
 *  Copyright (c) 2026 Lars Vogel and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.core.builders;

import static java.util.Map.entry;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;
import org.eclipse.pde.ui.tests.launcher.AbstractLaunchTest;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

/**
 * Tests for {@link DependencyLoopFinder}, the engine behind the MANIFEST.MF
 * editor's "Look for cycles in the dependency graph" action.
 * <p>
 * The graph for each test is set up as a set of dummy target bundles whose
 * manifest headers define the edges. The finder resolves those edges through
 * {@link org.eclipse.pde.core.plugin.PluginRegistry}, so the dummy bundles
 * exercise exactly the production code path.
 */
public class DependencyLoopFinderTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Path tpJarDirectory;

	@Before
	public void setupBefore() throws IOException {
		tpJarDirectory = folder.newFolder("TPJarDirectory").toPath();
		// ensure the PluginModelManager (and therefore PluginRegistry) is initialized
		PluginModelManager.getInstance().getState();
	}

	@Test
	public void testNoCycleReturnsNoLoops() throws Exception {
		// a -> b -> c, no edge back
		setTargetPlatform( //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.c")), //
				bundle("loop.c", "1.0.0"));

		assertEquals(List.of(), loopSignatures("loop.a"));
		assertEquals(List.of(), loopSignatures("loop.c"));
	}

	@Test
	public void testDirectMutualDependency() throws Exception {
		// a <-> b
		setTargetPlatform( //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a")));

		assertEquals(List.of("loop.a -> loop.b"), loopSignatures("loop.a"));
	}

	@Test
	public void testThreeBundleCycle() throws Exception {
		// a -> b -> c -> a
		setTargetPlatform( //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.c")), //
				bundle("loop.c", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a")));

		assertEquals(List.of("loop.a -> loop.b -> loop.c"), loopSignatures("loop.a"));
	}

	@Test
	public void testTwoSeparateCyclesThroughRoot() throws Exception {
		// r -> a -> r and r -> b -> r are two distinct cycles through the root
		setTargetPlatform( //
				bundle("loop.r", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a,loop.b")), //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.r")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.r")));

		assertEquals(List.of("loop.r -> loop.a", "loop.r -> loop.b"), loopSignatures("loop.r"));
	}

	/**
	 * A cycle that is only reachable through a second dependency of the root
	 * must be reported too.
	 *
	 * <pre>
	 *   r -> a, r -> d
	 *   a -> b, a -> r
	 *   b -> a
	 *   d -> b
	 * </pre>
	 *
	 * Two cycles pass through {@code r}: {@code r -> a -> r} and
	 * {@code r -> d -> b -> a -> r}. Reaching {@code b} from {@code a} ends in
	 * a cycle that does not touch {@code r}, which must not stop the search
	 * from reaching {@code b} again through {@code d}.
	 */
	@Test
	public void testCycleReachableOnlyViaSecondImportPathIsFound() throws Exception {
		setTargetPlatform( //
				bundle("loop.r", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a,loop.d")), //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b,loop.r")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a")), //
				bundle("loop.d", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")));

		assertEquals(List.of("loop.r -> loop.a", "loop.r -> loop.d -> loop.b -> loop.a"),
				loopSignatures("loop.r"));
	}

	/**
	 * The reported cycles must not depend on the order in which the root
	 * declares its dependencies.
	 *
	 * <pre>
	 *   r -> d, r -> a
	 *   a -> b, a -> r
	 *   b -> a
	 *   d -> b
	 * </pre>
	 */
	@Test
	public void testDetectedCyclesDoNotDependOnDeclarationOrder() throws Exception {
		setTargetPlatform( //
				bundle("loop.r", "1.0.0", entry(REQUIRE_BUNDLE, "loop.d,loop.a")), //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b,loop.r")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.a")), //
				bundle("loop.d", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")));

		assertEquals(List.of("loop.r -> loop.a", "loop.r -> loop.d -> loop.b -> loop.a"),
				loopSignatures("loop.r"));
	}

	/**
	 * A cycle that does not pass through the root is not reported, and must not
	 * hide the cycle that does: {@code b <-> c} sits beside the
	 * {@code r -> a -> r} cycle.
	 */
	@Test
	public void testCycleBesideTheRootDoesNotHideTheRootCycle() throws Exception {
		setTargetPlatform( //
				bundle("loop.r", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b,loop.a")), //
				bundle("loop.a", "1.0.0", entry(REQUIRE_BUNDLE, "loop.r")), //
				bundle("loop.b", "1.0.0", entry(REQUIRE_BUNDLE, "loop.c")), //
				bundle("loop.c", "1.0.0", entry(REQUIRE_BUNDLE, "loop.b")));

		assertEquals(List.of("loop.r -> loop.a"), loopSignatures("loop.r"));
	}

	// --- utility methods ---

	@SafeVarargs
	private void setTargetPlatform(Map.Entry<NameVersionDescriptor, Map<String, String>>... pluginDescriptions)
			throws Exception {
		TargetPlatformUtil.setDummyBundlesAsTarget(Map.ofEntries(pluginDescriptions), List.of(), tpJarDirectory);
	}

	/**
	 * Runs the finder from the given root bundle and returns the detected loops
	 * as readable, sorted "id -> id -> ..." signatures.
	 */
	private static List<String> loopSignatures(String rootId) {
		DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin(rootId));
		return Arrays.stream(loops) //
				.map(loop -> Arrays.stream(loop.getMembers()) //
						.map(IPluginBase::getId) //
						.collect(Collectors.joining(" -> "))) //
				.sorted() //
				.collect(Collectors.toList());
	}

	private static IPlugin plugin(String id) {
		IPluginModelBase model = AbstractLaunchTest.findTargetModel(id, "1.0.0");
		assertNotNull("expected target model for " + id, model);
		return (IPlugin) model.getPluginBase();
	}
}
