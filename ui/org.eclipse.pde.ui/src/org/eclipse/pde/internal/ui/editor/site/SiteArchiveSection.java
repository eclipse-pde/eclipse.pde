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
package org.eclipse.pde.internal.ui.editor.site;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class SiteArchiveSection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.SiteArchiveSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.SiteArchiveSection.desc";
	private static final String KEY_NEW = "SiteEditor.SiteArchiveSection.new";

	public SiteArchiveSection(ArchivePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] { PDEPlugin.getResourceString(KEY_NEW)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite) {
			return ((ISite) parent).getArchives();
		}
		return new Object[0];
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
		}
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteArchive;
	}

	protected String getOpenPopupLabel() {
		return null;
	}

	protected boolean isOpenable() {
		return false;
	}

	protected void handleNew() {
		final ISiteModel model = (ISiteModel) getFormPage().getModel();
		BusyIndicator
			.showWhile(tableViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				NewArchiveDialog dialog =
					new NewArchiveDialog(
						tableViewer.getControl().getShell(),
						model);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, -1);
				dialog.open();
			}
		});
	}

	protected void remove(Object input, List objects) throws CoreException {
		ISiteArchive[] array =
			(ISiteArchive[]) objects.toArray(new ISiteArchive[objects.size()]);
		ISite site = (ISite) input;
		site.removeArchives(array);
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
	}

	protected boolean isValidObject(Object obj) {
		return obj instanceof ISiteArchive;
	}

	protected void accept(ISite site, ArrayList archives)
		throws CoreException {
		site.addArchives(
			(ISiteArchive[]) archives.toArray(
				new ISiteArchive[archives.size()]));
	}
}
