/*******************************************************************************
 *  Copyright (c) 2004, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * IConentOutlinePage with externally enabled/disabled element sorting
 */
public interface ISortableContentOutlinePage extends IContentOutlinePage {
	/**
	 * Turns sorting on or off
	 * @param sorting - boolean value indicating if sorting should be enabled
	 */
	public void sort(boolean sorting);

}
