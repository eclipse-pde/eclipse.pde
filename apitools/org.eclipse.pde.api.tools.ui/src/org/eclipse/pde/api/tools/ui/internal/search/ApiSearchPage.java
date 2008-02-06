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
package org.eclipse.pde.api.tools.ui.internal.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.PatternStrings;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;


public class ApiSearchPage extends DialogPage implements ISearchPage {

    private static final int HISTORY_SIZE= 12;

	// Dialog store id constants
	private static final String PAGE_NAME= "ApiSearchPage"; //$NON-NLS-1$
	private static final String STORE_CASE_SENSITIVE= "CASE_SENSITIVE"; //$NON-NLS-1$
	private static final String STORE_IS_REG_EX_SEARCH= "REG_EX_SEARCH"; //$NON-NLS-1$
	private static final String STORE_SEARCH_DERIVED = "SEARCH_DERIVED"; //$NON-NLS-1$
	private static final String STORE_HISTORY= "HISTORY"; //$NON-NLS-1$
	private static final String STORE_HISTORY_SIZE= "HISTORY_SIZE"; //$NON-NLS-1$

	private List fPreviousSearchPatterns= new ArrayList(20);

	private boolean fFirstTime= true;
	private boolean fIsCaseSensitive;
	private boolean fIsRegExSearch;
	private boolean fSearchDerived;
	
	private Combo fPattern;
	private Button fCaseSensitive;
	private Button fIsRegExCheckbox;
	private CLabel fStatusLabel;
	
	private Button[] fSearchFor;
	private Button[] fLimitTo;
	private Group fLimitToGroup;
	private SearchPatternData fInitialData;
	private IJavaElement fJavaElement;
	private IApiProfile[] fProfiles;
	private Combo fScope;
	
	// limit to
	private final static int IMPLEMENTORS= 1;
	private final static int REFERENCES= 2;
	private final static int READ_ACCESSES= 3;
	private final static int WRITE_ACCESSES= 4;
	private final static int INSTANTIATIONS = 5;
	private final static int SUBTYPES = 6;
	private final static int OVERRIDE = 7;

	private ISearchPageContainer fContainer;
    
	private static class SearchPatternData {
		private int searchFor;
		private int limitTo;
		private String pattern;
		private boolean isCaseSensitive;
		private boolean isRegEx;
		private IJavaElement javaElement;
		private int scope;
		private IWorkingSet[] workingSets;
		
		public SearchPatternData(int searchFor, int limitTo, boolean isCaseSensitive, boolean isRegEx, String pattern, IJavaElement element) {
			this(searchFor, limitTo, pattern, isCaseSensitive, isRegEx, element, ISearchPageContainer.WORKSPACE_SCOPE, null);
		}
		
		public SearchPatternData(int searchFor, int limitTo, String pattern, boolean isCaseSensitive, boolean regEx, IJavaElement element, int scope, IWorkingSet[] workingSets) {
			this.searchFor= searchFor;
			this.limitTo= limitTo;
			this.pattern= pattern;
			this.isCaseSensitive= isCaseSensitive;
			this.scope= scope;
			this.workingSets= workingSets;
			this.isRegEx= regEx;
			setJavaElement(element);
		}
		
		public void setJavaElement(IJavaElement javaElement) {
			this.javaElement= javaElement;
		}

		public boolean isCaseSensitive() {
			return isCaseSensitive;
		}
		
		public boolean isRegEx() {
			return isRegEx;
		}

		public IJavaElement getJavaElement() {
			return javaElement;
		}

		public int getLimitTo() {
			return limitTo;
		}

		public String getPattern() {
			return pattern;
		}

		public int getScope() {
			return scope;
		}

		public int getSearchFor() {
			return searchFor;
		}

		public IWorkingSet[] getWorkingSets() {
			return workingSets;
		}
		
