/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.search.FindReferencesAction;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewSchemaFileWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class ExtensionPointDetails extends PDEDetails {
	private IPluginExtensionPoint fInput;
	private FormEntry fIdEntry;
	private FormEntry fNameEntry;
	private FormEntry fSchemaEntry;
	private FormText fRichText;
	private String fRichTextData;

	private static final String SCHEMA_RTEXT_DATA = PDEUIMessages.ExtensionPointDetails_schemaLinks; 
	private static final String NO_SCHEMA_RTEXT_DATA = PDEUIMessages.ExtensionPointDetails_noSchemaLinks; 
	public ExtensionPointDetails() {
	}
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.leftMargin = 5;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION|ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;
		section.marginWidth = 5;
		section.setText(PDEUIMessages.ExtensionPointDetails_title); 
		section
				.setDescription(PDEUIMessages.ExtensionPointDetails_desc); 
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		//toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		boolean paintedBorder = toolkit.getBorderStyle()!=SWT.BORDER;
		glayout.marginWidth = glayout.marginHeight = 2;//paintedBorder?2:0;
		glayout.numColumns = 3;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		
		fIdEntry = new FormEntry(client, toolkit, PDEUIMessages.ExtensionPointDetails_id, null, false); 
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setId(fIdEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		});
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.ExtensionPointDetails_name, null, false); 
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null)
					try {
						fInput.setName(fNameEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		boolean editable = getPage().getModel().isEditable();
		fSchemaEntry = new FormEntry(client, toolkit, PDEUIMessages.ExtensionPointDetails_schema, PDEUIMessages.ExtensionPointDetails_browse, editable); // 
		fSchemaEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setSchema(fSchemaEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
					updateRichText();
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				IProject project = getPage().getPDEEditor().getCommonProject();
				if (fSchemaEntry.getValue() == null || fSchemaEntry.getValue().length() ==0){
					generateSchema();
					return;
				}
				IFile file = project.getFile(fSchemaEntry.getValue());
				if (file.exists())
					openSchemaFile(file);
				else
					generateSchema();
			}

			public void browseButtonSelected(FormEntry entry) {
				final IProject project = getPage().getPDEEditor().getCommonProject();
				ElementTreeSelectionDialog dialog =
					new ElementTreeSelectionDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						new WorkbenchLabelProvider(),
						new WorkbenchContentProvider());
				dialog.setTitle(PDEUIMessages.ManifestEditor_ExtensionPointDetails_schemaLocation_title); 
				dialog.setMessage(PDEUIMessages.ManifestEditor_ExtensionPointDetails_schemaLocation_desc); 
				dialog.setDoubleClickSelects(false);
				dialog.setAllowMultiple(false);
				dialog.addFilter(new ViewerFilter(){
					public boolean select(Viewer viewer, Object parent,
							Object element) {
						if (element instanceof IFile){
							String ext = ((IFile)element).getFullPath().getFileExtension();
								return "exsd".equals(ext) || "mxsd".equals(ext); //$NON-NLS-1$ //$NON-NLS-2$
						} else if (element instanceof IContainer){ // i.e. IProject, IFolder
							try {
								IResource[] resources = ((IContainer)element).members();
								for (int i = 0; i < resources.length; i++){
									if (select(viewer, parent, resources[i]))
										return true;
								}
							} catch (CoreException e) {
								PDEPlugin.logException(e);
							}
						}
						return false;
					}
				});
				dialog.setValidator(new ISelectionStatusValidator() {
					public IStatus validate(Object[] selection) {
						IPluginModelBase model = (IPluginModelBase) getPage()
								.getPDEEditor().getAggregateModel();
						String pluginName = model.getPluginBase().getId();

						if (selection == null || selection.length != 1
								|| !(selection[0] instanceof IFile))
							return new Status(
									IStatus.ERROR,
									pluginName,
									IStatus.ERROR,
									PDEUIMessages.ManifestEditor_ExtensionPointDetails_validate_errorStatus, 
									null);
						IFile file = (IFile) selection[0];
						String ext = file.getFullPath().getFileExtension();
						if ("exsd".equals(ext) || "mxsd".equals(ext)) //$NON-NLS-1$ //$NON-NLS-2$
							return new Status(IStatus.OK, pluginName,
									IStatus.OK, "", null); //$NON-NLS-1$
						return new Status(
								IStatus.ERROR,
								pluginName,
								IStatus.ERROR,
								PDEUIMessages.ManifestEditor_ExtensionPointDetails_validate_errorStatus, 
								null);
					}
				});
				dialog.setDoubleClickSelects(true);
				dialog.setStatusLineAboveButtons(true);
				dialog.setInput(project);
				dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
				String filePath = fSchemaEntry.getValue();
				if (filePath!=null && filePath.length()!=0 && project.exists(new Path(filePath)))
					dialog.setInitialSelection(project.getFile(new Path(filePath)));
				else
					dialog.setInitialSelection(null);
				if (dialog.open() == Window.OK) {
					Object[] elements = dialog.getResult();
					if (elements.length >0){
						IResource elem = (IResource) elements[0];
						fSchemaEntry.setValue(elem.getProjectRelativePath().toString());
					}
				}
			}
		});
		createSpacer(toolkit, client, 2);
		fRichText = toolkit.createFormText(parent, true);
		td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		td.indent = 10;
		fRichText.setLayoutData(td);
		fRichText.setImage("schema", PDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				PDEPluginImages.DESC_SCHEMA_OBJ));
		fRichText.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				PDEPluginImages.DESC_DOC_SECTION_OBJ));
		fRichText.setImage("search", PDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				PDEPluginImages.DESC_PSEARCH_OBJ));
		fRichText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				IBaseModel model = getPage().getPDEEditor().getAggregateModel();
				String pluginID = ((IPluginModelBase)model).getPluginBase().getId();
				String pointID = pluginID + "." + fInput.getId(); //$NON-NLS-1$
				if (e.getHref().equals("search")) { //$NON-NLS-1$
					new FindReferencesAction(fInput, pluginID).run();
				} else {
					new ShowDescriptionAction(pointID).run();
				}
			}
		});
		
		fIdEntry.setEditable(isEditable());
		fNameEntry.setEditable(isEditable());
		fSchemaEntry.setEditable(isEditable());
		toolkit.paintBordersFor(client);
		section.setClient(client);
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		model.addModelChangedListener(this);
		markDetailsPart(section);
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType()==IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(fInput))
				refresh();
		}
	}
	private void update() {
		fIdEntry.setValue(
				fInput != null && fInput.getId() != null ? fInput.getId() : "", //$NON-NLS-1$
				true);
		fNameEntry.setValue(fInput != null && fInput.getName() != null ? fInput
				.getName() : "", true); //$NON-NLS-1$
		fSchemaEntry.setValue(fInput != null && fInput.getSchema() != null ? fInput
				.getSchema() : "", true); //$NON-NLS-1$
		updateRichText();
	}
	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fSchemaEntry.cancelEdit();
		updateRichText();
		super.cancelEdit();
	}
	private void updateRichText() {
		boolean hasSchema = fSchemaEntry.getValue().length() > 0;
		if (hasSchema && fRichTextData == SCHEMA_RTEXT_DATA)
			return;
		if (!hasSchema && fRichTextData == NO_SCHEMA_RTEXT_DATA)
			return;
		fRichTextData = hasSchema ? SCHEMA_RTEXT_DATA : NO_SCHEMA_RTEXT_DATA;
		fRichText.setText(fRichTextData, true, false);
		getManagedForm().getForm().reflow(true);
	}
	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = IPDEUIConstants.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(
						new FileEditorInput(file),
						editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	
	private void generateSchema() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator
			.showWhile(getPage().getPartControl().getDisplay(), new Runnable() {
			public void run() {
				NewSchemaFileWizard wizard =
					new NewSchemaFileWizard(project, fInput, true);
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				if(dialog.open() == Window.OK)
					update();
			}
		});
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			fInput = (IPluginExtensionPoint) ssel.getFirstElement();
		} else
			fInput = null;
		update();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fNameEntry.commit();
		fSchemaEntry.commit();
		super.commit(onSave);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		fIdEntry.getText().setFocus();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}
	
}
