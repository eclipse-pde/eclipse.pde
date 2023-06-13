package $packageName$;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;


public class $wizardPageClassName$ extends WizardNewFileCreationPage {
	
	protected FileFieldEditor editor;

	public $wizardPageClassName$(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(pageName); //$NON-NLS-1$
		setDescription("Import a file from the local file system into the workspace"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */	
	protected void createAdvancedControls(Composite parent) {
		Composite fileSelectionArea = new Composite(parent, SWT.NONE);
		GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.FILL_HORIZONTAL);
		fileSelectionArea.setLayoutData(fileSelectionData);

		GridLayout fileSelectionLayout = new GridLayout();
		fileSelectionLayout.numColumns = 3;
		fileSelectionLayout.makeColumnsEqualWidth = false;
		fileSelectionLayout.marginWidth = 0;
		fileSelectionLayout.marginHeight = 0;
		fileSelectionArea.setLayout(fileSelectionLayout);
		
		editor = new FileFieldEditor("fileSelect","Select File: ",fileSelectionArea); //$NON-NLS-1$ //$NON-NLS-2$
		editor.getTextControl(fileSelectionArea).addModifyListener(e -> {
			IPath path = IPath.fromOSString($wizardPageClassName$.this.editor.getStringValue());
			setFileName(path.lastSegment());
		});
%if wizardFileFilters == "All"
		String[] extensions = new String[] { "*.*" }; //$NON-NLS-1$
%else
%	if wizardFileFilters == "Images"
		String[] extensions = new String[] { "*.jpg;*.gif;*.bmp" }; //$NON-NLS-1$
%	else
%		if wizardFileFilters == "Docs"
		String[] extensions = new String[] { "*.doc;*.txt;*.pdf" }; //$NON-NLS-1$
%		else
%			if wizardFileFilters == "Archives"
		String[] extensions = new String[] { "*.zip;*.tar;*.jar" }; //$NON-NLS-1$
%			endif	
%		endif	
%	endif
%endif	
		editor.setFileExtensions(extensions);
		fileSelectionArea.moveAbove(null);

	}
	
	 /* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getInitialContents()
	 */
	protected InputStream getInitialContents() {
		try {
			return new FileInputStream(new File(editor.getStringValue()));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getNewFileLabel()
	 */
	protected String getNewFileLabel() {
		return "New File Name:"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return Status.OK_STATUS;
	}
}
