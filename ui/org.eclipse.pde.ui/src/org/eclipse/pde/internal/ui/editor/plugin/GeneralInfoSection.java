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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.IDE;

public class GeneralInfoSection extends PDESection
		implements
			IInputContextListener {
	public static final String KEY_MATCH = "ManifestEditor.PluginSpecSection.versionMatch"; //$NON-NLS-1$
	public static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	public static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	public static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	public static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater"; //$NON-NLS-1$
	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fNameEntry;
	private FormEntry fProviderEntry;
	private FormEntry fClassEntry;
	private Text fPluginIdText;
	private FormEntry fPluginVersionEntry;
	private Label fMatchLabel;
	private ComboPart fMatchCombo;
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.PluginSpecSection.title")); //$NON-NLS-1$
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		if (isFragment())
			section
					.setDescription(PDEPlugin
							.getResourceString("ManifestEditor.PluginSpecSection.fdesc")); //$NON-NLS-1$
		else
			section
					.setDescription(PDEPlugin
							.getResourceString("ManifestEditor.PluginSpecSection.desc")); //$NON-NLS-1$
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		if (isFragment())
			layout.numColumns = 2;
		else
			layout.numColumns = 3;
		client.setLayout(layout);

		section.setClient(client);
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();
		createIDEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createProviderEntry(client, toolkit, actionBars);
		if (isFragment()) {
			createPluginIDEntry(client, toolkit, actionBars);
			createPluginVersionEntry(client, toolkit, actionBars);
			createMatchCombo(client, toolkit, actionBars);
			if (isBundleMode()) {
				fMatchLabel.setVisible(false);
				fMatchCombo.getControl().setVisible(false);
			}
		} else {
			createClassEntry(client, toolkit, actionBars);
		}
		toolkit.paintBordersFor(client);
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
		InputContextManager manager = getPage().getPDEEditor()
				.getContextManager();
		manager.addInputContextListener(this);
	}
	public String getContextId() {
		if (getPluginBase() instanceof IBundlePluginBase)
			return BundleInputContext.CONTEXT_ID;
		return PluginInputContext.CONTEXT_ID;
	}
	private IPluginBase getPluginBase() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
	private void createIDEntry(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, PDEPlugin
				.getResourceString("GeneralInfoSection.id"), null, false); //$NON-NLS-1$
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setId(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fIdEntry.setEditable(isEditable());
	}
	private void createVersionEntry(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		fVersionEntry = new FormEntry(client, toolkit, PDEPlugin
				.getResourceString("GeneralInfoSection.version"), null, false); //$NON-NLS-1$
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fVersionEntry.setEditable(isEditable());
	}
	private void createNameEntry(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEPlugin
				.getResourceString("GeneralInfoSection.name"), null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fNameEntry.setEditable(isEditable());
	}
	private void createProviderEntry(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		fProviderEntry = new FormEntry(client, toolkit, PDEPlugin
				.getResourceString("GeneralInfoSection.provider"), null, //$NON-NLS-1$
				false);
		fProviderEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setProviderName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fProviderEntry.setEditable(isEditable());
	}
	private void createClassEntry(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		boolean editable = getPage().getModel().isEditable();
		fClassEntry = new FormEntry(
				client,
				toolkit,
				PDEPlugin.getResourceString("GeneralInfoSection.class"), PDEPlugin.getResourceString("GeneralInfoSection.browse"), //$NON-NLS-1$ //$NON-NLS-2$
				editable);
		fClassEntry
				.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
					public void textValueChanged(FormEntry entry) {
						try {
							((IPlugin) getPluginBase()).setClassName(entry
									.getValue());
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						}
					}
					public void linkActivated(HyperlinkEvent e) {
						String value = fClassEntry.getValue();
						value = trimNonAlphaChars(value);
						if (value.length() > 0 && doesClassExist(value))
							doOpenClass();
						else {
							JavaAttributeValue javaAttVal = createJavaAttributeValue();
							JavaAttributeWizard wizard = new JavaAttributeWizard(
									javaAttVal);
							WizardDialog dialog = new WizardDialog(PDEPlugin
									.getActiveWorkbenchShell(), wizard);
							dialog.create();
							SWTUtil.setDialogSize(dialog, 400, 500);
							int result = dialog.open();
							if (result == WizardDialog.OK) {
								String newValue = wizard.getClassNameWithArgs();
								fClassEntry.setValue(newValue);
							}
						}
					}
					public void browseButtonSelected(FormEntry entry) {
						doOpenSelectionDialog();
					}
				});
		fClassEntry.setEditable(isEditable());
	}
	private String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}
	private boolean doesClassExist(String className) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		String path = className.replace('.', '/') + ".java"; //$NON-NLS-1$
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);

				IJavaElement result = javaProject.findElement(new Path(path));
				return result != null;
			} 
			
			IResource resource = project.findMember(new Path(path));
			return resource != null;
		} catch (JavaModelException e) {
			return false;
		} catch (CoreException e) {
			return false;
		}
	}
	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		String value = fClassEntry.getValue();
		return new JavaAttributeValue(project, model, null, value);
	}
	private void createPluginIDEntry(Composite parent, FormToolkit toolkit,
			IActionBars actionBars) {
		Hyperlink link = toolkit.createHyperlink(parent, PDEPlugin
				.getResourceString("GeneralInfoSection.pluginId"), //$NON-NLS-1$
				SWT.NULL);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				ManifestEditor.openPluginEditor(fPluginIdText.getText());
			}
		});
		Composite client = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.makeColumnsEqualWidth = false;
		layout.numColumns = 2;
		client.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		client.setLayoutData(gd);
		fPluginIdText = toolkit.createText(client, "", SWT.SINGLE); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		fPluginIdText.setLayoutData(gd);

		fPluginIdText.setEditable(isEditable());
		fPluginIdText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					if (!((IFragment) getPluginBase()).getPluginId().equals(
							fPluginIdText.getText()))
						((IFragment) getPluginBase()).setPluginId(fPluginIdText
								.getText());
				} catch (CoreException e1) {
					PDEPlugin.logException(e1);
				}
			}
		});
		Button button = toolkit.createButton(client, PDEPlugin
				.getResourceString("GeneralInfoSection.browse"), SWT.PUSH); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PluginSelectionDialog dialog = new PluginSelectionDialog(
						getSection().getShell(), false, false);
				dialog.create();
				if (dialog.open() == PluginSelectionDialog.OK) {
					try {
						IPluginModel model = (IPluginModel) dialog
								.getFirstResult();
						IPlugin plugin = model.getPlugin();
						fPluginIdText.setText(plugin.getId());
						((IFragment) getPluginBase()).setPluginId(plugin
								.getId());
						fPluginVersionEntry.setValue(plugin.getVersion(), true);
						((IFragment) getPluginBase()).setPluginVersion(plugin
								.getVersion());
					} catch (CoreException e1) {
						PDEPlugin.logException(e1);
					}
				}
			}
		});
		button.setEnabled(isEditable());
	}
	private void createPluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {
		fPluginVersionEntry = new FormEntry(
				client,
				toolkit,
				PDEPlugin.getResourceString("GeneralInfoSection.pluginVersion"), null, false); //$NON-NLS-1$
		fPluginVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(entry
							.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fPluginVersionEntry.setEditable(isEditable());
	}
	private void createMatchCombo(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		fMatchLabel = toolkit.createLabel(client, PDEPlugin
				.getResourceString(KEY_MATCH));
		fMatchLabel.setForeground(toolkit.getColors()
				.getColor(FormColors.TITLE));
		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(client, toolkit, SWT.READ_ONLY);

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.grabExcessHorizontalSpace = true;
		fMatchCombo.getControl().setLayoutData(gd);
		String[] items = new String[]{"", //$NON-NLS-1$
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER)};
		fMatchCombo.setItems(items);
		fMatchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int match = fMatchCombo.getSelectionIndex();
				try {
					((IFragment) getPluginBase()).setRule(match);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fMatchCombo.getControl().setEnabled(isEditable());
	}
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fNameEntry.commit();
		fProviderEntry.commit();
		if (isFragment()) {
			fPluginVersionEntry.commit();
		} else {
			fClassEntry.commit();
		}
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IPluginBase) {
				String property = e.getChangedProperty();
				if (property != null
						&& property.equals(getPage().getPDEEditor()
								.getTitleProperty()))
					getPage().getPDEEditor().updateTitle();
			}
		}
	}

	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		IPluginBase pluginBase = model.getPluginBase();
		fIdEntry.setValue(pluginBase.getId(), true);
		fNameEntry.setValue(pluginBase.getName(), true);
		fVersionEntry.setValue(pluginBase.getVersion(), true);
		fProviderEntry.setValue(pluginBase.getProviderName(), true);
		if (isFragment()) {
			IFragment fragment = (IFragment) pluginBase;
			fPluginIdText.setText(fragment.getPluginId());
			fPluginVersionEntry.setValue(fragment.getPluginVersion(), true);
			if (!isBundleMode())
				fMatchCombo.select(fragment.getRule());
		} else {
			IPlugin plugin = (IPlugin) pluginBase;
			fClassEntry.setValue(plugin.getClassName(), true);
		}
		getPage().getPDEEditor().updateTitle();
		super.refresh();
	}
	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		fProviderEntry.cancelEdit();
		if (isFragment()) {
			fPluginVersionEntry.cancelEdit();
		} else {
			fClassEntry.cancelEdit();
		}
		super.cancelEdit();
	}
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		InputContextManager manager = getPage().getPDEEditor()
				.getContextManager();
		if (manager != null)
			manager.removeInputContextListener(this);
		super.dispose();
	}
	private void doOpenClass() {
		String name = fClassEntry.getText().getText();
		name = trimNonAlphaChars(name);
		IProject project = getPage().getPDEEditor().getCommonProject();
		String path = name.replace('.', '/') + ".java"; //$NON-NLS-1$
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = javaProject.findElement(new Path(path));
				JavaUI.openInEditor(result);
			} else {
				IResource resource = project.findMember(new Path(path));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				}
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void doOpenSelectionDialog() {
		try {
			Shell shell = PDEPlugin.getActiveWorkbenchShell();
			IResource resource = getPluginBase().getModel()
					.getUnderlyingResource();
			IProject project = (resource == null) ? null : resource
					.getProject();
			if (project != null) {
				SelectionDialog dialog = JavaUI.createTypeDialog(shell,
						PlatformUI.getWorkbench().getProgressService(),
						getSearchScope(project),
						IJavaElementSearchConstants.CONSIDER_CLASSES, false,
						""); //$NON-NLS-1$
				dialog.setTitle(PDEPlugin.getResourceString("GeneralInfoSection.selectionTitle")); //$NON-NLS-1$
				if (dialog.open() == SelectionDialog.OK) {
					IType type = (IType) dialog.getResult()[0];
					fClassEntry.setValue(type.getFullyQualifiedName('.'));
				}
			}
		} catch (CoreException e) {
		}
	}
	private IJavaSearchScope getSearchScope(IProject project) {
		IJavaProject jProject = JavaCore.create(project);
		return SearchEngine.createJavaSearchScope(getDirectRoots(jProject));
	}
	private IPackageFragmentRoot[] getDirectRoots(IJavaProject project) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result
				.toArray(new IPackageFragmentRoot[result.size()]);
	}
	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		return model.isFragmentModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextAdded(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			bundleModeChanged((IBundleModel) context.getModel(), true);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			bundleModeChanged((IBundleModel) context.getModel(), false);
	}
	private void bundleModeChanged(IBundleModel model, boolean added) {
		if (fMatchCombo != null) {
			fMatchLabel.setVisible(!added);
			fMatchCombo.getControl().setVisible(!added);
		}
	}
	private boolean isBundleMode() {
		InputContextManager icm = getPage().getPDEEditor().getContextManager();
		return icm.findContext(BundleInputContext.CONTEXT_ID) != null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return false;
	}
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
}
