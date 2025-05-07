/*******************************************************************************
 * Copyright (c) 2012, 2021 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.bndtools.core.ui.resource;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Requirement;

public class RequirementLabelProvider extends StyledCellLabelProvider {

	protected final boolean shortenNamespaces;

	public RequirementLabelProvider() {
		this(true);
	}

	public RequirementLabelProvider(boolean shortenNamespaces) {
		this.shortenNamespaces = shortenNamespaces;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof Requirement) {
			Requirement requirement = (Requirement) element;

			StyledString label = getLabel(requirement);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());

			Image icon = Icons.image(R5LabelFormatter.getNamespaceImagePath(requirement.getNamespace()));
			cell.setImage(icon);
		}
	}

	protected StyledString getLabel(Requirement requirement) {
		StyledString label = new StyledString();
		return getLabel(label, requirement);
	}

	protected StyledString getLabel(StyledString label, Requirement requirement) {
		R5LabelFormatter.appendRequirementLabel(label, requirement, shortenNamespaces);
		return label;
	}
}
