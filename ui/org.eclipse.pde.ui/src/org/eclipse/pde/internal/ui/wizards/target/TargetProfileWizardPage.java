package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
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

public class TargetProfileWizardPage extends WizardNewFileCreationPage {
	
	protected static final int USE_DEFAULT = 0;
	protected static final int USE_CURRENT_TP = 1;
	protected static final int USE_EXISTING_TARGET = 2;
	
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Button fExistingTargetButton;
	private Combo fTargets;
	private String[] fTargetIds;
	
	private static String EXTENSION = ".target"; //$NON-NLS-1$
	
	public TargetProfileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		setFileName(EXTENSION); 
	}
	
    protected void createAdvancedControls(Composite parent) {
    	Group initializeGroup = new Group(parent, SWT.NONE);
    	initializeGroup.setText(PDEUIMessages.TargetProfileWizardPage_groupTitle);
    	initializeGroup.setLayout(new GridLayout(2, false));
    	initializeGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    	
    	fDefaultButton = new Button(initializeGroup, SWT.RADIO);
    	fDefaultButton.setText(PDEUIMessages.TargetProfileWizardPage_blankTarget);
    	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = 2;
    	fDefaultButton.setLayoutData(gd);
    	fDefaultButton.setSelection(true);
    	
    	fCurrentTPButton = new Button(initializeGroup, SWT.RADIO);
    	fCurrentTPButton.setText(PDEUIMessages.TargetProfileWizardPage_currentPlatform);
    	gd = new GridData(GridData.FILL_HORIZONTAL);
    	gd.horizontalSpan = 2;
    	fCurrentTPButton.setLayoutData(gd);
    	
    	fExistingTargetButton = new Button(initializeGroup, SWT.RADIO);
    	fExistingTargetButton.setText(PDEUIMessages.TargetProfileWizardPage_existingTarget);
    	fExistingTargetButton.setLayoutData(new GridData());
    	fExistingTargetButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			fTargets.setEnabled(fExistingTargetButton.getSelection());
    		}
    	});
    	
    	fTargets = new Combo(initializeGroup, SWT.SINGLE|SWT.READ_ONLY);
    	fTargets.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	fTargets.setEnabled(false);
    	initializeTargetCombo();
    	Dialog.applyDialogFont(initializeGroup);
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
