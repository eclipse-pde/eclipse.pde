/*******************************************************************************
 * Copyright (c) 2010, 2021 bndtools project and others.
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
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *******************************************************************************/
package bndtools.shared;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

public class URLLabelProvider extends StyledCellLabelProvider {

	private final static Image	linkImg	= Icons.image("link");
	private final static Image	fileImg	= Icons.image("file");

	public URLLabelProvider(Device display) {}

	@Override
	public void update(ViewerCell cell) {
		Image img;
		String text;

		Object element = cell.getElement();
		if (element instanceof OBRLink) {
			StyledString label = ((OBRLink) element).getLabel();
			cell.setStyleRanges(label.getStyleRanges());
			text = label.getString();
		} else {
			text = (element == null ? "null" : element.toString());
		}

		if (text.startsWith("file:"))
			img = fileImg;
		else
			img = linkImg;

		cell.setText(text);
		cell.setImage(img);
	}
}
