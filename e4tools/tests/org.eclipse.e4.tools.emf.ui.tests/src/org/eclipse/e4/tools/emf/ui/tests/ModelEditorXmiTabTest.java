/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.internal.services.TranslationObjectSupplier;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.tools.emf.ui.common.IModelResource;
import org.eclipse.e4.tools.emf.ui.common.XMIModelResource;
import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.XmiTab;
import org.eclipse.e4.tools.emf.ui.internal.wbm.ApplicationModelEditor;
import org.eclipse.e4.tools.services.IResourcePool;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class ModelEditorXmiTabTest {

	@TempDir
	Path tempDir;

	private Shell shell;
	private BundleContext bundleContext;
	private IEclipseContext context;
	private ApplicationModelEditor editor;
	private CTabFolder folder;
	private int xmiTabIndex;
	private int otherTabIndex;

	@BeforeEach
	public void setUp() throws IOException, InvalidSyntaxException {
		File file = tempDir.resolve("Application.e4xmi").toFile(); //$NON-NLS-1$
		Resource resource = new XMIResourceImpl(URI.createFileURI(file.getAbsolutePath()));
		MApplication application = MApplicationFactory.INSTANCE.createApplication();
		resource.getContents().add((EObject) application);
		resource.save(null);
		IModelResource model = new XMIModelResource(URI.createFileURI(file.getAbsolutePath()));

		shell = new Shell(Display.getDefault());
		Composite parent = new Composite(shell, SWT.NONE);

		bundleContext = FrameworkUtil.getBundle(ApplicationModelEditor.class).getBundleContext();
		context = EclipseContextFactory.getServiceContext(bundleContext).createChild();
		application.setContext(context);
		context.set(MApplication.class, application);
		context.set(Composite.class, parent);
		context.set(Shell.class, shell);
		context.set(IModelResource.class, model);
		context.set(IResourcePool.class, mock(IResourcePool.class));
		context.set(ESelectionService.class, mock(ESelectionService.class));
		context.set(EPartService.class, mock(EPartService.class));
		context.set(UISynchronize.class, mock(UISynchronize.class));
		context.set(Logger.class, mock(Logger.class));
		context.set(ECommandService.class, mock(ECommandService.class));
		context.set(EHandlerService.class, mock(EHandlerService.class));
		injectTranslationSupplier(bundleContext, context); // inject @Translation fields

		Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), () -> {
			editor = ContextInjectionFactory.make(ApplicationModelEditor.class, context);
		});

		folder = findTabFolder(parent);
		xmiTabIndex = findXMITabIndex(folder);
		assertTrue(xmiTabIndex != -1, "could not find xmi tab");
		otherTabIndex = xmiTabIndex == 0 ? 1 : xmiTabIndex - 1;
	}

	@AfterEach
	public void tearDown() throws InvalidSyntaxException {
		// dispose the widgets first, so that the editor's @PreDestroy doesn't dispose
		// observables that the still-alive tree viewer reads from
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
		if (context != null) {
			context.dispose();
		}
		// rebind translation supplier to the service context so it doesn't keep
		// referencing the disposed test context
		injectTranslationSupplier(bundleContext, EclipseContextFactory.getServiceContext(bundleContext));
	}

	@Test
	public void testFindReplaceTargetAndTextViewerAreOnlyAvailableOnXmiTab() {
		selectTab(otherTabIndex);
		assertNull(editor.getAdapter(IFindReplaceTarget.class));
		assertNull(editor.getAdapter(ITextViewer.class));

		selectTab(xmiTabIndex);
		IFindReplaceTarget findReplaceTarget = editor.getAdapter(IFindReplaceTarget.class);
		assertNotNull(findReplaceTarget);
		assertTrue(findReplaceTarget.canPerformFind());
		assertNotNull(editor.getAdapter(ITextViewer.class));
	}

	private static void injectTranslationSupplier(BundleContext bundleContext, IEclipseContext ctx)
			throws InvalidSyntaxException {
		ServiceReference<ExtendedObjectSupplier> reference = null;
		try {
			String filter = "(dependency.injection.annotation=" + Translation.class.getName() + ")";
			Collection<ServiceReference<ExtendedObjectSupplier>> references = bundleContext
					.getServiceReferences(ExtendedObjectSupplier.class, filter);
			assertTrue(!references.isEmpty(), "no translation supplier registered");
			reference = references.iterator().next();
			ExtendedObjectSupplier supplier = bundleContext.getService(reference);
			assertTrue(supplier instanceof TranslationObjectSupplier, "unexpected translation supplier");
			ContextInjectionFactory.inject(supplier, ctx);
		} finally {
			if (reference != null) {
				bundleContext.ungetService(reference);
			}
		}
	}

	private void selectTab(int index) {
		folder.setSelection(index);
		folder.notifyListeners(SWT.Selection, new Event());
	}

	private static CTabFolder findTabFolder(Composite parent) {
		for (Control child : parent.getChildren()) {
			if (child instanceof CTabFolder tabFolder) {
				return tabFolder;
			}
			if (child instanceof Composite composite) {
				final CTabFolder found = findTabFolder(composite);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static int findXMITabIndex(CTabFolder folder) {
		for (int i = 0; i < folder.getItemCount(); i++) {
			CTabItem item = folder.getItem(i);
			if (item.getControl() instanceof XmiTab) {
				return i;
			}
		}
		return -1;
	}
}