		public void store(IDialogSettings settings) {
			settings.put("searchFor", searchFor); //$NON-NLS-1$
			settings.put("scope", scope); //$NON-NLS-1$
			settings.put("pattern", pattern); //$NON-NLS-1$
			settings.put("limitTo", limitTo); //$NON-NLS-1$
			settings.put("javaElement", javaElement != null ? javaElement.getHandleIdentifier() : ""); //$NON-NLS-1$ //$NON-NLS-2$
			settings.put("isCaseSensitive", isCaseSensitive); //$NON-NLS-1$
			settings.put("isRegEx", isRegEx); //$NON-NLS-1$
			if (workingSets != null) {
				String[] wsIds= new String[workingSets.length];
				for (int i= 0; i < workingSets.length; i++) {
					wsIds[i]= workingSets[i].getName();
				}
				settings.put("workingSets", wsIds); //$NON-NLS-1$
			} else {
				settings.put("workingSets", new String[0]); //$NON-NLS-1$
			}
		}
		
		public static SearchPatternData create(IDialogSettings settings) {
			String pattern= settings.get("pattern"); //$NON-NLS-1$
			if (pattern.length() == 0) {
				return null;
			}
			IJavaElement elem= null;
			String handleId= settings.get("javaElement"); //$NON-NLS-1$
			if (handleId != null && handleId.length() > 0) {
				IJavaElement restored= JavaCore.create(handleId); 
				if (restored != null && isSearchableType(restored) && restored.exists()) {
					elem= restored;
				}
			}
			String[] wsIds= settings.getArray("workingSets"); //$NON-NLS-1$
			IWorkingSet[] workingSets= null;
			if (wsIds != null && wsIds.length > 0) {
				IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
				workingSets= new IWorkingSet[wsIds.length];
				for (int i= 0; workingSets != null && i < wsIds.length; i++) {
					workingSets[i]= workingSetManager.getWorkingSet(wsIds[i]);
					if (workingSets[i] == null) {
						workingSets= null;
					}
				}
			}

			try {
				int searchFor= settings.getInt("searchFor"); //$NON-NLS-1$
				int scope= settings.getInt("scope"); //$NON-NLS-1$
				int limitTo= settings.getInt("limitTo"); //$NON-NLS-1$
				
				boolean isCaseSensitive= settings.getBoolean("isCaseSensitive"); //$NON-NLS-1$
				boolean isRegEx = settings.getBoolean("isRegEx"); //$NON-NLS-1$
				return new SearchPatternData(searchFor, limitTo, pattern, isCaseSensitive, isRegEx, elem, scope, workingSets);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
	}
	
	final static boolean isSearchableType(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.PACKAGE_DECLARATION:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
				return true;
		}
		return false;
	}	
	
	//---- Action Handling ------------------------------------------------
	
