/*******************************************************************************
 *  Copyright (c) 2012, 2013 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - bugs fixing
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public interface IFilter {

	boolean accept(ImageElement element);
}
