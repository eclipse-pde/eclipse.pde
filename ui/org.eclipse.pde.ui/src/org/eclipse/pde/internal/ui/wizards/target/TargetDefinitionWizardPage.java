/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetDefinitionManager;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.target.TargetModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.target.OpenTargetProfileAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class TargetDefinitionWizardPage extends WizardNewFileCreationPage {
	
	protected static final int USE_DEFAULT = 0;
	protected static final int USE_CURRENT_TP = 1;
	protected static final int USE_EXISTING_TARGET = 2;
	
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Button fExistingTargetButton;
	private Combo fTargets;
	private String[] fTargetIds;
	private Button fPreviewButton;
	
	private static String EXTENSION = ".target"; //$NON-NLS-1$
	
	public TargetDefinitionWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		setFileName(EXTENSION); 
	}
	
    protected void createAdvancedControls(Composite parent) {
    	Group group = new Group(parent, SWT.NONE);
    	group.setText(PDEUIMessages.TargetProfileWizardPage_groupTitle);
    	group.setLayout(new GridLayout(3, false));
    	group.setLayoutData(new GridData(GridData.FILL_BOTH));
    	
    	fDefaultButton = new Button(group, SWT.RADIO);
    	fDefaultButton.setText(PDEUIMessages.TargetProfileWizardPage_blankTarget);
    	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = 3;
    	fDefaultButton.setLayoutData(gd);
    	fDefaultButton.setSelection(true);
    	
    	fCurrentTPButton = new Button(group, SWT.RADIO);
    	fCurrentTPButton.setText(PDEUIMessages.TargetProfileWizardPage_currentPlatform);
    	gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = 3;
    	fCurrentTPButton.setLayoutData(gd);
    	
    	fExistingTargetButton = new Button(group, SWT.RADIO);
    	fExistingTargetButton.setText(PDEUIMessages.TargetProfileWizardPage_existingTarget);
    	fExistingTargetButton.setLayoutData(new GridData());
    	fExistingTargetButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			boolean enabled = fExistingTargetButton.getSelection();
    			fTargets.setEnabled(enabled);
    			fPreviewButton.setEnabled(enabled);
    		}
    	});
    	
    	fTargets = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
    	fTargets.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	fTargets.setEnabled(false);
    	initializeTargetCombo();
    	
    	fPreviewButton = new Button(group, SWT.PUSH);
    	fPreviewButton.setText(PDEUIMessages.TargetProfileWizardPage_viewProfile);
    	fPreviewButton.setLayoutData(new GridData());
    	SWTUtil.setButtonDimensionHint(fPreviewButton);
    	fPreviewButton.setEnabled(false);
    	fPreviewButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
				InputStream stream = null;
				try {
					URL url = getExternalTargetURL();
					if (url != null)
						stream = url.openStream();
 					if (stream != null) {
						ITargetModel model = new TargetModel();
						model.load(stream, false);
						new OpenTargetProfileAction(getShell(), model).run();
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
    	
    	Dialog.applyDialogFont(group);
    }
    
	private URL getExternalTargetURL() {
		TargetDefinitionManager manager = PDECore.getDefault().getTargetProfileManager();
		IConfigurationElement elem = manager.getTarget(fTargetIds[fTargets.getSelectionIndex()]);
		if (elem != null) {
			String path = elem.getAttribute("definition");  //$NON-NLS-1$
			String symbolicName = elem.getDeclaringExtension().getNamespaceIdentifier();
			return TargetDefinitionManager.getResourceURL(symbolicName, path);
		}
		return null;
	}

    
    protected void initializeTargetCombo() {
    	IConfigurationElement[] elements = PDECore.getDefault().getTargetProfileManager().getSortedTargets();
    	fTargetIds = new String[elements.length];
    	for (int i = 0; i < elements.length; i++) {
    		String name =elements[i].getAttribute("name"); //$NON-NLS-1$
			if (fTargets.indexOf(name) == -1)
				fTargets.add(name);
			fTargetIds[i] = elements[i].getAttribute("id"); //$NON-NLS-1$
    	}
    	if (elements.length > 0)
    		fTargets.select(0);
    }
    
    protected boolean validatePage() {
		if (!getFileName().trim().endsWith(EXTENSION)) { 
			setErrorMessage(PDEUIMessages.TargetProfileWizardPage_error); 
			return false;
		}
		if (getFileName().trim().length() <= EXTENSION.length()) {
			return false;
		}
		return super.validatePage();
    }
    
    protected void createLinkTarget() {
    }
        
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	protected int getInitializationOption() {
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
