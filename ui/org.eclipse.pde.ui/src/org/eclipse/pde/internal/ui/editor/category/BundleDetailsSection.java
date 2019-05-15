/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;

public class BundleDetailsSection extends IUDetailsSection<ISiteBundle> {

	public BundleDetailsSection(PDEFormPage page, Composite parent) {
		super(page, parent, PDEUIMessages.BundleDetails_title, PDEUIMessages.BundleDetails_sectionDescription,
				BundleDetailsSection::extractFromSelection);
	}

	private static ISiteBundle extractFromSelection(Object o) {
		if (o instanceof ISiteBundle) {
			return (ISiteBundle) o;
		} else if (o instanceof SiteBundleAdapter) {
			return ((SiteBundleAdapter) o).bundle;
		}
		return null;
	}
}
