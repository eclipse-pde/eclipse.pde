/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     Sean Bright <sean.bright@gmail.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.templating;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.pde.bnd.ui.BoldStyler;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

public class RepoTemplateLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

	private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		Category cat = Adapters.adapt(element, Category.class);
		if (cat != null) {
			cell.setText(cat.getName());
			cell.setImage(IMG_FOLDER);
			cell.setStyleRanges(new StyleRange[0]);
			return;
		}
		Template template = Adapters.adapt(element, Template.class);
		if (template != null) {
			// Name
			StyledString label = new StyledString(template.getName(), BoldStyler.INSTANCE_DEFAULT);

			// Version, with all segments except qualifier in bold
			Version version = template.getVersion();
			if (version != null) {
				label.append(" ");
				label.append(String.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro()),
						BoldStyler.INSTANCE_COUNTER);
				String q = version.getQualifier();
				if (q != null && !q.isEmpty()) {
					label.append("." + q, StyledString.COUNTER_STYLER);
				}
			}

			String description = template.getShortDescription();
			if (description != null) {
				label.append(" \u2014 [", StyledString.QUALIFIER_STYLER)
						.append(template.getShortDescription(), StyledString.QUALIFIER_STYLER)
						.append("]", StyledString.QUALIFIER_STYLER);
			}

			cell.setText(label.toString());
			cell.setStyleRanges(label.getStyleRanges());

			Image image = Adapters.adapt(template, Image.class);
			if (image == null) {
				cell.setImage(Resources.getImage("/icons/template.gif"));
			} else {
				cell.setImage(image);
			}
			return;
		}
		cell.setStyleRanges(new StyleRange[0]);
		cell.setText(getText(element));
		cell.setImage(getImage(element));
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return null;
	}

}