/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

/**
 * Tab that allows users to specify patterns that can be used to augment the search
 * to override API description settings for what is and what is not API.
 *<br><br>
 * For example a bundle manifest could say that a.b.c.provisional.package is
 * API and using this tab a user could provide a pattern a\.\.c\.provisional.* saying that
 * this should be treated as internal code use
 * 
 * @since 1.1
 */
public class ApiUsePatternTab extends AbstractLaunchConfigurationTab {

	class Labels extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {return null;}
		public String getColumnText(Object element, int columnIndex) {
			Pattern pattern = (Pattern) element;
			switch(columnIndex) {
				case 0: {
					return pattern.pattern;
				}
				case 1: {
					switch(pattern.kind) {
						case Pattern.API: {
							return Messages.ApiUsePatternTab_API;
						}
						case Pattern.INTERNAL: {
							return Messages.ApiUsePatternTab_internal;
						}
						case Pattern.JAR: {
							return Messages.ApiUsePatternTab_archive;
						}
						case Pattern.REPORT: {
							return Messages.ApiUsePatternTab_report;
						}
						case Pattern.REPORT_TO: {
							return Messages.ApiUsePatternTab_report_to;
						}
					}
				}
			}
			return null;
		}
	}
	
	class RegexValidator implements IInputValidator {
		public String isValid(String newText) {
			if(IApiToolsConstants.EMPTY_STRING.equals(newText)) {
				return Messages.ApiUsePatternTab_provide_regex;
			}
			try {
				java.util.regex.Pattern.compile(newText);
			}
			catch(PatternSyntaxException pse) {
				return pse.getDescription(); 
			}
			return null;
		}
	}
	
	class Pattern {
		static final int API = 1, INTERNAL = 2, JAR = 3, REPORT = 4, REPORT_TO = 5;
		String pattern = null;
		int kind = -1;
		public Pattern(String pattern, int kind) {
			this.pattern = pattern;
			this.kind = kind;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.pattern;
		}
	}
	
	TreeSet patterns = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
			return ((Pattern)o1).pattern.compareTo(((Pattern)o2).pattern);
		}
	});
	TableViewer viewer = null;
	Image image = null;
	Button addbutton = null, editbutton = null, removebutton = null;
	ColumnLayoutData[] columndata = {
			new ColumnWeightData(80), 
			new ColumnWeightData(20)}; 
	String[] columnnames = {
			"Pattern", //$NON-NLS-1$
			"Kind"}; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp, Messages.ApiUsePatternTab_patterns, 2);
		Composite tcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_BOTH, 0, 0);
		GridData gd = (GridData) tcomp.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		Table table = new Table(tcomp, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		table.setHeaderVisible(true);
		TableLayout layout = new TableLayout();
		for (int i = 0; i < columndata.length; i++) {
			layout.addColumnData(columndata[i]);
		}
		table.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.widthHint = 250;
		table.setLayoutData(gd);
		this.viewer = new TableViewer(table);
		this.viewer.setColumnProperties(columnnames);
		this.viewer.setComparator(new ViewerComparator() {
			public int category(Object element) {
				return ((Pattern)element).kind;
			}
		});
		this.viewer.setLabelProvider(new Labels());
		this.viewer.setContentProvider(new ArrayContentProvider());
		this.viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons((IStructuredSelection) event.getSelection());
			}
		});
		this.viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doEdit();
			}
		});
		TableColumn column = null;
		for (int i = 0; i < columnnames.length; i++) {
			column = new TableColumn(table, SWT.NONE);
			column.setResizable(false);
			column.setMoveable(false);
			column.setText(columnnames[i]);
		}
		this.viewer.setInput(this.patterns);
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					doRemove();
				}
			}
		});
		
		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		this.addbutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_add, null);
		this.addbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PatternWizard wizard = new PatternWizard(null, -1);
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				if(dialog.open() == IDialogConstants.OK_ID) {
					addPattern(wizard.getPattern(), wizard.getKind());
					ApiUsePatternTab.this.viewer.refresh(true, true);
					updateLaunchConfigurationDialog();
				}
			}
		});
		this.editbutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_edit, null);
		this.editbutton.setEnabled(false);
		this.editbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doEdit();
			}
		});
		this.removebutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_remove, null);
		this.removebutton.setEnabled(false);
		this.removebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_USE_PATTERN_TAB);
		setControl(comp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getShell()
	 */
	protected Shell getShell() {
		return super.getShell();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#updateLaunchConfigurationDialog()
	 */
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}
	
	/**
	 * Removes the selected elements from the table
	 */
	void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) ApiUsePatternTab.this.viewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			removePattern((Pattern) iter.next());
		}
		this.viewer.refresh();
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * handles editing a selected pattern
	 */
	void doEdit() {
		IStructuredSelection selection = (IStructuredSelection) ApiUsePatternTab.this.viewer.getSelection();
		Pattern pattern = (Pattern) selection.getFirstElement();
		PatternWizard wizard = new PatternWizard(pattern.pattern, pattern.kind);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if(dialog.open() == IDialogConstants.OK_ID) {
			pattern.pattern = wizard.getPattern();
			pattern.kind = wizard.getKind();
			ApiUsePatternTab.this.viewer.refresh(pattern, true, true);
			updateLaunchConfigurationDialog();
		}
	}
	
	/**
	 * Updates the buttons based on the selection in the viewer
	 * @param selection
	 */
	void updateButtons(IStructuredSelection selection) {
		int size = selection.size();
		this.editbutton.setEnabled(size == 1);
		this.removebutton.setEnabled(size > 0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return Messages.ApiUsePatternTab_patterns_title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_TEXT_EDIT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			this.patterns.clear();
			List pats = configuration.getAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, (List)null);
			if(pats != null) {
				for (Iterator iter = pats.iterator(); iter.hasNext();) {
					addPattern((String) iter.next(), Pattern.API);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, (List)null);
			if(pats != null) {
				for (Iterator iter = pats.iterator(); iter.hasNext();) {
					addPattern((String) iter.next(), Pattern.INTERNAL);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, (List)null);
			if(pats != null) {
				for (Iterator iter = pats.iterator(); iter.hasNext();) {
					addPattern((String) iter.next(), Pattern.JAR);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, (List)null);
			if(pats != null) {
				for (Iterator iter = pats.iterator(); iter.hasNext();) {
					addPattern((String) iter.next(), Pattern.REPORT);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, (List)null);
			if(pats != null) {
				for (Iterator iter = pats.iterator(); iter.hasNext();) {
					addPattern((String) iter.next(), Pattern.REPORT_TO);
				}
			}
			this.viewer.refresh();
		}
		catch(CoreException ce) {
			ApiUIPlugin.log(ce);
		}
	}

	/**
	 * Adds a new pattern to the list
	 * @param pattern
	 * @param kind
	 * @return
	 */
	boolean addPattern(String pattern, int kind) {
		return this.patterns.add(new Pattern(pattern, kind));
	}
	
	/**
	 * Removes the pattern from the listing
	 * @param pattern
	 * @return
	 */
	boolean removePattern(Pattern pattern) {
		return this.patterns.remove(pattern);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		ArrayList api = new ArrayList();
		ArrayList internal = new ArrayList();
		ArrayList jar = new ArrayList();
		ArrayList report = new ArrayList();
		ArrayList reportto = new ArrayList();
		Pattern pattern = null;
		for (Iterator iter = this.patterns.iterator(); iter.hasNext();) {
			pattern = (Pattern) iter.next();
			switch(pattern.kind) {
				case Pattern.API: {
					api.add(pattern.pattern);
					break;
				}
				case Pattern.INTERNAL: {
					internal.add(pattern.pattern);
					break;
				}
				case Pattern.JAR: {
					jar.add(pattern.pattern);
					break;
				}
				case Pattern.REPORT: {
					report.add(pattern.pattern);
					break;
				}
				case Pattern.REPORT_TO: {
					reportto.add(pattern.pattern);
					break;
				}
			}
		}
		configuration.setAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, api.size() > 0 ? api : (List)null);
		configuration.setAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, internal.size() > 0 ? internal : (List)null);
		configuration.setAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, jar.size() > 0 ? jar : (List)null);
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, report.size() > 0 ? report : (List)null);
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, reportto.size() > 0 ? reportto : (List)null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		//do nothing, default is no patterns
	}
}
