/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
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
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class DSFormOutlinePage extends FormOutlinePage  {

	public DSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		return new DSLabelProvider();
	}

	@Override
	protected String getParentPageId(Object item) {
		return DSOverviewPage.PAGE_ID;
	}
}
