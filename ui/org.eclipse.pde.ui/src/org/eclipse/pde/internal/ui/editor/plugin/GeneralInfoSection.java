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
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.*;

public class GeneralInfoSection extends PDESection {
	public static final String KEY_MATCH = "ManifestEditor.PluginSpecSection.versionMatch";
	public static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect";
	public static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent";
	public static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible";
	public static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater";
	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fNameEntry;
	private FormEntry fProviderEntry;
	private FormEntry fClassEntry;
	private Text fPluginIdText;
	private FormEntry fPluginVersionEntry;
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
				.getResourceString("ManifestEditor.PluginSpecSection.title"));
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		if (isFragment())
			section
					.setDescription(PDEPlugin
							.getResourceString("ManifestEditor.PluginSpecSection.fdesc"));
		else
			section
					.setDescription(PDEPlugin
							.getResourceString("ManifestEditor.PluginSpecSection.desc"));
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
		} else {
			createClassEntry(client, toolkit, actionBars);
		}
		toolkit.paintBordersFor(client);
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
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
		fIdEntry = new FormEntry(client, toolkit, "ID:", null, false);
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
		fVersionEntry = new FormEntry(client, toolkit, "Version:", null, false);
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
		fNameEntry = new FormEntry(client, toolkit, "Name:", null, false);
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
		fProviderEntry = new FormEntry(client, toolkit, "Provider:", null,
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
		fClassEntry = new FormEntry(client, toolkit, "Class:", "Browse...",
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
		int loc = value.indexOf(":");
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = "";
		return value;
	}
	private boolean doesClassExist(String className) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		String path = className.replace('.', '/') + ".java";
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);

				IJavaElement result = javaProject.findElement(new Path(path));
				return result != null;
			} else {
				IResource resource = project.findMember(new Path(path));
				return resource != null;
			}
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

		Hyperlink link = toolkit.createHyperlink(parent, "Plug-in Id:",
				SWT.NULL);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
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
		fPluginIdText = toolkit.createText(client, "", SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		fPluginIdText.setLayoutData(gd);

		fPluginIdText.setEditable(isEditable());
		fPluginIdText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				try {
					if (!((IFragment)getPluginBase()).getPluginId().equals(fPluginIdText.getText()))
						((IFragment)getPluginBase()).setPluginId(fPluginIdText.getText());
				} catch (CoreException e1) {
					PDEPlugin.logException(e1);
				}
			}
		});
		Button button = toolkit.createButton(client, "Browse...", SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PluginSelectionDialog dialog = new PluginSelectionDialog(
						getSection().getShell(), false, false);
				dialog.create();
				if (dialog.open() == PluginSelectionDialog.OK) {
					try {
					IPluginModel model = (IPluginModel) dialog.getFirstResult();
					IPlugin plugin = model.getPlugin();
					fPluginIdText.setText(plugin.getId());
					((IFragment)getPluginBase()).setPluginId(plugin.getId());
					fPluginVersionEntry.setValue(plugin.getVersion(), true);
					((IFragment)getPluginBase()).setPluginVersion(plugin.getVersion());
					} catch (CoreException e1){
						PDEPlugin.logException(e1);
					}
				}
			}
		});
		button.setEnabled(isEditable());
	}
	private void createPluginVersionEntry(Composite client,
			FormToolkit toolkit, IActionBars actionBars) {
		fPluginVersionEntry = new FormEntry(client, toolkit,
				"Plug-in Version:", null, false);
		fPluginVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IFragment) getPluginBase()).setPluginVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fPluginVersionEntry.setEditable(isEditable());
	}
	private void createMatchCombo(Composite client, FormToolkit toolkit,
			IActionBars actionBars) {
		Label label = toolkit.createLabel(client, PDEPlugin.getResourceString(KEY_MATCH));
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));		
		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(client, toolkit, SWT.READ_ONLY);
		
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.grabExcessHorizontalSpace = true;
		fMatchCombo.getControl().setLayoutData(gd);
		String[] items = new String[]{"",
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
			fMatchCombo.select(fragment.getRule());
		} else {
			IPlugin plugin = (IPlugin) pluginBase;
			fClassEntry.setValue(plugin.getClassName(), true);
		}
		super.refresh();
	}
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}
	private void doOpenClass() {
		String name = fClassEntry.getText().getText();
		name = trimNonAlphaChars(name);
		IProject project = getPage().getPDEEditor().getCommonProject();
		String path = name.replace('.', '/') + ".java";
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
						new ProgressMonitorDialog(shell),
						getSearchScope(project),
						IJavaElementSearchConstants.CONSIDER_CLASSES, false,
						"*");
				dialog.setTitle("Select Type");
				if (dialog.open() == SelectionDialog.OK) {
					IType type = (IType) dialog.getResult()[0];
					fClassEntry.setValue(type.getFullyQualifiedName());
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

}