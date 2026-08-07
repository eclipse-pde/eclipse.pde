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
package org.eclipse.pde.core.tests.internal;

import static java.util.Map.entry;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.plugin.PluginReference;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;

/**
 * Guards the Dependencies tab of the manifest editor against stale error
 * decorations. The decorations are computed by {@code PDELabelProvider} from
 * {@code isResolved()}, so they are only correct if the resolution result is
 * recomputed and if the editor is notified that the target platform changed.
 */
public class StaleDependencyResolutionTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Path targetWithBundleA;
	private Path targetWithoutBundleA;

	@Before
	public void setUp() throws IOException {
		targetWithBundleA = folder.newFolder("targetWithBundleA").toPath();
		targetWithoutBundleA = folder.newFolder("targetWithoutBundleA").toPath();
		// ensure the PluginModelManager is initialized before listening to it
		PluginModelManager.getInstance().getState();
	}

	/**
	 * A target reload never fires a {@link PluginModelDelta}, so the dependency
	 * sections cannot rely on plug-in model listeners alone. They also listen to
	 * {@link IStateDeltaListener#stateChanged}, which is the only notification a
	 * reload produces.
	 */
	@Test
	public void testTargetReloadNotifiesStateListeners() throws Exception {
		setTargetPlatform(targetWithBundleA, bundle("bundle.a", "1.0.0"));

		List<State> states = new CopyOnWriteArrayList<>();
		IStateDeltaListener listener = new IStateDeltaListener() {
			@Override
			public void stateResolved(StateDelta delta) {
				// not the notification the sections depend on for a reload
			}

			@Override
			public void stateChanged(State newState) {
				states.add(newState);
			}
		};
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.addStateDeltaListener(listener);
		try {
			setTargetPlatform(targetWithoutBundleA, bundle("bundle.b", "1.0.0"));

			assertNull("precondition: bundle.a must be gone from the target",
					PluginRegistry.findModel("bundle.a"));
			assertFalse("target reload did not notify the state listeners", states.isEmpty());
		} finally {
			manager.removeStateDeltaListener(listener);
		}
	}

	/**
	 * {@link PluginReference} must not memoize a looked-up plug-in. A required
	 * bundle that disappears from the target has to report itself as unresolved
	 * so the error decoration appears.
	 */
	@Test
	public void testResolutionIsRecomputedWhenBundleLeavesTarget() throws Exception {
		setTargetPlatform(targetWithBundleA, bundle("bundle.a", "1.0.0"));

		PluginReference reference = new PluginReference("bundle.a");
		assertTrue("precondition: bundle.a must resolve while it is in the target", reference.isResolved());

		setTargetPlatform(targetWithoutBundleA, bundle("bundle.b", "1.0.0"));

		assertNull("precondition: bundle.a must be gone from the target", PluginRegistry.findModel("bundle.a"));
		assertFalse("resolution result was not recomputed after bundle.a left the target", reference.isResolved());
	}

	/**
	 * The counterpart for imported packages. {@code ImportPackageObject} queries
	 * the live {@code PDEState} on every call, so its result follows a target
	 * change without any caching to invalidate.
	 */
	@Test
	public void testImportedPackageResolutionFollowsTarget() throws Exception {
		setTargetPlatform(targetWithBundleA, bundle("bundle.a", "1.0.0", entry(EXPORT_PACKAGE, "bundle.a.pack")));

		ImportPackageObject importedPackage = importedPackage("bundle.a.pack");
		assertNotNull("precondition: bundle.a must be in the target", PluginRegistry.findModel("bundle.a"));
		assertTrue("bundle.a.pack must resolve while its exporter is in the target", importedPackage.isResolved());

		setTargetPlatform(targetWithoutBundleA, bundle("bundle.b", "1.0.0"));

		assertFalse("bundle.a.pack must not resolve after its exporter left the target",
				importedPackage.isResolved());
	}

	private static ImportPackageObject importedPackage(String packageName) throws CoreException {
		Document document = new Document();
		document.set("""
				Manifest-Version: 1.0
				Bundle-ManifestVersion: 2
				Bundle-SymbolicName: bundle.importer
				Bundle-Version: 1.0.0
				Import-Package: %s
				""".formatted(packageName));
		BundleModel model = new BundleModel(document, false);
		model.load();
		ImportPackageHeader header = (ImportPackageHeader) model.getBundle()
				.getManifestHeader(Constants.IMPORT_PACKAGE);
		return header.getPackage(packageName);
	}

	@SafeVarargs
	private static void setTargetPlatform(Path jarDirectory,
			Map.Entry<NameVersionDescriptor, Map<String, String>>... bundles) throws Exception {
		TargetPlatformUtil.setDummyBundlesAsTarget(Map.ofEntries(bundles), List.of(), jarDirectory);
	}
}
