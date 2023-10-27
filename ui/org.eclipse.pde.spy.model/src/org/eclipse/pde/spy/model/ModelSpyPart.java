/*******************************************************************************
 * Copyright (c) 2011 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.spy.model;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.common.IModelResource;
import org.eclipse.e4.tools.emf.ui.internal.wbm.ApplicationModelEditor;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.MApplication;

import jakarta.inject.Inject;

@SuppressWarnings("restriction")
public class ModelSpyPart {

	private final ApplicationModelEditor instance;
	private final IEclipseContext childContext;

	@Inject
	public ModelSpyPart(IEclipseContext context, MApplication application) {
		childContext = context.createChild("EditorContext");
		MemoryModelResource resource = new MemoryModelResource(application);
		childContext.set(IModelResource.class, resource);

		instance = ContextInjectionFactory.make(ApplicationModelEditor.class, childContext);
	}

	@Focus
	void focus() {
		ContextInjectionFactory.invoke(instance, Focus.class, childContext);
	}
}