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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class QueryControl {

	protected final CommandComposerPart fCSP;
	protected final FormToolkit fToolkit;
	protected Button fRadioButton;
	protected Group fGroup;

	protected QueryControl(CommandComposerPart csp, Composite parent) {
		fCSP = csp;
		fToolkit = csp.getToolkit();
		createGroup(parent);
	}

	protected ICommandService getCommandService() {
		return fCSP.getCommandService();
	}

	private Group createGroup(Composite parent) {
		fRadioButton = fToolkit.createButton(parent, "", SWT.RADIO); //$NON-NLS-1$
		fRadioButton.addSelectionListener(widgetSelectedAdapter(e -> enable(fRadioButton.getSelection())));
		fGroup = new Group(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fGroup.setLayoutData(gd);
		fGroup.setLayout(new GridLayout());
		fGroup.setText(getName());
		createGroupContents(fGroup);
		fToolkit.adapt(fGroup, false, false);
		return fGroup;
	}

	protected QueryControl select(boolean select) {
		fRadioButton.setSelection(select);
		return this;
	}

	protected abstract void createGroupContents(Group parent);

	protected abstract String getName();

	protected abstract void enable(boolean enable);

	protected abstract Command[] getCommands();

}
