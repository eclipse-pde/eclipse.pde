/*******************************************************************************
 *  Copyright (c) 2006, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.Optional;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BasePackageHeader;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;

public class PackageHyperlink extends ManifestElementHyperlink {

	BasePackageHeader fHeader;

	public PackageHyperlink(IRegion region, String pack, BasePackageHeader header) {
		super(region, pack);
		fHeader = header;
	}

	@Override
	protected void open2() {
		Optional.ofNullable(fHeader.getBundle().getModel().getUnderlyingResource()).map(IResource::getProject)
				.map(p -> PDEJavaHelper.getPackageFragment(fElement, null, p))
				.ifPresent(EditorUtilities::showInPackageExplorer);
	}

}
