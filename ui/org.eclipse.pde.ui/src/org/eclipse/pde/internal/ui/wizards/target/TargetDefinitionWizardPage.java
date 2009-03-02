/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetDefinitionManager;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.target.TargetModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.target.OpenTargetProfileAction;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class TargetDefinitionWizardPage extends PDEWizardNewFileCreationPage {

	protected static final int USE_EMPTY = 0;
	protected static final int USE_DEFAULT = 1;
	protected static final int USE_CURRENT_TP = 2;
	protected static final int USE_EXISTING_TARGET = 3;

	private Button fEmptyButton;
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Button fExistingTargetButton;
	private Combo fTargets;
	private String[] fTargetIds;
	private Button fPreviewButton;

	private static String EXTENSION = "target"; //$NON-NLS-1$

	public TargetDefinitionWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
		// Force the file extension to be 'target'
		setFileExtension(EXTENSION);
	}

	public void createControl(Composite parent) {
		super.createControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_DEFINITION_PAGE);
	}

	protected void createAdvancedControls(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 3, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp, PDEUIMessages.TargetCreationPage_0, 3);

		fEmptyButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_1, 3);
		fDefaultButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_2, 3);
		fCurrentTPButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_3, 3);
		fExistingTargetButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_4, 1);
		fExistingTargetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fExistingTargetButton.getSelection();
				fTargets.setEnabled(enabled);
				fPreviewButton.setEnabled(enabled);
			}
		});

		fEmptyButton.setSelection(true);

		fTargets = SWTFactory.createCombo(comp, SWT.SINGLE | SWT.READ_ONLY, 1, null);
		fTargets.setEnabled(false);
		initializeTargetCombo();

		fPreviewButton = SWTFactory.createPushButton(comp, PDEUIMessages.TargetCreationPage_5, null);
		fPreviewButton.setEnabled(false);
		fPreviewButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputStream stream = null;
				try {
					URL url = getExternalTargetURL();
					if (url != null)
						stream = new BufferedInputStream(url.openStream());
					if (stream != null) {
						ITargetModel model = new TargetModel();
						model.load(stream, false);
						new OpenTargetProfileAction(getShell(), model, fTargets.getText()).run();
					}
				} catch (IOException e1) {
				} catch (CoreException e2) {
				} finally {
					try {
						if (stream != null)
							stream.close();
					} catch (IOException e3) {
					}
				}
			}
		});

		Dialog.applyDialogFont(comp);
		setControl(comp);

	}

	private URL getExternalTargetURL() {
		TargetDefinitionManager manager = PDECore.getDefault().getTargetProfileManager();
		IConfigurationElement elem = manager.getTarget(fTargetIds[fTargets.getSelectionIndex()]);
		if (elem != null) {
			String path = elem.getAttribute("definition"); //$NON-NLS-1$
			String symbolicName = elem.getDeclaringExtension().getNamespaceIdentifier();
			return TargetDefinitionManager.getResourceURL(symbolicName, path);
		}
		return null;
	}

	protected void initializeTargetCombo() {
		IConfigurationElement[] elements = PDECore.getDefault().getTargetProfileManager().getSortedTargets();
		fTargetIds = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			String name = elements[i].getAttribute("name"); //$NON-NLS-1$
			if (fTargets.indexOf(name) == -1)
				fTargets.add(name);
			fTargetIds[i] = elements[i].getAttribute("id"); //$NON-NLS-1$
		}
		if (elements.length > 0)
			fTargets.select(0);
	}

	protected int getInitializationOption() {
		if (fEmptyButton.getSelection())
			return USE_EMPTY;
		if (fDefaultButton.getSelection())
			return USE_DEFAULT;
		else if (fCurrentTPButton.getSelection())
			return USE_CURRENT_TP;
		return USE_EXISTING_TARGET;
	}

	protected String getTargetId() {
		return fTargetIds[fTargets.getSelectionIndex()];
	}

}
