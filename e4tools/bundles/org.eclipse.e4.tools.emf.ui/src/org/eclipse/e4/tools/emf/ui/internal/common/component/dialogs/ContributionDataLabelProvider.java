/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs;

import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.ContributionData;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

/**
 * Shows a contribution as {@code class - bundle - source type}.
 */
public class ContributionDataLabelProvider extends StyledCellLabelProvider {
	private final Image classImage;

	/** The image is shown for contributions without an icon and is owned by the caller. */
	public ContributionDataLabelProvider(Image classImage) {
		this.classImage = classImage;
	}

	@Override
	public void update(ViewerCell cell) {
		ContributionData data;
		if (cell.getElement() instanceof ContributionData) {
			data = (ContributionData) cell.getElement();
		} else if (cell.getElement() instanceof ContributionDataFile) {
			data = ((ContributionDataFile) cell.getElement()).getContributionData();
		} else {
			return;
		}

		final StyledString styledString = new StyledString();
		if (data.className != null) {
			styledString.append(data.className, null);
		}

		if (data.bundleName != null) {
			styledString.append(" - " + data.bundleName, StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		} else if (data.installLocation != null) {
			styledString.append(" - " + data.installLocation, StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		}

		if (data.sourceType != null) {
			styledString.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			styledString.append(data.sourceType + "", StyledString.COUNTER_STYLER); //$NON-NLS-1$
		}

		if (data.iconPath == null) {
			cell.setImage(classImage);
		}

		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
}
