/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.search.ui.IActionGroupFactory;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.ui.actions.ActionGroup;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchActionGroupFactory implements IActionGroupFactory {

	/**
	 * @see org.eclipse.search.ui.IActionGroupFactory#createActionGroup(org.eclipse.search.ui.ISearchResultView)
	 */
	public ActionGroup createActionGroup(ISearchResultView searchView) {
		return new PluginSearchActionGroup();
	}

}
