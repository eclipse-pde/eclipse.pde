/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

public class RepositoriesDialog extends TrayDialog {

	private TargetReposGroup fRepoGroup;
	private ITargetDefinition fTarget;

	/**
	 * Create a dialog to host the given input.
	 * @param shell a shell
	 * @param target the target that this dialog will display repositories for, the target will not be modified
	 */
	public RepositoriesDialog(Shell shell, ITargetDefinition target) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		fTarget = target;
	}

	public URI[] getRepositories() {
		if (fRepoGroup == null) {
			return fTarget.getRepositories();
		}
		return fRepoGroup.getRepos();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		fRepoGroup = TargetReposGroup.createInDialog(comp);
		fRepoGroup.setInput(fTarget);
		applyDialogFont(parent);
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.ResizableDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.RepositoriesDialog_TargetRepositoriesTitle);
		// TODO Add help context
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, getHelpContextId());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
	 */
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

}
