/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import java.util.ArrayList;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

/**
 * Tab that allows users to specify patterns that can be used to augment the
 * search to override API description settings for what is and what is not API. <br>
 * <br>
 * For example a bundle manifest could say that a.b.c.provisional.package is API
 * and using this tab a user could provide a pattern a\.\.c\.provisional.*
 * saying that this should be treated as internal code use
 *
 * @since 1.1
 */
public class ApiUsePatternTab extends AbstractLaunchConfigurationTab {

	class Labels extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Pattern pattern = (Pattern) element;
			switch (columnIndex) {
				case 0: {
					return pattern.pattern;
				}
				case 1: {
					switch (pattern.kind) {
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
						default:
							break;
					}
					break;
				}
				default:
					break;
			}
			return null;
		}
	}

	class RegexValidator implements IInputValidator {
		@Override
		public String isValid(String newText) {
			if (IApiToolsConstants.EMPTY_STRING.equals(newText)) {
				return Messages.ApiUsePatternTab_provide_regex;
			}
			try {
				java.util.regex.Pattern.compile(newText);
			} catch (PatternSyntaxException pse) {
				return pse.getDescription();
			}
			return null;
		}
	}

	class Pattern {
		static final int API = 1, INTERNAL = 2, JAR = 3, REPORT = 4,
				REPORT_TO = 5;
		String pattern = null;
		int kind = -1;

		public Pattern(String pattern, int kind) {
			this.pattern = pattern;
			this.kind = kind;
		}

		@Override
		public String toString() {
			return this.pattern;
		}
	}

	TreeSet<Pattern> patterns = new TreeSet<>((o1, o2) -> o1.pattern.compareTo(o2.pattern));
	TableViewer viewer = null;
	Image image = null;
	Button addbutton = null, editbutton = null, removebutton = null;
	ColumnLayoutData[] columndata = {
			new ColumnWeightData(80), new ColumnWeightData(20) };
	String[] columnnames = {
			Messages.ApiUsePatternTab_column_pattern,
			Messages.ApiUsePatternTab_column_kind };

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(comp, Messages.ApiUsePatternTab_patterns, 2, 100);
		Composite tcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_BOTH, 0, 0);
		GridData gd = (GridData) tcomp.getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		Table table = new Table(tcomp, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		table.setHeaderVisible(true);
		TableLayout layout = new TableLayout();
		for (ColumnLayoutData element : columndata) {
			layout.addColumnData(element);
		}
		table.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.widthHint = 250;
		table.setLayoutData(gd);
		this.viewer = new TableViewer(table);
		this.viewer.setColumnProperties(columnnames);
		this.viewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				return ((Pattern) element).kind;
			}
		});
		this.viewer.setLabelProvider(new Labels());
		this.viewer.setContentProvider(ArrayContentProvider.getInstance());
		this.viewer.addSelectionChangedListener(event -> updateButtons(event.getStructuredSelection()));
		this.viewer.addDoubleClickListener(event -> doEdit());
		TableColumn column = null;
		for (String columnname : columnnames) {
			column = new TableColumn(table, SWT.NONE);
			column.setResizable(false);
			column.setMoveable(false);
			column.setText(columnname);
		}
		this.viewer.setInput(this.patterns);
		table.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				doRemove();
			}
		}));

		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		this.addbutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_add, null);
		this.addbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			PatternWizard wizard = new PatternWizard(null, -1);
			WizardDialog dialog = new WizardDialog(getShell(), wizard);
			if (dialog.open() == IDialogConstants.OK_ID) {
				addPattern(wizard.getPattern(), wizard.getKind());
				ApiUsePatternTab.this.viewer.refresh(true, true);
				updateLaunchConfigurationDialog();
			}
		}));
		this.editbutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_edit, null);
		this.editbutton.setEnabled(false);
		this.editbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doEdit()));
		this.removebutton = SWTFactory.createPushButton(bcomp, Messages.ApiUsePatternTab_remove, null);
		this.removebutton.setEnabled(false);
		this.removebutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doRemove()));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_USE_PATTERN_TAB);
		setControl(comp);
	}

	@Override
	protected Shell getShell() {
		return super.getShell();
	}

	@Override
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}

	/**
	 * Removes the selected elements from the table
	 */
	void doRemove() {
		IStructuredSelection selection = ApiUsePatternTab.this.viewer.getStructuredSelection();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			removePattern((Pattern) iter.next());
		}
		this.viewer.refresh();
		updateLaunchConfigurationDialog();
	}

	/**
	 * handles editing a selected pattern
	 */
	void doEdit() {
		IStructuredSelection selection = ApiUsePatternTab.this.viewer.getStructuredSelection();
		Pattern pattern = (Pattern) selection.getFirstElement();
		PatternWizard wizard = new PatternWizard(pattern.pattern, pattern.kind);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if (dialog.open() == IDialogConstants.OK_ID) {
			pattern.pattern = wizard.getPattern();
			pattern.kind = wizard.getKind();
			ApiUsePatternTab.this.viewer.refresh(pattern, true, true);
			updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Updates the buttons based on the selection in the viewer
	 *
	 * @param selection
	 */
	void updateButtons(IStructuredSelection selection) {
		int size = selection.size();
		this.editbutton.setEnabled(size == 1);
		this.removebutton.setEnabled(size > 0);
	}

	@Override
	public String getName() {
		return Messages.ApiUsePatternTab_patterns_title;
	}

	@Override
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_TEXT_EDIT);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			this.patterns.clear();
			List<String> pats = configuration.getAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, (List<String>) null);
			if (pats != null) {
				for (String pattern : pats) {
					addPattern(pattern, Pattern.API);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, (List<String>) null);
			if (pats != null) {
				for (String pattern : pats) {
					addPattern(pattern, Pattern.INTERNAL);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, (List<String>) null);
			if (pats != null) {
				for (String pattern : pats) {
					addPattern(pattern, Pattern.JAR);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, (List<String>) null);
			if (pats != null) {
				for (String pattern : pats) {
					addPattern(pattern, Pattern.REPORT);
				}
			}
			pats = configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, (List<String>) null);
			if (pats != null) {
				for (String pattern : pats) {
					addPattern(pattern, Pattern.REPORT_TO);
				}
			}
			this.viewer.refresh();
		} catch (CoreException ce) {
			ApiUIPlugin.log(ce);
		}
	}

	/**
	 * Adds a new pattern to the list
	 *
	 * @param pattern
	 * @param kind
	 * @return
	 */
	boolean addPattern(String pattern, int kind) {
		return this.patterns.add(new Pattern(pattern, kind));
	}

	/**
	 * Removes the pattern from the listing
	 *
	 * @param pattern
	 * @return
	 */
	boolean removePattern(Pattern pattern) {
		return this.patterns.remove(pattern);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		ArrayList<String> api = new ArrayList<>();
		ArrayList<String> internal = new ArrayList<>();
		ArrayList<String> jar = new ArrayList<>();
		ArrayList<String> report = new ArrayList<>();
		ArrayList<String> reportto = new ArrayList<>();
		for (Pattern pattern : this.patterns) {
			switch (pattern.kind) {
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
				default:
					break;
			}
		}
		configuration.setAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, api.size() > 0 ? api : (List<String>) null);
		configuration.setAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, internal.size() > 0 ? internal : (List<String>) null);
		configuration.setAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, jar.size() > 0 ? jar : (List<String>) null);
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, report.size() > 0 ? report : (List<String>) null);
		configuration.setAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, reportto.size() > 0 ? reportto : (List<String>) null);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// do nothing, default is no patterns
	}
}
