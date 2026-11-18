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
package org.eclipse.pde.ui.tests.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.product.CustomizationPage;
import org.eclipse.pde.internal.ui.editor.product.PreferencesSection;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ide.IDE;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class PreferencesSectionTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static final String PRODUCT_PLUGIN = "com.example.product";
	private static final String BRANDING_PLUGIN = "com.example.branding";
	private static final String INI = "plugin_customization.ini";

	private PDEFormEditor editor;

	@After
	public void closeEditor() {
		if (editor != null) {
			editor.close(false);
		}
	}

	@Test
	public void customizationFileInOtherPluginReplacesGeneratedFile() throws Exception {
		IProject product = ProjectUtils.createPluginProject(PRODUCT_PLUGIN, PRODUCT_PLUGIN, "1.0.0",
				(d, s) -> d.setSingleton(true));
		IProject branding = ProjectUtils.createPluginProject(BRANDING_PLUGIN, BRANDING_PLUGIN, "1.0.0");
		ProjectUtils.createFile(branding, IPath.fromOSString(INI), "org.eclipse.ui/SHOW_TRADITIONAL_STYLE_TABS=false\n");
		// a splash progress bar makes the sync store SHOW_PROGRESS_ON_STARTUP in a customization file
		IFile productFile = ProjectUtils.createFile(product, IPath.fromOSString("test.product"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<?pde version="3.5"?>
				<product name="Test" uid="test" id="com.example.product.product" application="org.eclipse.ui.ide.workbench" version="1.0.0" type="bundles">
				   <splash startupProgressRect="5,275,445,15" />
				</product>
				""");

		synchronize(productFile);
		assertTrue("without a customization file the sync generates one", product.getFile(INI).exists());
		product.getFile(INI).delete(true, null);

		editor = (PDEFormEditor) IDE.openEditor(PDEPlugin.getActivePage(), productFile,
				IPDEUIConstants.PRODUCT_EDITOR_ID);
		PDEFormPage page = (PDEFormPage) editor.setActivePage(CustomizationPage.PAGE_ID);
		PreferencesSection section = Arrays.stream(page.getManagedForm().getParts())
				.filter(PreferencesSection.class::isInstance).map(PreferencesSection.class::cast).findFirst()
				.orElseThrow();
		Text field = findText(section.getSection());
		assertNotNull(field);
		field.setText("/" + BRANDING_PLUGIN + "/" + INI);
		editor.doSave(new NullProgressMonitor());

		synchronize(productFile);
		assertFalse(product.getFile(INI).exists());
		assertTrue(read(product.getFile("plugin.xml"))
				.contains("value=\"platform:/plugin/" + BRANDING_PLUGIN + "/" + INI + "\""));
		String brandingIni = read(branding.getFile(INI));
		assertTrue(brandingIni.contains("SHOW_TRADITIONAL_STYLE_TABS=false"));
		assertTrue(brandingIni.contains("SHOW_PROGRESS_ON_STARTUP"));

		// every launch synchronizes again, which must not bring the generated file back
		synchronize(productFile);
		assertFalse(product.getFile(INI).exists());
		assertEquals(brandingIni, read(branding.getFile(INI)));
	}

	private static void synchronize(IFile productFile) throws Exception {
		WorkspaceProductModel model = new WorkspaceProductModel(productFile, false);
		model.load();
		new SynchronizationOperation(model.getProduct(), null, productFile.getProject())
				.run(new NullProgressMonitor());
	}

	private static Text findText(Control control) {
		if (control instanceof Text text) {
			return text;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				Text text = findText(child);
				if (text != null) {
					return text;
				}
			}
		}
		return null;
	}

	private static String read(IFile file) throws Exception {
		try (var in = file.getContents()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