	private ISearchQuery newQuery() throws CoreException {
		
		// scope
		int index = fScope.getSelectionIndex();
		IApiProfile profile = null;
		if (index == -1) {
			profile = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		} else {
			profile = fProfiles[index];
		}
		
		// criteria
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setConsiderComponentLocalReferences(false);
		IApiComponent[] apiComponents = null;
		if (fJavaElement instanceof IType) {
			IType type = (IType) fJavaElement;
			IReferenceTypeDescriptor descriptor = Factory.typeDescriptor(type.getFullyQualifiedName('$'));
			criteria.addElementRestriction(fJavaElement.getJavaProject().getElementName(), 
					new IElementDescriptor[]{descriptor});
			IApiComponent root = profile.getApiComponent(fJavaElement.getJavaProject().getElementName());
			if (root != null) {
				apiComponents = profile.getDependentComponents(new IApiComponent[]{root});
			}
		} else {
			criteria.addPatternRestriction(getPattern(), getSearchFor());
		}
		
		if (apiComponents == null) {
			apiComponents = profile.getApiComponents();
		}
		List comps = new ArrayList(apiComponents.length);
		for (int i = 0; i < apiComponents.length; i++) {
			IApiComponent apiComponent = apiComponents[i];
			if (!apiComponent.isSystemComponent()) {
				comps.add(apiComponent);
			}
		}
		IApiSearchScope scope = Factory.newScope((IApiComponent[]) comps.toArray(new IApiComponent[comps.size()]));
		
		int refKinds = 0;
		String label = null;
		switch (getLimitTo()) {
			case IMPLEMENTORS:
				label = "Implementors";
				refKinds = ReferenceModifiers.REF_IMPLEMENTS;
				break;
			case INSTANTIATIONS:
				label = "Instantiations";
				refKinds = ReferenceModifiers.REF_INSTANTIATE;
				break;
			case SUBTYPES:
				label = "Subclasses";
				refKinds = ReferenceModifiers.REF_EXTENDS;
				break;
			case OVERRIDE:
				label = "Overrides";
				refKinds = ReferenceModifiers.REF_OVERRIDE;
				break;	
			case WRITE_ACCESSES:
				label = "Write Accesses";
				refKinds = ReferenceModifiers.REF_PUTFIELD | ReferenceModifiers.REF_PUTSTATIC;
				break;				
			case READ_ACCESSES:
				label = "Read Accesses";
				refKinds = ReferenceModifiers.REF_GETFIELD | ReferenceModifiers.REF_GETSTATIC;
				break;
			case REFERENCES:
				label = "All References";
				refKinds = ReferenceModifiers.MASK_REF_ALL;
				break;
		}
		criteria.setReferenceKinds(refKinds, VisibilityModifiers.ALL_VISIBILITIES, RestrictionModifiers.ALL_RESTRICTIONS);
		return new ApiSearchQuery(label, scope, criteria);
	}
	
