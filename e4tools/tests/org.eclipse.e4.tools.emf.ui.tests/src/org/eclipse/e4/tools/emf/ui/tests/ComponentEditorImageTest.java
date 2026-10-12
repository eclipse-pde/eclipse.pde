/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.e4.tools.emf.ui.common.component.AbstractComponentEditor;
import org.eclipse.e4.tools.emf.ui.internal.common.ModelEditor;
import org.eclipse.e4.tools.services.IResourcePool;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ComponentEditorImageTest {

	private static final String KEY = "test_icon"; //$NON-NLS-1$

	private Image pooledImage;
	private IResourcePool pool;

	@BeforeEach
	public void setUp() {
		pooledImage = new Image(Display.getDefault(), 16, 16);
		pool = mock(IResourcePool.class);
		when(pool.getImageUnchecked(KEY)).thenReturn(pooledImage);
	}

	@AfterEach
	public void tearDown() {
		pooledImage.dispose();
	}

	@Test
	public void testClosingEditorKeepsPooledImages() {
		inRealm(() -> {
			ImageRegistry closedRegistry = new ImageRegistry();
			ImageRegistry openRegistry = new ImageRegistry();
			try {
				assertSame(pooledImage, componentEditor(closedRegistry).getImage(new Object(), KEY));

				closedRegistry.dispose();

				assertFalse(pooledImage.isDisposed(), "the pool shares this image with other editors"); //$NON-NLS-1$
				assertSame(pooledImage, componentEditor(openRegistry).getImage(new Object(), KEY));
			} finally {
				openRegistry.dispose();
			}
		});
	}

	@Test
	public void testClosingEditorDisposesOnlyItsGreyImages() {
		inRealm(() -> {
			ImageRegistry registry = new ImageRegistry();
			MPart hidden = MBasicFactory.INSTANCE.createPart();
			hidden.setVisible(false);

			Image grey = componentEditor(registry).getImage(hidden, KEY);
			assertNotSame(pooledImage, grey);

			registry.dispose();

			assertTrue(grey.isDisposed());
			assertFalse(pooledImage.isDisposed());
		});
	}

	private interface ReflectiveRunnable {
		void run() throws ReflectiveOperationException;
	}

	private static void inRealm(ReflectiveRunnable runnable) {
		Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), () -> {
			try {
				runnable.run();
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
	}

	private AbstractComponentEditor<Object> componentEditor(ImageRegistry registry)
			throws ReflectiveOperationException {
		ModelEditor modelEditor = mock(ModelEditor.class);
		when(modelEditor.getComponentImages()).thenReturn(registry);
		AbstractComponentEditor<Object> editor = new TestComponentEditor();
		editor.resourcePool = pool;
		Field field = AbstractComponentEditor.class.getDeclaredField("editor"); //$NON-NLS-1$
		field.setAccessible(true);
		field.set(editor, modelEditor);
		return editor;
	}

	private static class TestComponentEditor extends AbstractComponentEditor<Object> {
		@Override
		public String getLabel(Object element) {
			return null;
		}

		@Override
		public String getDetailLabel(Object element) {
			return null;
		}

		@Override
		public String getDescription(Object element) {
			return null;
		}

		@Override
		protected Composite doGetEditor(Composite parent, Object object) {
			return null;
		}

		@Override
		public IObservableList<?> getChildList(Object element) {
			return null;
		}
	}
}
