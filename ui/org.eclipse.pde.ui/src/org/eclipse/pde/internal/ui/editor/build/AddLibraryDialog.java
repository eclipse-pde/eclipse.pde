/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

public class AddLibraryDialog extends SelectionStatusDialog {
	private String newName;
	private String[] libraries;
	private IPluginModelBase model;
	private static String init = "library.jar";
	private Text text;
	private Image libImage;
	private TableViewer libraryViewer;
	private DuplicateStatusValidator validator;

	class DuplicateStatusValidator {
		public IStatus validate (String text){
			if(libraries==null || libraries.length==0)
			return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				"",
				null);
			
			if (!text.endsWith(".jar"))
				text = text + ".jar";
				
			for (int i =0;i<libraries.length; i++){
				if (libraries[i].equals(text))
				return new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					PDEPlugin.getResourceString(
						"BuildPropertiesEditor.RuntimeInfoSection.missingSource.duplicateLibrary"),
					null);
			}
			return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				"",
				null);

		}
	}
	class TableContentProvider extends DefaultContentProvider implements IStructuredContentProvider{
		public Object[] getElements(Object input){
			if (input instanceof IPluginModelBase){
				return ((IPluginModelBase)input).getPluginBase().getLibraries();
			}
			return new Object[0];
		}
	}
	
	class TableLabelProvider extends LabelProvider implements ITableLabelProvider{
		public String getColumnText(Object obj, int index){
			return obj.toString();
		}
		
		public Image getColumnImage(Object obj, int index){
			return libImage;
		}
	}
	
	public AddLibraryDialog(Shell shell, String[] libraries, IPluginModelBase model) {
		super(shell);
		setLibraryNames(libraries);
		setPluginModel(model);
		initializeImages();
		initializeValidator();
		setStatusLineAboveButtons(true);
	}
	
	public void setPluginModel(IPluginModelBase model){
		this.model = model;
	}
	
	private void initializeImages(){
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		libImage= provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);	
	}
	
	public void setLibraryNames(String[] libraries) {
		this.libraries = libraries;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);
		
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesEditor.AddLibraryDialog.label")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			updateStatus(validator.validate(text.getText()));
			}
		});
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Table table = new Table(container, SWT.FULL_SELECTION | SWT.BORDER);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		libraryViewer = new TableViewer(table);
		libraryViewer.setContentProvider(new TableContentProvider());
		libraryViewer.setLabelProvider(new TableLabelProvider());
		libraryViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent e){
				ISelection sel = e.getSelection();
				Object obj = ((IStructuredSelection)sel).getFirstElement();
				text.setText(obj!=null ? obj.toString() : "");
			}
		});
		libraryViewer.setInput(model);
		return container;
	}
	
	public int open() {
		text.setText(init);
		text.selectAll();
		return super.open();
	}

	protected void computeResult(){
		
	}
	  
	public String getNewName() {
		return newName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		newName = text.getText();
		super.okPressed();
	}
	
	private void initializeValidator(){
		this.validator = new DuplicateStatusValidator();
	}

}
