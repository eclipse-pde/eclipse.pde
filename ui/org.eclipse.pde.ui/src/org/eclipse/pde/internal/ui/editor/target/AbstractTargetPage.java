package org.eclipse.pde.internal.ui.editor.target;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.LoadTargetOperation;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.IProgressService;

public abstract class AbstractTargetPage extends PDEFormPage {

	public AbstractTargetPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		ControlContribution save = new ControlContribution("Set") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				Button loadButton = new Button(parent, SWT.PUSH);
				loadButton.setText(PDEUIMessages.AbstractTargetPage_setTarget);
				loadButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						doLoadTarget();
					}
				});
				return loadButton;
			}
		};
		form.getToolBarManager().add(save);
		form.getToolBarManager().update(true);
		super.createFormContent(managedForm);
	}
	
	private void doLoadTarget() {
		IRunnableWithProgress run = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					ITargetModel model = getTargetModel();
					if (!model.isLoaded()) {
						MessageDialog.openError(getManagedForm().getForm().getShell(), PDEUIMessages.TargetPlatformPreferencePage_invalidTitle, PDEUIMessages.TargetPlatformPreferencePage_invalidDescription);
						monitor.done();
						return;
					}
					LoadTargetOperation op = new LoadTargetOperation(getTarget());
					PDEPlugin.getWorkspace().run(op, monitor);
					Object[] features = op.getMissingFeatures();
					Object[] plugins = op.getMissingPlugins();
					if (plugins.length + features.length > 0)
						TargetErrorDialog.showDialog(getManagedForm().getForm().getShell(), features, plugins);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException(e.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			service.runInUI(service, run, PDEPlugin.getWorkspace().getRoot());
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}
	
	private ITarget getTarget() {
		return getTargetModel().getTarget();
	}
	
	private ITargetModel getTargetModel() {
		return ((ITargetModel) getPDEEditor().getAggregateModel());
	}
	
	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/tools/file_wizards/new_target_definition.htm"; //$NON-NLS-1$
	}


}