	public boolean performAction() {
		try {
			NewSearchUI.runQueryInBackground(newQuery());
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), null, null, e.getStatus());
			return false;
		}
 		return true;
	}


	private String getPattern() {
		return fPattern.getText();
	}
	
	private SearchPatternData findInPrevious(String pattern) {
		for (Iterator iter= fPreviousSearchPatterns.iterator(); iter.hasNext();) {
			SearchPatternData element= (SearchPatternData) iter.next();
			if (pattern.equals(element.getPattern())) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Return search pattern data and update previous searches.
	 * An existing entry will be updated.
	 * @return the pattern data
	 */
	private SearchPatternData getPatternData() {
		String pattern= getPattern();
		SearchPatternData match= findInPrevious(pattern);
		if (match != null) {
			fPreviousSearchPatterns.remove(match);
		}
		match= new SearchPatternData(
				getSearchFor(),
				getLimitTo(),
				pattern,
				fCaseSensitive.getSelection(),
				fIsRegExCheckbox.getSelection(),
				fJavaElement,
				getContainer().getSelectedScope(),
				getContainer().getSelectedWorkingSets()
		);
			
		fPreviousSearchPatterns.add(0, match); // insert on top
		return match;
	}

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fPreviousSearchPatterns.size();
		String [] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchPatternData) fPreviousSearchPatterns.get(i)).getPattern();
		return patterns;
	}

	private boolean isCaseSensitive() {
		return fCaseSensitive.getSelection();
	}

	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				if (!initializePatternControl()) {
					fPattern.select(0);
					handleWidgetSelected();
				}
				initSelections();
			}
			fPattern.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}
	
	final void updateOKStatus() {
		boolean regexStatus= validateRegex();
		getContainer().setPerformActionEnabled(regexStatus);
	}

	//---- Widget creation ------------------------------------------------

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		readConfiguration();
		
		Composite result= new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridLayout layout= new GridLayout(2, false);
		result.setLayout(layout);
		
		addTextPatternControls(result);
		
		Label separator= new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint= convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		Control searchFor= createSearchFor(result);
		searchFor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		Control limitTo= createLimitTo(result);
		limitTo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		SelectionAdapter javaElementInitializer= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (getSearchFor() == fInitialData.getSearchFor())
					fJavaElement= fInitialData.getJavaElement();
				else
					fJavaElement= null;
				setLimitTo(getSearchFor(), getLimitTo());
				doPatternModified();
			}
		};

		for (int i= 0; i < fSearchFor.length; i++) {
			fSearchFor[i].addSelectionListener(javaElementInitializer);
		}
		
		separator= new Label(result, SWT.NONE);
		separator.setVisible(false);
		data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint= convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		Control scope = createScope(result);
		scope.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		
		setControl(result);
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, IApiToolsHelpContextIds.APITOOLS_SEARCH_PAGE);
	}
	
	private IEditorPart getActiveEditor() {
		IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage != null) {
			return activePage.getActiveEditor();
		}
		return null;
	}
	
	private SearchPatternData getDefaultInitValues() {
		if (!fPreviousSearchPatterns.isEmpty()) {
			return (SearchPatternData) fPreviousSearchPatterns.get(0);
		}

		return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, fIsCaseSensitive, fIsRegExSearch, "", null); //$NON-NLS-1$
	}
	
	private SearchPatternData determineInitValuesFrom(IJavaElement element) {			
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.PACKAGE_DECLARATION:
				return new SearchPatternData(IElementDescriptor.T_PACKAGE, REFERENCES, true, false, element.getElementName(), element);
			case IJavaElement.IMPORT_DECLARATION: {
				IImportDeclaration declaration= (IImportDeclaration) element;
				if (declaration.isOnDemand()) {
					String name= Signature.getQualifier(declaration.getElementName());
					return new SearchPatternData(IElementDescriptor.T_PACKAGE, REFERENCES, true, false, name, element);
				}
				return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, true, false, element.getElementName(), element);
			}
			case IJavaElement.TYPE:
				return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, true, false, PatternStrings.getTypeSignature((IType) element), element);
			case IJavaElement.COMPILATION_UNIT: {
				IType mainType= ((ICompilationUnit) element).findPrimaryType();
				if (mainType != null) {
					return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, true, false, PatternStrings.getTypeSignature(mainType), mainType);
				}
				break;
			}
			case IJavaElement.CLASS_FILE: {
				IType mainType= ((IClassFile) element).getType();
				if (mainType.exists()) {
					return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, true, false, PatternStrings.getTypeSignature(mainType), mainType);
				}
				break;
			}
			case IJavaElement.FIELD:
				return new SearchPatternData(IElementDescriptor.T_FIELD, REFERENCES, true, false, PatternStrings.getFieldSignature((IField) element), element);
			case IJavaElement.METHOD:
				IMethod method= (IMethod) element;
				return new SearchPatternData(IElementDescriptor.T_METHOD, REFERENCES, true, false, PatternStrings.getMethodSignature(method), element);
		}
		return null;	
	}
	
	private SearchPatternData trySimpleTextSelection(ITextSelection selection) {
		String selectedText= selection.getText();
		if (selectedText != null && selectedText.length() > 0) {
			int i= 0;
			while (i < selectedText.length() && !IndentManipulation.isLineDelimiterChar(selectedText.charAt(i))) {
				i++;
			}
			if (i > 0) {
				return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, fIsCaseSensitive, fIsRegExSearch, selectedText.substring(0, i), null);
			}
		}
		return null;
	}
	
	private SearchPatternData tryStructuredSelection(IStructuredSelection selection) {
		if (selection == null || selection.size() > 1)
			return null;

		Object o= selection.getFirstElement();
		SearchPatternData res= null;
		if (o instanceof IJavaElement) {
			res= determineInitValuesFrom((IJavaElement) o);
		} else if (o instanceof LogicalPackage) {
			LogicalPackage lp= (LogicalPackage)o;
			return new SearchPatternData(IElementDescriptor.T_PACKAGE, REFERENCES, fIsCaseSensitive, fIsRegExSearch, lp.getElementName(), null);
		} else if (o instanceof IAdaptable) {
			IJavaElement element= (IJavaElement) ((IAdaptable) o).getAdapter(IJavaElement.class);
			if (element != null) {
				res= determineInitValuesFrom(element);
			}
		}
		if (res == null && o instanceof IAdaptable) {
			IWorkbenchAdapter adapter= (IWorkbenchAdapter)((IAdaptable)o).getAdapter(IWorkbenchAdapter.class);
			if (adapter != null) {
				return new SearchPatternData(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES, fIsCaseSensitive, fIsRegExSearch, adapter.getLabel(o), null);
			}
		}
		return res;
	}	
	
	private void initSelections() {
		ISelection sel= getContainer().getSelection();
		SearchPatternData initData= null;

		if (sel instanceof IStructuredSelection) {
			initData= tryStructuredSelection((IStructuredSelection) sel);
		} else if (sel instanceof ITextSelection) {
			IEditorPart activePart= getActiveEditor();
			if (activePart instanceof JavaEditor) {
				try {
					IJavaElement[] elements= SelectionConverter.codeResolve((JavaEditor) activePart);
					if (elements != null && elements.length > 0) {
						initData= determineInitValuesFrom(elements[0]);
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
			if (initData == null) {
				initData= trySimpleTextSelection((ITextSelection) sel);
			}
		}
		if (initData == null) {
			initData= getDefaultInitValues();
		}
		
		fInitialData= initData;
		fJavaElement= initData.getJavaElement();
		fCaseSensitive.setSelection(initData.isCaseSensitive());
		fCaseSensitive.setEnabled(fJavaElement == null);
		
		setSearchFor(initData.getSearchFor());
		setLimitTo(initData.getSearchFor(), initData.getLimitTo());
		
		fPattern.setText(initData.getPattern());
	}	
	
	private boolean validateRegex() {
		if (fIsRegExCheckbox.getSelection()) {
			try {
				Pattern.compile(fPattern.getText());
			} catch (PatternSyntaxException e) {
				String locMessage= e.getLocalizedMessage();
				int i= 0;
				while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
					i++;
				}
				statusMessage(true, locMessage.substring(0, i)); // only take first line
				return false;
			}
			statusMessage(false, ""); //$NON-NLS-1$
		} else {
			statusMessage(false, "(* = any string, ? = any character, \\ = escape for literals: * ? \\)"); 
		}
		return true;
	}

	private void addTextPatternControls(Composite group) {
		// grid layout with 2 columns

		// Info text		
		Label label= new Label(group, SWT.LEAD);
		label.setText("Se&arch string:"); 
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(group.getFont());

		// Pattern combo
		fPattern= new Combo(group, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(group.getFont());
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);
		
		fCaseSensitive= new Button(group, SWT.CHECK);
		fCaseSensitive.setText("Case sens&itive"); 
		fCaseSensitive.setSelection(fIsCaseSensitive);
		fCaseSensitive.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive= fCaseSensitive.getSelection();
			}
		});
		fCaseSensitive.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fCaseSensitive.setFont(group.getFont());

		// Text line which explains the special characters
		fStatusLabel= new CLabel(group, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fStatusLabel.setFont(group.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText("(* = any string, ? = any character, \\ = escape for literals: * ? \\)"); 

		// RegEx checkbox
		fIsRegExCheckbox= new Button(group, SWT.CHECK);
		fIsRegExCheckbox.setText("&Regular expression"); 
		fIsRegExCheckbox.setSelection(fIsRegExSearch);
		fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsRegExSearch= fIsRegExCheckbox.getSelection();
				updateOKStatus();

				writeConfiguration();
			}
		});
		fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIsRegExCheckbox.setFont(group.getFont());
	}

	private void handleWidgetSelected() {
		int selectionIndex= fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
			return;
		
		SearchPatternData patternData= (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);
		if (!fPattern.getText().equals(patternData.getPattern()))
			return;
		fCaseSensitive.setSelection(patternData.isCaseSensitive);
		fIsRegExCheckbox.setSelection(patternData.isRegEx);
		fPattern.setText(patternData.getPattern());
	}

	private boolean initializePatternControl() {
		ISelection selection= getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty()) {
			String text= ((ITextSelection) selection).getText();
			if (text != null) {
				fPattern.setText(insertEscapeChars(text));
				return true;
			}
		}
		return false;
	}

	private String insertEscapeChars(String text) {
		if (text == null || text.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		StringBuffer sbIn= new StringBuffer(text);
		BufferedReader reader= new BufferedReader(new StringReader(text));
		int lengthOfFirstLine= 0;
		try {
			lengthOfFirstLine= reader.readLine().length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sbOut= new StringBuffer(lengthOfFirstLine + 5);
		int i= 0;
		while (i < lengthOfFirstLine) {
			char ch= sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\')
				sbOut.append("\\"); //$NON-NLS-1$
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}

	/**
	 * Sets the search page's container.
	 * @param container the container to set
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}
	
	private ISearchPageContainer getContainer() {
		return fContainer;
	}
	
	private ISelection getSelection() {
		return fContainer.getSelection();
	}
		

	//--------------- Configuration handling --------------
	
    /* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}
	
	/**
	 * Returns the page settings for this Text search page.
	 * 
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		return ApiUIPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
	}
		
	
	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
		fIsRegExSearch= s.getBoolean(STORE_IS_REG_EX_SEARCH);
		fSearchDerived= s.getBoolean(STORE_SEARCH_DERIVED);
		
		try {
			int historySize= s.getInt(STORE_HISTORY_SIZE);
			for (int i= 0; i < historySize; i++) {
				IDialogSettings histSettings= s.getSection(STORE_HISTORY + i);
				if (histSettings != null) {
					SearchPatternData data= SearchPatternData.create(histSettings);
					if (data != null) {
						fPreviousSearchPatterns.add(data);
					}
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}
	
	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s= getDialogSettings();
		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
		s.put(STORE_IS_REG_EX_SEARCH, fIsRegExSearch);
		s.put(STORE_SEARCH_DERIVED, fSearchDerived);
		
		int historySize= Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i= 0; i < historySize; i++) {
			IDialogSettings histSettings= s.addNewSection(STORE_HISTORY + i);
			SearchPatternData data= ((SearchPatternData) fPreviousSearchPatterns.get(i));
			data.store(histSettings);
		}
	}
	
	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error)
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		else
			fStatusLabel.setForeground(null);
	}

	private Button createButton(Composite parent, int style, String text, int data, boolean isSelected) {
		Button button= new Button(parent, style);
		button.setText(text);
		button.setData(new Integer(data));
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setSelection(isSelected);
		return button;
	}
	
	private Control createScope(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText("Scope"); 
		result.setLayout(new GridLayout(2, false));

		Label label= new Label(result, SWT.LEAD);
		label.setText("API P&rofile:"); 
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setFont(result.getFont());

		fScope= new Combo(result, SWT.SINGLE | SWT.BORDER);
		IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();
		// TODO: this can cause a "hang"
		fProfiles = manager.getApiProfiles();
		Arrays.sort(fProfiles, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((IApiProfile)o1).getName().compareTo(((IApiProfile)o2).getName());
			}
		});
		String[] names = new String[fProfiles.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = fProfiles[i].getName();
		}
		fScope.setItems(names);
		fScope.setFont(result.getFont());
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);
		
		return result;		
	}	
		
	private Control createSearchFor(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText("Search for"); 
		result.setLayout(new GridLayout(2, true));

		fSearchFor= new Button[] {
			createButton(result, SWT.RADIO, "&Type", IElementDescriptor.T_REFERENCE_TYPE, true),
			createButton(result, SWT.RADIO, "&Method", IElementDescriptor.T_METHOD, false),
			createButton(result, SWT.RADIO, "&Package", IElementDescriptor.T_PACKAGE, false),
			createButton(result, SWT.RADIO, "&Field", IElementDescriptor.T_FIELD, false)
		};
			
		// Fill with dummy radio buttons
		Label filler= new Label(result, SWT.NONE);
		filler.setVisible(false);
		filler.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		return result;		
	}
	
	private void doPatternModified() {
		if (fInitialData != null && getPattern().equals(fInitialData.getPattern()) && fInitialData.getJavaElement() != null && fInitialData.getSearchFor() == getSearchFor()) {
			fCaseSensitive.setEnabled(false);
			fCaseSensitive.setSelection(true);
			fJavaElement= fInitialData.getJavaElement();
		} else {
			fCaseSensitive.setEnabled(true);
			fCaseSensitive.setSelection(fIsCaseSensitive);
			fJavaElement= null;
		}
	}
	
	private Control createLimitTo(Composite parent) {
		fLimitToGroup= new Group(parent, SWT.NONE);
		fLimitToGroup.setText("Limit to"); 
		fLimitToGroup.setLayout(new GridLayout(2, false));

		fillLimitToGroup(IElementDescriptor.T_REFERENCE_TYPE, REFERENCES);
		
		return fLimitToGroup;
	}
	
	private int setLimitTo(int searchFor, int limitTo) {
		if (searchFor != IElementDescriptor.T_REFERENCE_TYPE && limitTo == IMPLEMENTORS) {
			limitTo= REFERENCES;
		}

		if (searchFor != IElementDescriptor.T_FIELD && (limitTo == READ_ACCESSES || limitTo == WRITE_ACCESSES)) {
			limitTo= REFERENCES;
		}
		fillLimitToGroup(searchFor, limitTo);
		return limitTo;
	}
	
	private void setSearchFor(int searchFor) {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			button.setSelection(searchFor == getIntData(button));
		}
	}	
	
	private int getLimitTo() {
		for (int i= 0; i < fLimitTo.length; i++) {
			Button button= fLimitTo[i];
			if (button.getSelection()) {
				return getIntData(button);
			}
		}
		return -1;
	}	
	
	private int getIntData(Button button) {
		return ((Integer) button.getData()).intValue();
	}
	
	private int getSearchFor() {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			if (button.getSelection()) {
				return getIntData(button);
			}
		}
		Assert.isTrue(false, "shouldNeverHappen"); //$NON-NLS-1$
		return -1;
	}	
		
	private void fillLimitToGroup(int searchFor, int limitTo) {
		Control[] children= fLimitToGroup.getChildren();
		for (int i= 0; i < children.length; i++) {
			children[i].dispose();
		}
		
		
		ArrayList buttons= new ArrayList();
		buttons.add(createButton(fLimitToGroup, SWT.RADIO, "R&eferences", REFERENCES, limitTo == REFERENCES));
		if (searchFor == IElementDescriptor.T_REFERENCE_TYPE) {
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "&Implementors", IMPLEMENTORS, limitTo == IMPLEMENTORS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "I&nstantiations", INSTANTIATIONS, limitTo == INSTANTIATIONS));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "&Subclasses", SUBTYPES, limitTo == SUBTYPES));
		}
		
		if (searchFor == IElementDescriptor.T_METHOD) {
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "&Overrides", OVERRIDE, limitTo == OVERRIDE));
		}
		
		if (searchFor == IElementDescriptor.T_FIELD) {
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Read a&ccesses", READ_ACCESSES, limitTo == READ_ACCESSES));
			buttons.add(createButton(fLimitToGroup, SWT.RADIO, "Wr&ite accesses", WRITE_ACCESSES, limitTo == WRITE_ACCESSES));
		}
		
		fLimitTo= (Button[]) buttons.toArray(new Button[buttons.size()]);
		
		SelectionAdapter listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				performLimitToSelectionChanged((Button) e.widget);
			}
		};
		for (int i= 0; i < fLimitTo.length; i++) {
			fLimitTo[i].addSelectionListener(listener);
		}
		
		fLimitToGroup.layout();
	}	
	
	protected final void performLimitToSelectionChanged(Button button) {
		if (button.getSelection()) {
			for (int i= 0; i < fLimitTo.length; i++) {
				Button curr= fLimitTo[i];
				if (curr != button) {
					curr.setSelection(false);
				}
			}
		}
	}	
}	
