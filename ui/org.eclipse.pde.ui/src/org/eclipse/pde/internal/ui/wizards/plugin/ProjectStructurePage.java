package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.StringTokenizer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author melhem
 *
 */
public class ProjectStructurePage extends WizardPage {
	
	private Button fSimpleButton;
	private Button fJavaButton;
	private Label fLibraryLabel;
	private Text fLibraryText;
	private Label fSourceLabel;
	private Text fSourceText;
	private Label fOutputlabel;
	private Text fOutputText;
	private boolean fIsFragment;
	private Button fBundleCheck;
	private IProjectProvider fProjectProvider;
	private boolean fFirstVisible = true;
	private AbstractFieldData fData;

	public ProjectStructurePage(String pageName, IProjectProvider provider, AbstractFieldData data, boolean isFragment) {
		super(pageName);
		if (isFragment) {
			setTitle(PDEPlugin.getResourceString("ProjectStructurePage.ftitle")); //$NON-NLS-1$
		} else {
			setTitle(PDEPlugin.getResourceString("ProjectStructurePage.title")); //$NON-NLS-1$
		}
		setDescription(PDEPlugin.getResourceString("ProjectStructurePage.desc")); //$NON-NLS-1$
		fIsFragment = isFragment;
		fProjectProvider = provider;
		fData = data;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 20;
		container.setLayout(layout);		
		createProjectTypeGroup(container);
		createBundleStructureGroup(container);
		Dialog.applyDialogFont(container);
		setControl(container);
	}
	
	private void createProjectTypeGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProjectStructurePage.settings")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fSimpleButton = createButton(group);
		fSimpleButton.setText(PDEPlugin.getResourceString("ProjectStructurePage.simple")); //$NON-NLS-1$
		fSimpleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = !fSimpleButton.getSelection();
				fLibraryLabel.setEnabled(enabled);
				fLibraryText.setEnabled(enabled);
				fSourceLabel.setEnabled(enabled);
				fSourceText.setEnabled(enabled);
				fOutputlabel.setEnabled(enabled);
				fOutputText.setEnabled(enabled);
				validatePage();
			}
		});
		
		fJavaButton = createButton(group);
		fJavaButton.setText(PDEPlugin.getResourceString("ProjectStructurePage.java")); //$NON-NLS-1$
		fJavaButton.setSelection(true);
		
		fLibraryLabel = createLabel(group, PDEPlugin.getResourceString("ProjectStructurePage.library")); //$NON-NLS-1$
		fLibraryText = createText(group);
		
		fSourceLabel = createLabel(group, PDEPlugin.getResourceString("ProjectStructurePage.source")); //$NON-NLS-1$
		fSourceText = createText(group);
		fSourceText.setText("src"); //$NON-NLS-1$
		
		fOutputlabel = createLabel(group, PDEPlugin.getResourceString("ProjectStructurePage.output")); //$NON-NLS-1$
		fOutputText = createText(group);
		fOutputText.setText("bin");		 //$NON-NLS-1$
	}
	
	private Label createLabel(Composite container, String text) {
		Label label = new Label(container, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 30;
		label.setLayoutData(gd);
		return label;
	}
	
	private Text createText(Composite container) {
		Text text = new Text(container, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		return text;
	}
	
	private Button createButton(Composite container) {
		Button button = new Button(container, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		button.setLayoutData(gd);
		return button;		
	}
	
	private void createBundleStructureGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProjectStructurePage.alternateFormat")); //$NON-NLS-1$
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBundleCheck = new Button(group, SWT.CHECK);
		if (fIsFragment)
			fBundleCheck.setText(PDEPlugin.getResourceString("ProjectStructurePage.fbundle")); //$NON-NLS-1$
		else
			fBundleCheck.setText(PDEPlugin.getResourceString("ProjectStructurePage.pbundle")); //$NON-NLS-1$
		
		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString("ProjectStructurePage.note")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 250;
		gd.horizontalIndent = 30;
		label.setLayoutData(gd);		
	}

	public void setVisible(boolean visible) {
		if (visible && fFirstVisible) {
			fFirstVisible = false;
			String fullName = fProjectProvider.getProjectName().trim();
			StringTokenizer tok = new StringTokenizer(fullName, "."); //$NON-NLS-1$
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				if (!tok.hasMoreTokens())
					fLibraryText.setText(token + ".jar"); //$NON-NLS-1$
			}
		}
		if (!visible) updateData();
		super.setVisible(visible);
	}
	
	private void validatePage() {
		String errorMessage = null;
		if (fJavaButton.getSelection()) {
			if (fLibraryText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ProjectStructurePage.noLibrary"); //$NON-NLS-1$
			} else if (fOutputText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ProjectStructurePage.noOutput"); //$NON-NLS-1$
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	public void updateData() {		
		fData.setIsSimple(fSimpleButton.getSelection());
		fData.setHasBundleStructure(fBundleCheck.getSelection());
		if (fJavaButton.getSelection()) {
			fData.setLibraryName(fLibraryText.getText().trim());
			fData.setSourceFolderName(fSourceText.getText().trim());
			fData.setOutputFolderName(fOutputText.getText().trim());
		}
	}
	
	public boolean isSimpleProject() {
		return fSimpleButton.getSelection();
	}
	
	public boolean hasBundleStructure() {
		return fBundleCheck.getSelection();
	}
}
