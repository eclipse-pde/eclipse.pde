/*******************************************************************************
 * Copyright (c) 2010, 2015 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 * Lars Vogel <Lars.Vogel@gmail.com> - Bug 421453
 ******************************************************************************/
package org.eclipse.e4.tools.compat.internal;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.ILoggerProvider;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.tools.services.IClipboardService;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.workbench.swt.DisplayUISynchronize;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.services.IWorkbenchLocationService;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.css.CSSStyleDeclaration;

@SuppressWarnings("restriction")
public class ContextServiceFactory extends AbstractServiceFactory {

	@Override
	public Object create(Class serviceInterface, IServiceLocator parentLocator,
			IServiceLocator locator) {
		if (!IEclipseContext.class.equals(serviceInterface)) {
			return null;
		}

		final IWorkbenchLocationService wls = locator.getService(IWorkbenchLocationService.class);
		final IWorkbenchWindow window = wls.getWorkbenchWindow();
		final IWorkbenchPartSite site = wls.getPartSite();

		@SuppressWarnings("unchecked")
		final Object o = parentLocator.getService(serviceInterface);

		// This happens when we run in plain 3.x
		// We need to create a parent service context
		if (window == null && site == null) {
			final Bundle bundle = FrameworkUtil.getBundle(ContextServiceFactory.class);
			final BundleContext bundleContext = bundle.getBundleContext();
			final IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundleContext);

			final IEclipseContext appContext = serviceContext.createChild("WorkbenchContext"); //$NON-NLS-1$
			appContext.set(Display.class, Display.getCurrent());
			appContext.set(Logger.class, new WorkbenchLogger());
			appContext.set(IClipboardService.class, new ClipboardServiceImpl());
			appContext.set(Realm.class, Realm.getDefault());

			final Display display = Display.getCurrent();
			appContext.set(UISynchronize.class, new DisplayUISynchronize(display));

			final IThemeManager manager = serviceContext.get(IThemeManager.class);
			final IThemeEngine engine = manager.getEngineForDisplay(Display.getCurrent());
			appContext.set(IThemeEngine.class, engine);

			appContext.set(IStylingEngine.class, new IStylingEngine() {

				@Override
				public void setClassname(Object widget, String classname) {
					((Widget) widget).setData("org.eclipse.e4.ui.css.CssClassName", classname); //$NON-NLS-1$
					engine.applyStyles(widget, true);
				}

				@Override
				public void setId(Object widget, String id) {
					((Widget) widget).setData("org.eclipse.e4.ui.css.id", id); //$NON-NLS-1$
					engine.applyStyles(widget, true);
				}

				@Override
				public void style(Object widget) {
					engine.applyStyles(widget, true);
				}

				@Override
				public CSSStyleDeclaration getStyle(Object widget) {
					return engine.getStyle(widget);
				}

				@Override
				public void setClassnameAndId(Object widget, String classname,
						String id) {
					((Widget) widget).setData("org.eclipse.e4.ui.css.CssClassName", classname); //$NON-NLS-1$
					((Widget) widget).setData("org.eclipse.e4.ui.css.id", id); //$NON-NLS-1$
					engine.applyStyles(widget, true);
				}
			});

			if (appContext.get(ILoggerProvider.class) == null) {
				appContext.set(ILoggerProvider.class,
						ContextInjectionFactory.make(DefaultLoggerProvider.class, appContext));
			}

			return appContext;
		} else if (o != null && site == null) {
			final IEclipseContext windowContext = ((IEclipseContext) o).createChild("WindowContext(" + window + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			windowContext.set(ISelectionService.class, window.getSelectionService());

			windowContext.declareModifiable(IServiceConstants.ACTIVE_SELECTION);
			window.getSelectionService().addSelectionListener((part, selection) -> {
				if (!selection.isEmpty()) {
					if (selection instanceof final IStructuredSelection s) {
						if (s.size() == 1) {
							windowContext.set(IServiceConstants.ACTIVE_SELECTION, s.getFirstElement());
						} else {
							windowContext.set(IServiceConstants.ACTIVE_SELECTION, s.toList());
						}
					} else {
						windowContext.set(IServiceConstants.ACTIVE_SELECTION, selection);
					}
				}
			});
			return windowContext;
		}

		return o;
	}
}