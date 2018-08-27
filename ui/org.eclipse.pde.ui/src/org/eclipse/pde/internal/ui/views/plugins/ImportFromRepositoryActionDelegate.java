/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import org.eclipse.core.commands.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Action delegate to import a selected object if it represents a plug-in with a
 * Eclipse-SourceReferences header that can be processed by Team.
 *
 * @see ImportActionGroup
 */
public class ImportFromRepositoryActionDelegate extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//			enable = ImportActionGroup.canImport((IStructuredSelection) selection);

		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			ImportActionGroup.handleImport(PluginImportOperation.IMPORT_FROM_REPOSITORY, (IStructuredSelection) selection);
		}
		return null;
	}
}
