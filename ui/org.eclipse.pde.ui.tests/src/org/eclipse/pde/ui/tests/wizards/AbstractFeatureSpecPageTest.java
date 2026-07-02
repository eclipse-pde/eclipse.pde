/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.pde.ui.tests.wizards;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.wizards.feature.AbstractFeatureSpecPage;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureData;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AbstractFeatureSpecPageTest {

	private static final String FEATURE_ID = "test.feature.id"; //$NON-NLS-1$
	private static final String PROJECT_NAME = "test.project.name"; //$NON-NLS-1$

	private Shell fShell;

	@Before
	public void setUp() {
		fShell = new Shell(Display.getDefault());
	}

	@After
	public void tearDown() {
		if (fShell != null && !fShell.isDisposed()) {
			fShell.dispose();
		}
	}

	@Test
	public void testUpdateNameRelativeFields_calledIfPageIsNotValid() {
		AtomicBoolean updateNameCalled = new AtomicBoolean(false);
		boolean isValid = buildAndValidatePage(null, updateNameCalled);

		assertFalse("page should not be valid", isValid);
		assertTrue("updateNameRelativeFields() should be called when page is not valid", updateNameCalled.get());
	}

	@Test
	public void testUpdateNameRelativeFields_calledIfPageIsValid() {
		AtomicBoolean updateNameCalled = new AtomicBoolean(false);
		boolean isValid = buildAndValidatePage(PROJECT_NAME, updateNameCalled);

		assertTrue("page should be valid", isValid);
		assertTrue("updateNameRelativeFields() should be called when page is valid", updateNameCalled.get());
	}

	private boolean buildAndValidatePage(String projectName, AtomicBoolean updateNameCalled) {
		TestableFeatureSpecPage page = new TestableFeatureSpecPage(updateNameCalled);
		page.setInitialProjectName(projectName);
		page.setInitialId(FEATURE_ID);
		page.createContents(fShell);
		page.initialize();
		page.createControl(fShell);
		return page.validatePagePublic();
	}

	private static class TestableFeatureSpecPage extends AbstractFeatureSpecPage {

		private final AtomicBoolean updateNameCalled;

		TestableFeatureSpecPage(AtomicBoolean updateNameCalled) {
			this.updateNameCalled = updateNameCalled;
		}

		@Override
		protected void updateNameRelativeFields() {
			updateNameCalled.set(true);
		}

		@Override
		protected void createContents(Composite container) {
			createCommonInput(container);
			createInstallHandlerText(container);
		}

		@Override
		protected void initialize() {
			fFeatureNameText.setText("Test Feature"); //$NON-NLS-1$
			fFeatureVersionText.setText("1.0.0"); //$NON-NLS-1$
		}

		@Override
		protected void attachListeners(ModifyListener listener) {
			// No listeners needed for this test
		}

		@Override
		protected String getHelpId() {
			return ""; //$NON-NLS-1$
		}

		@Override
		protected void saveSettings(IDialogSettings settings) {
			// No settings to save for this test
		}

		@Override
		protected String validateContent() {
			return null;
		}

		@Override
		protected String getFeatureId() {
			return FEATURE_ID;
		}

		@Override
		public FeatureData getFeatureData() {
			FeatureData data = new FeatureData();
			data.id = getFeatureId();
			data.name = fFeatureNameText.getText();
			data.version = fFeatureVersionText.getText();
			return data;
		}

		public boolean validatePagePublic() {
			return validatePage();
		}
	}

}