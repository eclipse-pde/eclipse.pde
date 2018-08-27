/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.commands;

import org.eclipse.core.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class QueryByTag extends QueryControl {

	private TagManager fTagManager;
	private Combo fTagsCombo;
	private TagManager.Listener fTagManagerListener;

	public QueryByTag(CommandComposerPart csp, Composite comp) {
		super(csp, comp);
	}

	@Override
	protected void createGroupContents(Group parent) {
		fTagManager = fCSP.getTagManager();

		fTagsCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		fTagsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fToolkit.adapt(fTagsCombo, true, true);

		fTagManagerListener = new TagManagerListener();
		fCSP.getTagManager().addListener(fTagManagerListener);

		parent.addDisposeListener(e -> {
			if (fTagManagerListener != null) {
				fTagManager.removeListener(fTagManagerListener);
			}
		});
	}

	@Override
	protected String getName() {
		return "Query Commands by Tags"; //$NON-NLS-1$
	}

	private void refreshTags() {
		fTagsCombo.removeAll();
		String[] tags = fTagManager.getTags();
		for (String tag : tags) {
			fTagsCombo.add(tag);
		}
	}

	private class TagManagerListener implements TagManager.Listener {
		@Override
		public void tagManagerChanged() {
			refreshTags();
		}
	}

	@Override
	protected Command[] getCommands() {
		String tagText = fTagsCombo.getText();
		return fCSP.getTagManager().getCommands(tagText);
	}

	@Override
	protected void enable(boolean enable) {
		fGroup.setEnabled(enable);
		fTagsCombo.setEnabled(enable);
	}
}
