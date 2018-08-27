/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;

/**
 * Label provider for lists of plug-ins in the plug-in import wizard.  Uses images
 * from the PDELabelProvider, but uses a styled string for the text to colour the
 * versions in a different colour.
 *
 * @since 3.6
 */
public class PluginImportLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

	@Override
	public Image getImage(Object element) {
		return PDEPlugin.getDefault().getLabelProvider().getImage(element);
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public void update(ViewerCell cell) {
		StyledString string = getStyledText(cell.getElement());
		cell.setText(string.getString());
		cell.setStyleRanges(string.getStyleRanges());
		cell.setImage(getImage(cell.getElement()));
		super.update(cell);
	}

	private StyledString getStyledText(Object element) {
		StyledString styledString = new StyledString();
		if (element instanceof IPluginModelBase) {
			IPluginModelBase plugin = (IPluginModelBase) element;
			String symbolicName = plugin.getBundleDescription().getSymbolicName();
			Version version = plugin.getBundleDescription().getVersion();
			styledString.append(symbolicName);
			styledString.append(' ');
			styledString.append('(', StyledString.QUALIFIER_STYLER);
			styledString.append(version.toString(), StyledString.QUALIFIER_STYLER);
			styledString.append(')', StyledString.QUALIFIER_STYLER);
			return styledString;
		}
		styledString.append(element.toString());
		return styledString;
	}
}