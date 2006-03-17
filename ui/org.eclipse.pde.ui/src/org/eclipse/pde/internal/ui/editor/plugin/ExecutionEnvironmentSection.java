/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.preferences.PDEPreferencesUtil;
import org.eclipse.pde.internal.ui.wizards.plugin.ClasspathComputer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.Constants;

public class ExecutionEnvironmentSection extends TableSection {

	private TableViewer fEETable;
	private Action fRemoveAction;
	private Action fAddAction;
	
	class EELabelProvider extends LabelProvider {
		
		private Image fImage;
		
		public EELabelProvider() {
			fImage = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
		}
		
		public Image getImage(Object element) {
			return fImage;
		}
		
		public String getText(Object element) {
			if (element instanceof IExecutionEnvironment)
				return ((IExecutionEnvironment)element).getId();
			return super.getText(element);
		}
		
		public void dispose() {
			if (fImage != null)
				fImage.dispose();
			super.dispose();
		}
	}
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IBundleModel) {
				IBundleModel model = (IBundleModel)inputElement;
				IBundle bundle = model.getBundle();
				IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
				if (header instanceof RequiredExecutionEnvironmentHeader) {
					return ((RequiredExecutionEnvironmentHeader)header).getEnvironments();
				}
			}
			return new Object[0];
		}
	}

	public ExecutionEnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.RequiredExecutionEnvironmentSection_add, PDEUIMessages.RequiredExecutionEnvironmentSection_remove, PDEUIMessages.RequiredExecutionEnvironmentSection_up, PDEUIMessages.RequiredExecutionEnvironmentSection_down});
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.RequiredExecutionEnvironmentSection_title);
		if (isFragment())
			section.setDescription(PDEUIMessages.RequiredExecutionEnvironmentSection_fragmentDesc);
		else
			section.setDescription(PDEUIMessages.RequiredExecutionEnvironmentSection_pluginDesc);		
		
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION|SWT.MULTI, 2, toolkit);
		fEETable = tablePart.getTableViewer();
		fEETable.setContentProvider(new ContentProvider());
		fEETable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		
		Hyperlink link = toolkit.createHyperlink(container, PDEUIMessages.BuildExecutionEnvironmentSection_configure, SWT.NONE);
		link.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		link.addHyperlinkListener(new IHyperlinkListener() {
			public void linkEntered(HyperlinkEvent e) {
			}
			public void linkExited(HyperlinkEvent e) {
			}
			public void linkActivated(HyperlinkEvent e) {
				PDEPreferencesUtil.showPreferencePage(new String[] {"org.eclipse.jdt.debug.ui.jreProfiles"}); //$NON-NLS-1$
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		link.setLayoutData(gd);
		
		final IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				link = toolkit.createHyperlink(container, PDEUIMessages.ExecutionEnvironmentSection_updateClasspath, SWT.NONE);
				link.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
				link.addHyperlinkListener(new IHyperlinkListener() {
					public void linkEntered(HyperlinkEvent e) {
					}
					public void linkExited(HyperlinkEvent e) {
					}
					public void linkActivated(HyperlinkEvent e) {
						try {
							getPage().getEditor().doSave(null);
							IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
							if (model != null)
								ClasspathComputer.setClasspath(project, model);
						} catch (CoreException e1) {
						}
					}
				});
				gd = new GridData();
				gd.horizontalSpan = 2;
				link.setLayoutData(gd);
			}
		} catch (CoreException e1) {
		}

		makeActions();
        
        IBundleModel model = getBundleModel();
        if (model != null) {
        	fEETable.setInput(model);
        	model.addModelChangedListener(this);
        }
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}
	
	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
	}
	
	public void refresh() {
		fEETable.refresh();
		updateButtons();
	}
	
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleAdd();
				break;
           case 1:
                handleRemove();
                break;
			case 2 :
				handleUp();
				break;
			case 3 :
				handleDown();
				break;
		}
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fAddAction);
		if (!fEETable.getSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRemoveAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
	
    private void makeActions() {
        fAddAction = new Action(PDEUIMessages.RequiredExecutionEnvironmentSection_add) {
            public void run() {
                handleAdd();
            }
        };
        fAddAction.setEnabled(isEditable());
        
        fRemoveAction = new Action(PDEUIMessages.NewManifestEditor_LibrarySection_remove) {
            public void run() {
                handleRemove();
            }
        };
        fRemoveAction.setEnabled(isEditable());
    }
	
	private void updateButtons() {
        Table table = fEETable.getTable();
        int count = table.getItemCount();
        boolean canMoveUp = count > 0 && table.getSelection().length == 1 && table.getSelectionIndex() > 0;
        boolean canMoveDown = count > 0 && table.getSelection().length == 1 && table.getSelectionIndex() < count - 1;
        
        TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(0, isEditable());
        tablePart.setButtonEnabled(1, isEditable() && table.getSelection().length > 0);
        tablePart.setButtonEnabled(2, isEditable() && canMoveUp);
        tablePart.setButtonEnabled(3, isEditable() && canMoveDown);
    }	
	
	private void handleDown() {
		int selection = fEETable.getTable().getSelectionIndex();
		swap(selection, selection + 1);
	}

	private void handleUp() {
		int selection = fEETable.getTable().getSelectionIndex();
		swap(selection, selection - 1);
	}
	
	public void swap(int index1, int index2) {
		RequiredExecutionEnvironmentHeader header = getHeader();
		header.swap(index1, index2);
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)fEETable.getSelection();
		if (ssel.size() > 0) {
			Iterator iter = ssel.iterator();
			while (iter.hasNext()) {
				Object object = iter.next();
				if (object instanceof ExecutionEnvironment) {
					getHeader().removeExecutionEnvironment((ExecutionEnvironment)object);
				}
			}
		}
	}

	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new EELabelProvider());
		dialog.setElements(getEnvironments());
		dialog.setAllowDuplicates(false);
		dialog.setMultipleSelection(true);
		dialog.setTitle(PDEUIMessages.RequiredExecutionEnvironmentSection_dialog_title);
		dialog.setMessage(PDEUIMessages.RequiredExecutionEnvironmentSection_dialogMessage);
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			IManifestHeader header = getHeader();
			if (header == null) {
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < result.length; i++) {
					if (buffer.length() > 0) {
						buffer.append(","); //$NON-NLS-1$
						buffer.append(getLineDelimiter());
						buffer.append(" "); //$NON-NLS-1$
					}
					buffer.append(((IExecutionEnvironment)result[i]).getId());
				}
				getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, buffer.toString());
			} else {
				RequiredExecutionEnvironmentHeader ee = (RequiredExecutionEnvironmentHeader)header;
				for (int i = 0; i < result.length; i++) {
					ee.addExecutionEnvironment((IExecutionEnvironment)result[i]);
				}
			}
		}
	}
	
    private String getLineDelimiter() {
		BundleInputContext inputContext = getBundleContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}
	
	private Object[] getEnvironments() {
		RequiredExecutionEnvironmentHeader header = getHeader();
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		if (header == null)
			return envs;
		ArrayList list = new ArrayList();
		for (int i = 0; i < envs.length; i++) {
			if (!header.hasExecutionEnvironment(envs[i]))
				list.add(envs[i]);
		}
		return list.toArray();
	}
	
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				Table table = fEETable.getTable();
				if (objects[i] instanceof ExecutionEnvironment) {
                    int index = table.getSelectionIndex();
                    fEETable.remove(objects[i]);
                    table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
				}
			}
			updateButtons();
		} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof ExecutionEnvironment) {
					fEETable.add(objects[i]);
				}
			}
			if (objects.length > 0)
				fEETable.setSelection(new StructuredSelection(objects[objects.length - 1]));
			updateButtons();
		} else if (Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.equals(e.getChangedProperty())){
			refresh();
		}
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}
	
	private IBundle getBundle() {
		IBundleModel model = getBundleModel();
		return model == null ? null : model.getBundle();
	}
	
	private IBundleModel getBundleModel() {
		BundleInputContext context = getBundleContext();
		return context == null ? null : (IBundleModel)context.getModel();		
	}
	
	protected RequiredExecutionEnvironmentHeader getHeader() {
		IBundle bundle = getBundle();
		if (bundle == null) return null;
		IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader)
			return (RequiredExecutionEnvironmentHeader) header;
		return null;
	}
	
	protected boolean isFragment() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		IPluginModelBase model = (IPluginModelBase) manager.getAggregateModel();
		return model.isFragmentModel();
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}
	
    protected void selectionChanged(IStructuredSelection selection) {
		if (getPage().getModel().isEditable())
			updateButtons();
	}


}
