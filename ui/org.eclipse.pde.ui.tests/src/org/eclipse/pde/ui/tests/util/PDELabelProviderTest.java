/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.plugin.PluginImport;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Verifies that {@link PDELabelProvider} does not resolve the target platform
 * while decorating labels.
 */
public class PDELabelProviderTest {

	private static final String UNRESOLVED_ID = "org.eclipse.pde.tests.does.not.exist"; //$NON-NLS-1$
	private static final String VERSION = "1.0.0"; //$NON-NLS-1$

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	@Rule
	public final TestRule deleteCreatedProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private ImportObject fUnresolvedImport;

	@Before
	public void setUp() {
		WorkspacePluginModel model = new WorkspacePluginModel(null, false);
		fUnresolvedImport = new ImportObject(new PluginImport(model, UNRESOLVED_ID));
	}

	/**
	 * Once the models are available an unresolved import must still be decorated
	 * with the error overlay.
	 */
	@Test
	public void testUnresolvedImportIsDecoratedWhenModelsAreAvailable() {
		PluginRegistry.findEntry(UNRESOLVED_ID);
		assertTrue("plug-in models should be initialized", //$NON-NLS-1$
				PDECore.getDefault().getModelManager().isInitialized());

		PDELabelProvider labelProvider = new PDELabelProvider();
		try {
			Image plain = labelProvider.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
			assertNotSame("unresolved import should be decorated", plain, //$NON-NLS-1$
					labelProvider.getImage(fUnresolvedImport));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * As long as the models are not available the plain image must be returned
	 * instead of querying the plug-in registry.
	 */
	@Test
	public void testImportIsUndecoratedWhileModelsAreUnavailable() {
		PDELabelProvider labelProvider = newProviderWithoutPluginModels();
		try {
			Image plain = labelProvider.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
			assertSame("import must not be decorated before the models are known", plain, //$NON-NLS-1$
					labelProvider.getImage(fUnresolvedImport));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * The image of a feature plug-in must not wait for the plug-in models, which
	 * isFragment() resolves just like the error overlay does.
	 */
	@Test
	public void testFeaturePluginImageDoesNotWaitForPluginModels() throws Exception {
		IFeaturePlugin featurePlugin = createFeaturePlugin(UNRESOLVED_ID);
		Object entriesLock = ConcurrencyUtil.readLock(PDECore.getDefault().getModelManager(),
				"fEntriesSynchronizer"); //$NON-NLS-1$

		PDELabelProvider labelProvider = newProviderWithoutPluginModels();
		try {
			ConcurrencyUtil.assertReturnsWhileLockIsHeld(
					"feature plug-in image must not wait for the plug-in models", entriesLock, //$NON-NLS-1$
					() -> labelProvider.getImage(featurePlugin));
			Image plain = labelProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
			assertSame("feature plug-in must not be decorated before the models are known", plain, //$NON-NLS-1$
					labelProvider.getImage(featurePlugin));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * Once the feature models are available a feature that cannot be found must
	 * still be decorated with the error overlay.
	 */
	@Test
	public void testUnresolvedFeatureIsDecoratedWhenModelsAreAvailable() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		manager.getModels();
		assertTrue("feature models should be initialized", manager.isInitialized()); //$NON-NLS-1$

		PDELabelProvider labelProvider = new PDELabelProvider();
		try {
			Image plain = labelProvider.get(PDEPluginImages.DESC_FEATURE_OBJ);
			assertNotSame("unresolved feature should be decorated", plain, //$NON-NLS-1$
					labelProvider.getImage(createProductFeature(UNRESOLVED_ID)));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * As long as the feature models are not available the plain image must be
	 * returned instead of querying the feature model manager.
	 */
	@Test
	public void testFeatureIsUndecoratedWhileModelsAreUnavailable() {
		PDELabelProvider labelProvider = newProviderWithoutFeatureModels();
		try {
			Image plain = labelProvider.get(PDEPluginImages.DESC_FEATURE_OBJ);
			assertSame("feature must not be decorated before the models are known", plain, //$NON-NLS-1$
					labelProvider.getImage(createProductFeature(UNRESOLVED_ID)));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * The label of a site feature falls back to its URL while the feature models
	 * are unavailable.
	 */
	@Test
	public void testSiteFeatureFallsBackToUrlWhileModelsAreUnavailable() throws Exception {
		ISiteFeature siteFeature = createSiteFeature(UNRESOLVED_ID);

		PDELabelProvider labelProvider = newProviderWithoutFeatureModels();
		try {
			assertEquals("site feature must not be resolved before the models are known", siteFeature.getURL(), //$NON-NLS-1$
					labelProvider.getObjectText(siteFeature));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * Once the feature models are available the site feature is labeled with the
	 * feature it points to, not with its URL.
	 */
	@Test
	public void testSiteFeatureIsResolvedWhenModelsAreAvailable() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();
		String id = "org.eclipse.pde.tests.label.feature"; //$NON-NLS-1$
		ProjectUtils.createFeatureProject(id, VERSION, f -> {
		});
		IFeatureModel featureModel = PDECore.getDefault().getFeatureModelManager().findFeatureModel(id, VERSION);
		assertNotNull("feature model should be known to the manager", featureModel); //$NON-NLS-1$

		ISiteFeature siteFeature = createSiteFeature(id);

		PDELabelProvider labelProvider = new PDELabelProvider();
		try {
			assertEquals("site feature should be labeled with the resolved feature", //$NON-NLS-1$
					labelProvider.getObjectText(featureModel), labelProvider.getObjectText(siteFeature));
		} finally {
			labelProvider.dispose();
		}
	}

	/**
	 * The image of an extension element must not wait for the plug-in models,
	 * which the schema lookup builds the extension registry from.
	 */
	@Test
	public void testExtensionElementImageDoesNotWaitForPluginModels() throws Exception {
		IPluginElement element = createExtensionElement("org.eclipse.ui.views"); //$NON-NLS-1$
		Object entriesLock = ConcurrencyUtil.readLock(PDECore.getDefault().getModelManager(),
				"fEntriesSynchronizer"); //$NON-NLS-1$

		PDELabelProvider labelProvider = newProviderWithoutPluginModels();
		try {
			ConcurrencyUtil.assertReturnsWhileLockIsHeld(
					"extension element image must not wait for the plug-in models", entriesLock, //$NON-NLS-1$
					() -> labelProvider.getImage(element));
		} finally {
			labelProvider.dispose();
		}
	}

	private static IPluginElement createExtensionElement(String point) throws Exception {
		WorkspacePluginModel model = new WorkspacePluginModel(null, false);
		model.setEnabled(true);
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint(point);
		IPluginElement element = model.getFactory().createElement(extension);
		element.setName("view"); //$NON-NLS-1$
		extension.add(element);
		return element;
	}

	private static PDELabelProvider newProviderWithoutPluginModels() {
		return new PDELabelProvider() {
			@Override
			public boolean arePluginModelsAvailable() {
				return false;
			}
		};
	}

	private static IFeaturePlugin createFeaturePlugin(String id) throws Exception {
		IFeaturePlugin plugin = new WorkspaceFeatureModel().getFactory().createPlugin();
		plugin.setId(id);
		plugin.setVersion(VERSION);
		return plugin;
	}

	private static PDELabelProvider newProviderWithoutFeatureModels() {
		return new PDELabelProvider() {
			@Override
			public boolean areFeatureModelsAvailable() {
				return false;
			}
		};
	}

	private static IProductFeature createProductFeature(String id) {
		IProductFeature feature = new WorkspaceProductModel(null, false).getFactory().createFeature();
		feature.setId(id);
		feature.setVersion(VERSION);
		return feature;
	}

	private static ISiteFeature createSiteFeature(String id) throws Exception {
		ISiteFeature feature = new WorkspaceSiteModel(null).getFactory().createFeature();
		feature.setId(id);
		feature.setVersion(VERSION);
		feature.setURL("features/" + id + '_' + VERSION + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		return feature;
	}
}
