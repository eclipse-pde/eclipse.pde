/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.LoadTargetOperation;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.IProgressService;

public class TargetDefinitionSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fPath;
	private Button fUseDefault;
	private Button fCustomPath;
	private Button fFileSystem;
	private Button fVariable;
	private static int NUM_COLUMNS = 5;
	
	public TargetDefinitionSection(PDEFormPage page, Composite parent) {
		super(page, parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		createClient(getSection(), page.getEditor().getToolkit());
		getSection().setExpanded(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		text.setText(PDEUIMessages.TargetDefinitionSection_description, true, false);
		text.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				doLoadTarget();
			}
		});
		section.setDescriptionControl(text);
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = NUM_COLUMNS ;
		client.setLayout(layout);

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		createNameEntry(client, toolkit, actionBars);
		
		createLocation(client, toolkit, actionBars);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText(PDEUIMessages.TargetDefinitionSection_title); 
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_name, null, false); 
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getTarget().setName(entry.getValue());
			}
		});
		GridData gd = (GridData)fNameEntry.getText().getLayoutData();
		gd.horizontalSpan = 4;
		fNameEntry.setEditable(isEditable());
	}
	
	private void doLoadTarget() {
		IRunnableWithProgress run = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					ITargetModel model = getModel();
					if (!model.isLoaded()) {
						MessageDialog.openError(getSection().getShell(), PDEUIMessages.TargetPlatformPreferencePage_invalidTitle, PDEUIMessages.TargetPlatformPreferencePage_invalidDescription);
						monitor.done();
						return;
					}
					LoadTargetOperation op = new LoadTargetOperation(getTarget());
					PDEPlugin.getWorkspace().run(op, monitor);
					Object[] features = op.getMissingFeatures();
					Object[] plugins = op.getMissingPlugins();
					if (plugins.length + features.length > 0)
						handleWarningDialog(features, plugins);
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
	
	private void handleWarningDialog(Object[] features, Object[] plugins) {
		TargetErrorDialog.showDialog(getSection().getShell(), features, plugins);
	}

	private void createLocation(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		Label label = toolkit.createLabel(client, PDEUIMessages.TargetDefinitionSection_targetLocation);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fUseDefault = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_sameAsHost, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 5;
		gd.horizontalIndent = 15;
		fUseDefault.setLayoutData(gd);
		fUseDefault.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fUseDefault.getSelection()) {
					fPath.getText().setEditable(false);
					fPath.setValue("", true); //$NON-NLS-1$
					fFileSystem.setEnabled(false);
					fVariable.setEnabled(false);
					getLocationInfo().setDefault(true);
				}
			}
		});
		
		fCustomPath = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_location, SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 15;
		fCustomPath.setLayoutData(gd);
		fCustomPath.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fCustomPath.getSelection()) {
					ILocationInfo info = getLocationInfo();
					fPath.getText().setEditable(true);
					fPath.setValue(info.getPath(), true);
					fFileSystem.setEnabled(true);
					fVariable.setEnabled(true);
					info.setDefault(false);
				}
			}
		});
		
		fPath = new FormEntry(client, toolkit, null, null, false); 
		fPath.getText().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPath.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLocationInfo().setPath(fPath.getValue());
			}
		});
		
		fFileSystem = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_fileSystem, SWT.PUSH);
		fFileSystem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFileSystem();
			}
		});
		
		fVariable = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_variables, SWT.PUSH);
		fVariable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInsertVariable();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fPath.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fPath.cancelEdit();
		super.cancelEdit();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ITarget target = getTarget();
		fNameEntry.setValue(target.getName(), true);
		ILocationInfo info = getLocationInfo();
		fUseDefault.setSelection(info.useDefault());
		fCustomPath.setSelection(!info.useDefault());
		String path = (info.useDefault()) ? "" : info.getPath(); //$NON-NLS-1$
		fPath.setValue(path,true);
		fPath.getText().setEditable(!info.useDefault());
		fFileSystem.setEnabled(!info.useDefault());
		fVariable.setEnabled(!info.useDefault());
		super.refresh();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
	
	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(getSection().getShell());
		dialog.setFilterPath(fPath.getValue());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null) {
			fPath.setValue(result);
			getLocationInfo().setPath(result);
		}
	}
	
	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
					new StringVariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell());
		if (dialog.open() == Window.OK) {
			fPath.getText().insert(dialog.getVariableExpression());
			// have to setValue to make sure getValue reflects the actual text in the Text object.
			fPath.setValue(fPath.getText().getText());
			getLocationInfo().setPath(fPath.getText().getText());
		}
	}
	
	private ILocationInfo getLocationInfo() {
		ILocationInfo info = getTarget().getLocationInfo();
		if (info == null) {
			info = getModel().getFactory().createLocation();
			getTarget().setLocationInfo(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
}
