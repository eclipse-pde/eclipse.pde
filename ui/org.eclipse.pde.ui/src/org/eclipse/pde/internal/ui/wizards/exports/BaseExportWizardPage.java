package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class BaseExportWizardPage extends WizardPage {
	private String S_EXPORT_UPDATE = "exportUpdate";
	private IStructuredSelection selection;
	private ExportPart exportPart;
	private Button zipRadio;
	private Button updateRadio;
	
	class ExportListProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}
	
	class ExportPart extends WizardCheckboxTablePart {
		public ExportPart(String label) {
			super(label);
		}
		
		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
		}
	}
	
	public BaseExportWizardPage(IStructuredSelection selection, String name, String choiceLabel) {
		super(name);
		this.selection = selection;
		exportPart = new ExportPart(choiceLabel);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		exportPart.createControl(container);
		initializeList();
		
		createLabel(container, "", 2);
		createLabel(container, "Export plug-ins and fragments into", 2);
		zipRadio = createRadioButton(container, "deployable &ZIP file");
		updateRadio = createRadioButton(container, "&JAR archives for the Update site");
		loadSettings();
		setControl(container);
	}
	
	private void createLabel(Composite container, String text, int span) {
		Label label = new Label(container, SWT.NULL);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}
	
	private Button createRadioButton(Composite container, String text) {
		Button button = new Button(container, SWT.RADIO);
		button.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 10;
		button.setLayoutData(gd);
		return button;
	}
	
	protected Object[] getListElements() {
		return new Object[0];
	}
	
	protected void initializeList() {
		TableViewer viewer = exportPart.getTableViewer();
		viewer.setContentProvider(new ExportListProvider());
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		exportPart.getTableViewer().setInput(PDECore.getDefault().getWorkspaceModelManager());
		checkSelected();
		pageChanged();
	}
	
	protected void checkSelected() {
		Object[] elems = selection.toArray();
		ArrayList checked = new ArrayList(elems.length);

		for (int i = 0; i < elems.length; i++) {
			Object elem = elems[i];
			IProject project = null;

			if (elem instanceof IFile) {
				IFile file = (IFile) elem;
				project = file.getProject();
			} else if (elem instanceof IProject) {
				project = (IProject) elem;
			} else if (elem instanceof IJavaProject) {
				project = ((IJavaProject) elem).getProject();
			}
			if (project != null) {
				IModel model = findModelFor(project);
				if (model != null) {
					checked.add(model);
				}
			}
		}
		exportPart.setSelection(checked.toArray());
	}
	
	protected IModel findModelFor(IProject project) {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceModel(project);
	}
	
	protected void pageChanged() {
		setPageComplete(exportPart.getSelectionCount()>0);
	}
	
	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean exportUpdate = settings.getBoolean(S_EXPORT_UPDATE);
		zipRadio.setSelection(!exportUpdate);
		updateRadio.setSelection(exportUpdate);
	}
	
	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_EXPORT_UPDATE, updateRadio.getSelection());
	}
	
	public Object [] getSelectedItems() {
		return exportPart.getSelection();
	}
	
	public boolean getExportZip() {
		return zipRadio.getSelection();
	}
}
