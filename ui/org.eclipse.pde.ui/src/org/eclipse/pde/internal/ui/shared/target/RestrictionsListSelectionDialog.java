/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * Custom list selection dialog that is used to restrict a bundle container to a specific
 * list of bundle names.
 * 
 * @see BundleContainerTable
 * @see IBundleContainer
 */
public class RestrictionsListSelectionDialog extends ListSelectionDialog {

	private boolean fUseVersion;

	public RestrictionsListSelectionDialog(Shell parentShell, IResolvedBundle[] allBundles, BundleInfo[] restrictions) {
		super(parentShell, allBundles, new ArrayContentProvider(), new BundleInfoLabelProvider(), Messages.RestrictionsListSelectionDialog_0);
		setTitle(Messages.RestrictionsListSelectionDialog_1);
		if (restrictions == null) {
			// No restrictions means we should select everything
			setInitialSelections(allBundles);
		} else {
			// The resolved BundleInfos have name, version and location, while the restrictions only have name and possibly version
			// Find resolved bundles that match the restrictions
			fUseVersion = restrictions.length != 0 && restrictions[0].getVersion() != null;
			Set restrictionSet = new HashSet();
			for (int i = 0; i < restrictions.length; i++) {
				String restriction = restrictions[i].getSymbolicName();
				if (fUseVersion) {
					restriction = restriction.concat(restrictions[i].getVersion());
				}
				restrictionSet.add(restriction);
			}

			List initialSelect = new ArrayList();
			for (int i = 0; i < allBundles.length; i++) {
				String restriction = allBundles[i].getBundleInfo().getSymbolicName();
				if (fUseVersion) {
					restriction = restriction.concat(allBundles[i].getBundleInfo().getVersion());
				}
				if (restrictionSet.contains(restriction)) {
					initialSelect.add(allBundles[i]);
				}
			}
			setInitialElementSelections(initialSelect);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ListSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		Button versionButton = new Button((Composite) control, SWT.CHECK);
		versionButton.setText(Messages.RestrictionsListSelectionDialog_2);
		versionButton.setSelection(fUseVersion);
		versionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fUseVersion = ((Button) e.getSource()).getSelection();
			}
		});
		return control;
	}

	/**
	 * Whether the restrictions are intended to include the specific version or just to use the most recent version.
	 * @return whether the custom check box was selected
	 */
	public boolean isUseVersion() {
		return fUseVersion;
	}

}
