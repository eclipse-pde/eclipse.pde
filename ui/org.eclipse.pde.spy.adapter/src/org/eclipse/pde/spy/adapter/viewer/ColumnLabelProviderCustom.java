/*******************************************************************************
 * Copyright (c)  Lacherp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lacherp - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.adapter.viewer;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.custom.StyleRange;

public class ColumnLabelProviderCustom extends ColumnLabelProvider {

	protected StyleRange[] getToolTipStyleRanges(Object element) {
		return null;
	}

}
