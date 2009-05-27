/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import org.eclipse.core.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class QueryByTag extends QueryControl {

	private TagManager fTagManager;
	private Combo fTagsCombo;
	private TagManager.Listener fTagManagerListener;

	public QueryByTag(CommandComposerPart csp, Composite comp) {
		super(csp, comp);
	}

	protected void createGroupContents(Group parent) {
		fTagManager = fCSP.getTagManager();

		fTagsCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		fTagsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fToolkit.adapt(fTagsCombo, true, true);

		fTagManagerListener = new TagManagerListener();
		fCSP.getTagManager().addListener(fTagManagerListener);

		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (fTagManagerListener != null) {
					fTagManager.removeListener(fTagManagerListener);
				}
			}
		});
	}

	protected String getName() {
		return "Query Commands by Tags"; //$NON-NLS-1$
	}

	private void refreshTags() {
		fTagsCombo.removeAll();
		String[] tags = fTagManager.getTags();
		for (int i = 0; i < tags.length; i++) {
			fTagsCombo.add(tags[i]);
		}
	}

	private class TagManagerListener implements TagManager.Listener {
		public void tagManagerChanged() {
			refreshTags();
		}
	}

	protected Command[] getCommands() {
		String tagText = fTagsCombo.getText();
		return fCSP.getTagManager().getCommands(tagText);
	}

	protected void enable(boolean enable) {
		fGroup.setEnabled(enable);
		fTagsCombo.setEnabled(enable);
	}
}
