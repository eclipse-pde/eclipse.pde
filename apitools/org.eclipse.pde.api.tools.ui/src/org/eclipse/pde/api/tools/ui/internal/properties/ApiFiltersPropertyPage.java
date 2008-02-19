/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.api.tools.internal.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import com.ibm.icu.text.MessageFormat;

/**
 * Property page to allow UI edits to the current set of filters for a given project
 * 
 * @since 1.0.0
 */
public class ApiFiltersPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	
	static class ApiKindDescription {
		String kind = null;
		String description = null;
		int[] types = null;
		
		public ApiKindDescription(String kind, String description, int[] types) {
			this.kind = kind;
			this.description = description;
			this.types = types;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.kind.hashCode();
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if(obj instanceof String) {
				return this.kind.equals(obj);
			}
			if(obj instanceof ApiKindDescription) {
				return this.kind.equals(((ApiKindDescription)obj).kind);
			}
			return false;
		}
		
		public boolean appliesTo(IElementDescriptor element) {
			int type = element.getElementType();
			for(int i = 0; i < types.length; i++) {
				if(types[i] == type) {
					return true;
				}
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return kind;
		}
	}
	
	private static final HashMap fgFilterDescriptions = new HashMap(); 
	
	static {
		//binary compatibility issues
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_CLASS_BOUND, new ApiKindDescription(IApiProblemFilter.ADDED_CLASS_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_0, 
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_INTERFACE_BOUND, new ApiKindDescription(IApiProblemFilter.ADDED_INTERFACE_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_1,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_INTERFACE_BOUNDS, new ApiKindDescription(IApiProblemFilter.ADDED_INTERFACE_BOUNDS, 
				PropertiesMessages.ApiFiltersPropertyPage_2,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_METHOD_WITHOUT_DEFAULT_VALUE, new ApiKindDescription(IApiProblemFilter.ADDED_METHOD_WITHOUT_DEFAULT_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_3,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_NO_EXTEND, new ApiKindDescription(IApiProblemFilter.ADDED_NO_EXTEND, 
				PropertiesMessages.ApiFiltersPropertyPage_4,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_NOT_IMPLEMENT_RESTRICTION, new ApiKindDescription(IApiProblemFilter.ADDED_NOT_IMPLEMENT_RESTRICTION, 
				PropertiesMessages.ApiFiltersPropertyPage_5,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_TYPE_PARAMETER, new ApiKindDescription(IApiProblemFilter.ADDED_TYPE_PARAMETER, 
				PropertiesMessages.ApiFiltersPropertyPage_6,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.ADDED_VALUE, new ApiKindDescription(IApiProblemFilter.ADDED_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_7,
				new int[] {IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_CLASS_BOUND, new ApiKindDescription(IApiProblemFilter.CHANGED_CLASS_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_8,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_CONTRACTED_SUPERCLASS_SET, new ApiKindDescription(IApiProblemFilter.CHANGED_CONTRACTED_SUPERCLASS_SET, 
				PropertiesMessages.ApiFiltersPropertyPage_9,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_CONTRACTED_SUPERINTERFACES_SET, new ApiKindDescription(IApiProblemFilter.CHANGED_CONTRACTED_SUPERINTERFACES_SET, 
				PropertiesMessages.ApiFiltersPropertyPage_10,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_DECREASE_ACCESS, new ApiKindDescription(IApiProblemFilter.CHANGED_DECREASE_ACCESS, 
				PropertiesMessages.ApiFiltersPropertyPage_11,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD, IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT, new ApiKindDescription(IApiProblemFilter.CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT, 
				PropertiesMessages.ApiFiltersPropertyPage_12,
				new int[] {IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_INTERFACE_BOUND, new ApiKindDescription(IApiProblemFilter.CHANGED_INTERFACE_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_13,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_NON_ABSTRACT_TO_ABSTRACT, new ApiKindDescription(IApiProblemFilter.CHANGED_NON_ABSTRACT_TO_ABSTRACT, 
				PropertiesMessages.ApiFiltersPropertyPage_14,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_NON_FINAL_TO_FINAL, new ApiKindDescription(IApiProblemFilter.CHANGED_NON_FINAL_TO_FINAL, 
				PropertiesMessages.ApiFiltersPropertyPage_15,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD, IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_NON_STATIC_TO_STATIC, new ApiKindDescription(IApiProblemFilter.CHANGED_NON_STATIC_TO_STATIC, 
				PropertiesMessages.ApiFiltersPropertyPage_16,
				new int[] {IElementDescriptor.T_FIELD, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_STATIC_TO_NON_STATIC, new ApiKindDescription(IApiProblemFilter.CHANGED_STATIC_TO_NON_STATIC, 
				PropertiesMessages.ApiFiltersPropertyPage_17,
				new int[] {IElementDescriptor.T_FIELD, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_SUPERCLASS, new ApiKindDescription(IApiProblemFilter.CHANGED_SUPERCLASS, 
				PropertiesMessages.ApiFiltersPropertyPage_18,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_TO_ANNOTATION, new ApiKindDescription(IApiProblemFilter.CHANGED_TO_ANNOTATION, 
				PropertiesMessages.ApiFiltersPropertyPage_19,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_TO_CLASS, new ApiKindDescription(IApiProblemFilter.CHANGED_TO_CLASS, 
				PropertiesMessages.ApiFiltersPropertyPage_20,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_TO_ENUM, new ApiKindDescription(IApiProblemFilter.CHANGED_TO_ENUM, 
				PropertiesMessages.ApiFiltersPropertyPage_21,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_TO_INTERFACE, new ApiKindDescription(IApiProblemFilter.CHANGED_TO_INTERFACE, 
				PropertiesMessages.ApiFiltersPropertyPage_22,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_TYPE, new ApiKindDescription(IApiProblemFilter.CHANGED_TYPE, 
				PropertiesMessages.ApiFiltersPropertyPage_23,
				new int[] {IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_VALUE, new ApiKindDescription(IApiProblemFilter.CHANGED_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_24,
				new int[] {IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.CHANGED_VARARGS_TO_ARRAY, new ApiKindDescription(IApiProblemFilter.CHANGED_VARARGS_TO_ARRAY, 
				PropertiesMessages.ApiFiltersPropertyPage_25,
				new int[] {IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_ANNOTATION_DEFAULT_VALUE, new ApiKindDescription(IApiProblemFilter.REMOVED_ANNOTATION_DEFAULT_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_26,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_API_COMPONENT, new ApiKindDescription(IApiProblemFilter.REMOVED_API_COMPONENT, 
				PropertiesMessages.ApiFiltersPropertyPage_27,
				new int[] {IDelta.API_COMPONENT_ELEMENT_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_CLASS_BOUND, new ApiKindDescription(IApiProblemFilter.REMOVED_CLASS_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_28,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_CONSTRUCTOR, new ApiKindDescription(IApiProblemFilter.REMOVED_CONSTRUCTOR, 
				PropertiesMessages.ApiFiltersPropertyPage_29,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_ENUM_CONSTANT, new ApiKindDescription(IApiProblemFilter.REMOVED_ENUM_CONSTANT, 
				PropertiesMessages.ApiFiltersPropertyPage_30,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_FIELD, new ApiKindDescription(IApiProblemFilter.REMOVED_FIELD,
				PropertiesMessages.ApiFiltersPropertyPage_31,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_INTERFACE_BOUND, new ApiKindDescription(IApiProblemFilter.REMOVED_INTERFACE_BOUND, 
				PropertiesMessages.ApiFiltersPropertyPage_32,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_INTERFACE_BOUNDS, new ApiKindDescription(IApiProblemFilter.REMOVED_INTERFACE_BOUNDS, 
				PropertiesMessages.ApiFiltersPropertyPage_33,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_METHOD, new ApiKindDescription(IApiProblemFilter.REMOVED_METHOD, 
				PropertiesMessages.ApiFiltersPropertyPage_34,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_METHOD_WITH_DEFAULT_VALUE, new ApiKindDescription(IApiProblemFilter.REMOVED_METHOD_WITH_DEFAULT_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_35,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_METHOD_WITHOUT_DEFAULT_VALUE, new ApiKindDescription(IApiProblemFilter.REMOVED_METHOD_WITHOUT_DEFAULT_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_36,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_TYPE, new ApiKindDescription(IApiProblemFilter.REMOVED_TYPE, 
				PropertiesMessages.ApiFiltersPropertyPage_37,
				new int[] {IDelta.API_COMPONENT_ELEMENT_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_TYPE_ARGUMENTS, new ApiKindDescription(IApiProblemFilter.REMOVED_TYPE_ARGUMENTS, 
				PropertiesMessages.ApiFiltersPropertyPage_38,
				new int[] {IElementDescriptor.T_FIELD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_TYPE_MEMBER, new ApiKindDescription(IApiProblemFilter.REMOVED_TYPE_MEMBER, 
				PropertiesMessages.ApiFiltersPropertyPage_39,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_TYPE_PARAMETER, new ApiKindDescription(IApiProblemFilter.REMOVED_TYPE_PARAMETER, 
				PropertiesMessages.ApiFiltersPropertyPage_40,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_TYPE_PARAMETERS, new ApiKindDescription(IApiProblemFilter.REMOVED_TYPE_PARAMETERS, 
				PropertiesMessages.ApiFiltersPropertyPage_41,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		fgFilterDescriptions.put(IApiProblemFilter.REMOVED_VALUE, new ApiKindDescription(IApiProblemFilter.REMOVED_VALUE, 
				PropertiesMessages.ApiFiltersPropertyPage_42,
				new int[] {IElementDescriptor.T_FIELD}));
		
		//usage restriction issues
		String temp = Util.getReferenceKind(ReferenceModifiers.REF_EXTENDS);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_43,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE, IElementDescriptor.T_METHOD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_IMPLEMENTS);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_44,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_INSTANTIATE);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_45,
				new int[] {IElementDescriptor.T_REFERENCE_TYPE}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_OVERRIDE);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_46,
				new int[] {IElementDescriptor.T_METHOD, IElementDescriptor.T_REFERENCE_TYPE}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_GETFIELD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_47,
				new int[] {IElementDescriptor.T_FIELD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_GETSTATIC);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_48,
				new int[] {IElementDescriptor.T_FIELD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_PUTFIELD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_49,
				new int[] {IElementDescriptor.T_FIELD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_PUTSTATIC);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_50,
				new int[] {IElementDescriptor.T_FIELD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_INTERFACEMETHOD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_51,
				new int[] {IElementDescriptor.T_METHOD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_SPECIALMETHOD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_52,
				new int[] {IElementDescriptor.T_METHOD}));
		temp =  Util.getReferenceKind(ReferenceModifiers.REF_STATICMETHOD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp,
				PropertiesMessages.ApiFiltersPropertyPage_53,
				new int[] {IElementDescriptor.T_METHOD}));
		temp = Util.getReferenceKind(ReferenceModifiers.REF_VIRTUALMETHOD);
		fgFilterDescriptions.put(temp, new ApiKindDescription(temp, 
				PropertiesMessages.ApiFiltersPropertyPage_54,
				new int[] {IElementDescriptor.T_METHOD}));
		
		//version number problem kinds
		fgFilterDescriptions.put(IApiProblemFilter.MINOR_VERSION_CHANGE, new ApiKindDescription(IApiProblemFilter.MINOR_VERSION_CHANGE,
				PropertiesMessages.ApiFiltersPropertyPage_60,
				new int[] {IElementDescriptor.T_RESOURCE}));
		fgFilterDescriptions.put(IApiProblemFilter.MAJOR_VERSION_CHANGE, new ApiKindDescription(IApiProblemFilter.MAJOR_VERSION_CHANGE,
				PropertiesMessages.ApiFiltersPropertyPage_61,
				new int[] {IElementDescriptor.T_RESOURCE}));
	}
	
	/**
	 * Describes a reversible change to the listing of filters
	 */
	class Change {
		static final int REMOVE = 1;
		static final int CHANGE = 2;
		IApiProblemFilter filter = null;
		int type = 0;
		public Change(IApiProblemFilter filter, int type) {
			this.filter = filter;
			this.type = type;
		}
	}
	
	/**
	 * Comparator for the viewer to group filters by {@link IElementDescriptor} type
	 */
	class ApiFilterComparator extends WorkbenchViewerComparator {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
		 */
		public int category(Object element) {
			if(element instanceof IApiProblemFilter) {
				return ((IApiProblemFilter) element).getElement().getElementType();
			}
			return -1;
		}
	}
	
	/**
	 * Label provider for the viewer
	 */
	class FileStoreLabelProvider extends LabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if(element instanceof IApiProblemFilter) {
				IApiProblemFilter filter = (IApiProblemFilter) element;
				IElementDescriptor desc = filter.getElement();
				ISharedImages images = JavaUI.getSharedImages();
				switch(desc.getElementType()) {
					case IElementDescriptor.T_PACKAGE: {
						return images.getImage(ISharedImages.IMG_OBJS_PACKAGE);
					}
					case IElementDescriptor.T_REFERENCE_TYPE: {
						return images.getImage(ISharedImages.IMG_OBJS_CLASS);
					}
					case IElementDescriptor.T_METHOD: {
						return images.getImage(ISharedImages.IMG_OBJS_PUBLIC);
					}
					case IElementDescriptor.T_FIELD: {
						return images.getImage(ISharedImages.IMG_FIELD_PUBLIC);
					}
					case IElementDescriptor.T_RESOURCE: {
						IResourceDescriptor rdesc = ((IResourceDescriptor)desc);
						if(rdesc.getResourceType() == IResource.FOLDER) {
							return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER);
						}
						return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FILE);
					}
				}
			}
			return super.getImage(element);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if(element instanceof IApiProblemFilter) {
				IApiProblemFilter filter = (IApiProblemFilter) element;
				String name = Util.getFormattedFilterName(filter);
				if(name != null) {
					return name;
				}
			}
			return super.getText(element);
		}
	}
	
	private TableViewer fViewer = null;
	private Button fRemoveButton, fEditButton;
	private IProject fProject = null;
	private ArrayList fChangeset = new ArrayList();
	private ArrayList fInputset = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(comp, PropertiesMessages.ApiFiltersPropertyPage_55, 2);
		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		table.setLayoutData(gd);
		fViewer = new TableViewer(table);
		fViewer.setContentProvider(new ArrayContentProvider());
		fViewer.setLabelProvider(new FileStoreLabelProvider());
		fViewer.setComparator(new ApiFilterComparator());
		try {
			fInputset = new ArrayList(Arrays.asList(getFilterStore().getFilters()));
			fViewer.setInput(fInputset);
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		}
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				fRemoveButton.setEnabled(ss.size() > 0);
				fEditButton.setEnabled(ss.size() == 1);
			}
		});
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				if(ss.size() == 1) {
					IApiProblemFilter filter = (IApiProblemFilter) ss.getFirstElement();
					handleEdit(filter);
				}
			}
		});
		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		fEditButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.ApiFiltersPropertyPage_56, null, SWT.LEFT);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit(getSelectedFilter());
			}
		});
		fEditButton.setEnabled(false);
		fRemoveButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.ApiFiltersPropertyPage_57, null, SWT.LEFT);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection) fViewer.getSelection();
				IApiProblemFilter filter = (IApiProblemFilter) ss.getFirstElement();
				Change change = new Change(filter, Change.REMOVE);
				fChangeset.add(change);
				fInputset.remove(filter);
				fViewer.refresh();
			}
		});
		fRemoveButton.setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_FILTERS_PROPERTY_PAGE);
		return comp;
	}	
	
	/**
	 * Returns the API kind description for the given kind, or <code>null</code>
	 * @param kind
	 * @return the kind description or <code>null</code>
	 */
	public static ApiKindDescription getDescription(String kind) {
		return (ApiKindDescription) fgFilterDescriptions.get(kind);
	}
	
	/**
	 * @return all registered {@link ApiKindDescription}s
	 */
	public static Collection getAllKindDescriptions() {
		return fgFilterDescriptions.values();
	}
	
	/**
	 * Returns the {@link IApiProblemFilter} currently selected in the viewer
	 * @return the selected {@link IApiProblemFilter}
	 */
	private IApiProblemFilter getSelectedFilter() {
		IStructuredSelection ss = (IStructuredSelection) fViewer.getSelection();
		if(ss.size() == 1) {
			return (IApiProblemFilter) ss.getFirstElement();
		}
		return null;
	}
	
	/**
	 * Handles the edit button being pressed
	 * @param filter the selected filter to edit
	 */
	private void handleEdit(IApiProblemFilter filter) {
		EditApiFilterDialog dialog = new EditApiFilterDialog(getShell(), (IApiProblemFilter)((ApiProblemFilter)filter).clone());
		if(dialog.open() == IDialogConstants.OK_ID) {
			//do change
			IApiProblemFilter editedfilter =  dialog.getFilter();
			//update the viewer
			fChangeset.add(new Change(filter, Change.REMOVE));
			fInputset.remove(filter);
			fChangeset.add(new Change(editedfilter, Change.CHANGE));
			fInputset.add(editedfilter);
			fViewer.refresh();
		}
	}
	
	/**
	 * @return the backing project for this page, or <code>null</code> if this page was 
	 * somehow opened without a project
	 */
	private IProject getProject() {
		if(fProject == null) {
			fProject = (IProject) getElement().getAdapter(IProject.class);
		}
		return fProject;
	}
	
	/**
	 * @return the {@link IApiFilterStore} from the backing project
	 * @throws CoreException
	 */
	private IApiFilterStore getFilterStore() throws CoreException {
		IProject project  = getProject();
		IApiFilterStore store = null;
		if(project != null) {
			IApiComponent component = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile().getApiComponent(project.getName());
			if(component != null) {
				return component.getFilterStore();
			}
		}
		return store;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		super.performApply();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		try {
			Change change = null;
			IApiFilterStore store = getFilterStore();
			for(int i = 0; i < fChangeset.size(); i++) {
				change = (Change) fChangeset.get(i);
				if(change.type == Change.REMOVE) {
					store.removeFilter(change.filter);
				}
				else {
					store.addFilter(change.filter);
				}
			}
			if(fChangeset.size() > 0) {
				//TODO we need to incremental build to ensure new filter kinds are enacted and removed kinds show the markers
				if(MessageDialog.openQuestion(getShell(), PropertiesMessages.ApiFiltersPropertyPage_58, 
						MessageFormat.format(PropertiesMessages.ApiFiltersPropertyPage_59, new String[] {fProject.getName()}))) {
					Util.getBuildJob(fProject).schedule();
				}
			}
			fChangeset.clear();
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		}
		return super.performOk();
	}
}
