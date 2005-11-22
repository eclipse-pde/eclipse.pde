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
package org.eclipse.pde.internal.ui.editor.schema;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.schema.ChoiceRestriction;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaEnumeration;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaAttributeDetails extends AbstractSchemaDetails {
	
	private static final String JAVA_TYPE = "java"; //$NON-NLS-1$
	private static final String RESOURCE_TYPE = "resource"; //$NON-NLS-1$
	private static final int BOOL_IND = 0;
	private static final int STR_IND = 1;
	private static final int JAVA_IND = 2;
	private static final int RES_IND = 3;
	private static final String[] TYPES = new String[4];
	static {
		TYPES[BOOL_IND]= BOOLEAN_TYPE;
		TYPES[STR_IND] = STRING_TYPE;
		TYPES[JAVA_IND] = JAVA_TYPE;
		TYPES[RES_IND] = RESOURCE_TYPE;
	}
	private static final String[] USE = 
		new String[] {"optional", "required", "default"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	private SchemaAttribute fAttribute;
	private FormEntry fValue;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private ComboPart fTranslatable;
	private ComboPart fType;
	private ComboPart fUse;
	private TableViewer fRestrictionsTable;
	private FormEntry fClassEntry;
	private FormEntry fInterfaceEntry;
	private Button fAddRestriction;
	private Button fRemoveRestriction;
	private Label fResLabel;
	private Label fTransLabel;
	
	public SchemaAttributeDetails(ISchemaAttribute attribute, ElementSection section) {
		super(section, false);
		fAttribute = (SchemaAttribute)attribute;
	}

	class SchemaAttributeLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return super.getText(element);
		}
	}
	class SchemaAttributeContentProvider extends DefaultTableProvider {
		
		public Object[] getElements(Object inputElement) {
			ISchemaSimpleType type = fAttribute.getType();
			ISchemaRestriction restriction = type.getRestriction();
			if (restriction != null)
				return restriction.getChildren();
			return new Object[0];
		}
	}
	
	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);
		
		toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated).setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);
		
		toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_use).setForeground(foreground);
		fUse = createComboPart(parent, toolkit, USE, 2);
		
		fValue = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_defaultValue, null, false, 6);
		fValue.setDimLabel(true);
		
		toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_type).setForeground(foreground);
		fType = createComboPart(parent, toolkit, TYPES, 2);
		
		fTransLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_translatable);
		fTransLabel.setForeground(foreground);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 6;
		gd.verticalIndent = 2;
		fTransLabel.setLayoutData(gd);
		fTranslatable = createComboPart(parent, toolkit, BOOLS, 2);
		
		fResLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_restrictions);
		fResLabel.setForeground(foreground);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 6;
		gd.verticalIndent = 2;
		fResLabel.setLayoutData(gd);

		Composite tableComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(); layout.marginHeight = layout.marginWidth = 0;
		tableComp.setLayout(layout);
		tableComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Table table = toolkit.createTable(tableComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		table.setLayoutData(gd);
		fRestrictionsTable = new TableViewer(table);
		fRestrictionsTable.setContentProvider(new SchemaAttributeContentProvider());
		fRestrictionsTable.setLabelProvider(new SchemaAttributeLabelProvider());
		fRestrictionsTable.setInput(new Object());
		
		Composite resButtonComp = toolkit.createComposite(parent);
		layout = new GridLayout(); layout.marginHeight = layout.marginWidth = 0;
		resButtonComp.setLayout(layout);
		resButtonComp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fAddRestriction = toolkit.createButton(resButtonComp, PDEUIMessages.SchemaAttributeDetails_addRestButton, SWT.NONE);
		fRemoveRestriction = toolkit.createButton(resButtonComp, PDEUIMessages.SchemaAttributeDetails_removeRestButton, SWT.NONE);
		fAddRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fClassEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_extends, PDEUIMessages.SchemaAttributeDetails_browseButton, true, 6);
		fInterfaceEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_implements, PDEUIMessages.SchemaAttributeDetails_browseButton, true, 6);
		
		setText(PDEUIMessages.SchemaAttributeDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaAttributeDetails_description, fAttribute.getName()));
	}

	public void updateFields() {
		if (fAttribute == null)
			return;
		String curr = fAttribute.getName();
		fName.setValue(curr != null ? curr : ""); //$NON-NLS-1$
		
		fDeprecated.select(fAttribute.isDeprecated() ? 0 : 1);
		fTranslatable.select(fAttribute.isTranslatable() ? 0 : 1);
		
		boolean isStringType = fAttribute.getType().getName().equals(STRING_TYPE);
		int kind = fAttribute.getKind();
		fType.select(isStringType ? 1 + kind : 0);
		
		fUse.select(fAttribute.getUse());
		Object value = fAttribute.getValue();
		fValue.setEditable(fAttribute.getUse() == 2);
		fValue.setValue(value != null ? value.toString() : ""); //$NON-NLS-1$
		
		if (kind == IMetaAttribute.JAVA) {
			curr = fAttribute.getBasedOn();
			if (curr != null && curr.length() > 0) {
				int index = curr.indexOf(":"); //$NON-NLS-1$
				if (index == -1) {
					String className = curr.substring(curr.lastIndexOf(".") + 1); //$NON-NLS-1$
					if (className.length() > 1 && className.charAt(0) == 'I')
						fInterfaceEntry.setValue(curr);
					else
						fClassEntry.setValue(curr);
				} else {
					fClassEntry.setValue(curr.substring(0, index));
					fInterfaceEntry.setValue(curr.substring(index + 1));
				}
			}
		}
		updateTypeParts(isStringType && kind == IMetaAttribute.STRING, kind == IMetaAttribute.JAVA);
	}

	public void hookListeners() {
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fValue.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fAttribute.setValue(fValue.getValue());
			}
		});
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fAttribute.setName(fName.getValue());
				setDecription(NLS.bind(PDEUIMessages.SchemaAttributeDetails_description, fAttribute.getName()));
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAttribute.setDeprecatedProperty(fDeprecated.getSelection().equals(BOOLS[0]));
			}
		});
		fTranslatable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAttribute.setTranslatableProperty(fTranslatable.getSelection().equals(BOOLS[0]));
			}
		});
		fType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String typeString = fType.getSelection();
				if (!typeString.equals(BOOLEAN_TYPE))
					typeString = STRING_TYPE;
				
				fAttribute.setType(new SchemaSimpleType(fAttribute.getSchema(), typeString));
				
				int kind = fType.getSelectionIndex() - 1; // adjust for "boolean" in combo
				fAttribute.setKind(kind > 0 ? kind : 0); // kind could be -1
				
				ISchemaSimpleType type = fAttribute.getType();
				if (type instanceof SchemaSimpleType
						&& kind != IMetaAttribute.STRING
						&& ((SchemaSimpleType) type).getRestriction() != null) {
					((SchemaSimpleType) type).setRestriction(null);
				}
				updateTypeParts(kind == IMetaAttribute.STRING, kind == IMetaAttribute.JAVA);
			}
		});
		fUse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int use = fUse.getSelectionIndex();
				fAttribute.setUse(use);
				fValue.setEditable(use == 2);
				if (use == 2 && fValue.getValue().length() == 0) {
					fValue.setValue(PDEUIMessages.SchemaAttributeDetails_defaultDefaultValue);
					fValue.getText().setSelection(0, fValue.getValue().length());
					fValue.getText().setFocus();
				} else if (use != 2)
					fValue.setValue(""); //$NON-NLS-1$
				
			}
		});
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				setBasedOn();
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fClassEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fClassEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_CLASSES, fClassEntry);
			}
		});
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				setBasedOn();
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, true);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_INTERFACES, fInterfaceEntry);
			}
		});
		fAddRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewRestrictionDialog dialog = new NewRestrictionDialog(getPage().getSite().getShell());
				if (dialog.open() != Window.OK) return;
				String text = dialog.getNewRestriction();
				if (text != null && text.length() > 0) {
					ISchemaSimpleType type = fAttribute.getType();
					ChoiceRestriction res = (ChoiceRestriction)type.getRestriction();
					Vector vres = new Vector();
					if (res != null)  {
						Object[] currRes = res.getChildren();
						for (int i = 0; i < currRes.length; i++) {
							vres.add(currRes[i]);
						}
					}
					vres.add(new SchemaEnumeration(fAttribute.getSchema(), text));
					if (res == null)
						res = new ChoiceRestriction(fAttribute.getSchema());
					res.setChildren(vres);
					if (type instanceof SchemaSimpleType)
							((SchemaSimpleType)type).setRestriction(res);
					fRestrictionsTable.refresh();
				}
			}
		});
		fRemoveRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = fRestrictionsTable.getSelection();
				if (selection.isEmpty()) return;
				if (!(selection instanceof StructuredSelection)) return;
				StructuredSelection sselection = (StructuredSelection)selection;
				Object[] aselection = sselection.toArray();
				ISchemaSimpleType type = fAttribute.getType();
				ChoiceRestriction res = (ChoiceRestriction)type.getRestriction();
				Vector vres = new Vector();
				if (res != null)  {
					Object[] currRes = res.getChildren();
					for (int i = 0; i < currRes.length; i++) {
						boolean stays = true;
						for (int j = 0; j < aselection.length; j++) {
							if (currRes[i].equals(aselection[j]))
								stays = false;
						}
						if (stays) vres.add(currRes[i]);
					}
					res.setChildren(vres);
					if (type instanceof SchemaSimpleType)
						((SchemaSimpleType)type).setRestriction(res);
					fRestrictionsTable.refresh();
				}
			}
		});
	}
	
	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$', '.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					NewClassCreationWizard wizard = new NewClassCreationWizard(project, isInter);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == WizardDialog.OK) {
						return wizard.getClassName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}
	
	private void setBasedOn() {
		String classEntry = fClassEntry.getValue();
		String interfaceEntry = fInterfaceEntry.getValue();
		StringBuffer sb = new StringBuffer();
		if (classEntry.length() > 0)
			sb.append(classEntry);
		if (classEntry.length() > 0 && interfaceEntry.length() > 0)
			sb.append(":"); //$NON-NLS-1$
		if (interfaceEntry.length() > 0)
			sb.append(interfaceEntry);
		fAttribute.setBasedOn(sb.length() > 0 ? sb.toString() : null);
	}
	
	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			String filter = entry.getValue();
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					PlatformUI.getWorkbench().getProgressService(),
					SearchEngine.createWorkspaceScope(), scopeType, false, filter); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.GeneralInfoSection_selectionTitle); 
			if (dialog.open() == SelectionDialog.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
			}
		} catch (CoreException e) {
		}
	}
	
	private void updateTypeParts(boolean isStringKind, boolean isJavaKind) {
		fTranslatable.getControl().setEnabled(isStringKind);
		fTranslatable.getControl().setVisible(isStringKind || isJavaKind);
		fTransLabel.setEnabled(isStringKind);
		fTransLabel.setVisible(isStringKind || isJavaKind);
		fResLabel.setEnabled(isStringKind);
		fResLabel.setVisible(isStringKind || isJavaKind);
		fRestrictionsTable.getControl().setVisible(isStringKind || isJavaKind);
		fRestrictionsTable.getControl().setEnabled(isStringKind);
		fAddRestriction.setVisible(isStringKind || isJavaKind);
		fAddRestriction.setEnabled(isStringKind);
		fRemoveRestriction.setVisible(isStringKind || isJavaKind);
		fRemoveRestriction.setEnabled(isStringKind);
		fRestrictionsTable.refresh();
		fInterfaceEntry.setVisible(isJavaKind);
		fInterfaceEntry.setEditable(isJavaKind);
		fClassEntry.setVisible(isJavaKind);
		fClassEntry.setEditable(isJavaKind);
	}
}
