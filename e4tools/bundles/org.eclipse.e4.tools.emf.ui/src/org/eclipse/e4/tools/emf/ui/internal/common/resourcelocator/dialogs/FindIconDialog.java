/*******************************************************************************
 * Copyright (c) 2014 TwelveTone LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Steven Spungin <steven@spungin.tv> - initial API and implementation, Bug 424730
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.common.ResourceSearchScope;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.AbstractIconDialogWithScopeAndFilter;
import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.empty.E;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages;
import org.eclipse.swt.widgets.Shell;

final class FindIconDialog extends AbstractIconDialogWithScopeAndFilter {
	private final String bundle;

	FindIconDialog(Shell parentShell, IEclipseContext context) {
		this(parentShell, context, null);
	}

	/** Searches the workspace for the icons of the given bundle, or of any bundle if it is empty. */
	FindIconDialog(Shell parentShell, IEclipseContext context, String bundle) {
		super(parentShell, context);
		this.bundle = bundle;
		if (E.notEmpty(bundle)) {
			setScope(ResourceSearchScope.WORKSPACE);
			setSearchScopes(EnumSet.of(ResourceSearchScope.WORKSPACE));
			final List<String> bundles = new ArrayList<>();
			bundles.add(bundle);
			setFilterBundles(bundles);
		}
	}

	@Override
	protected boolean isScopePersisted() {
		return E.isEmpty(bundle);
	}

	@Override
	protected IFile checkResourceAccessible(IFile file, String installLocation) {
		// an icon of the bundle the icon link was opened for needs no reference check
		boolean locked = E.notEmpty(bundle) && getFilterBundles() != null && getFilterBundles().contains(bundle)
				&& getSearchScopes().equals(EnumSet.of(ResourceSearchScope.WORKSPACE));
		return locked ? file : super.checkResourceAccessible(file, installLocation);
	}

	@Override
	protected String getShellTitle() {
		return Messages.FindIconDialog_findIcon;
	}

	@Override
	protected String getDialogTitle() {
		return Messages.FindIconDialog_findIcon;
	}

	@Override
	protected String getDialogMessage() {
		if (E.notEmpty(bundle)) {
			return Messages.FindIconDialog_filteringByBundle + " " + bundle; //$NON-NLS-1$
		}
		return Messages.FindIconDialog_searchByFilename;
	}
}
