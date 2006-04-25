package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class MissingResourcePage extends PDEFormPage {

	public MissingResourcePage(FormEditor editor) {
		super(editor, "missing", PDEUIMessages.MissingResourcePage_missingResource); //$NON-NLS-1$
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		Composite comp = managedForm.getToolkit().createComposite(form);
		comp.setLayout(new GridLayout());
		IPersistableElement persistable = getEditorInput().getPersistable();
		String text;
		if (persistable instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput)persistable).getFile();
			text = NLS.bind(PDEUIMessages.MissingResourcePage_unableToOpenFull,
					new String[] {
					PDEUIMessages.MissingResourcePage_unableToOpen,
					file.getProjectRelativePath().toString(),
					file.getProject().getName()});
		} else
			text = PDEUIMessages.MissingResourcePage_unableToOpen;
		form.setText(text);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
}
