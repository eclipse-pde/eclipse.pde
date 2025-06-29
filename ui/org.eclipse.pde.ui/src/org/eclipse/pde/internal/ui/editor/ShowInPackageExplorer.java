/*******************************************************************************
 * Copyright (c) 2025 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

public final class ShowInPackageExplorer implements Runnable {

	private final IPackageFragment fragment;

	public ShowInPackageExplorer(IPackageFragment fragment) {
		this.fragment = Objects.requireNonNull(fragment);
	}

	@Override
	public void run() {
		showPackageExplorer().map(p -> p.getAdapter(IShowInTarget.class))
				.ifPresent(show -> show.show(new ShowInContext(null, new StructuredSelection(fragment))));
	}

	private Optional<IViewPart> showPackageExplorer() {
		try {
			return Optional.of(PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES));
		} catch (PartInitException e) {
			return Optional.empty();
		}
	}

}
