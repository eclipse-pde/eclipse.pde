/*******************************************************************************
 *  Copyright (c) 2025 Patrick Ziegler and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTable;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * A FilteredCheckboxTable implementation to be used internally in PDE UI code.
 * This table stores all the table elements internally, and keeps the check
 * state in sync. This way, even if an element is filtered, the caller can get
 * and set the checked state.
 *
 * @since 3.16.100
 */
public class FilteredCheckboxTable extends FilteredTable {

	private static final long FILTER_DELAY = 400;
	private final FormToolkit toolkit;

	/**
	 * Constructor that creates a table with preset style bits and a
	 * CheckboxTableViewer for the table.
	 *
	 * @param parent
	 *            parent composite
	 * @param toolkit
	 *            optional toolkit to create UI elements with, required if the
	 *            table is being created in a form editor
	 */
	public FilteredCheckboxTable(Composite parent, FormToolkit toolkit) {
		this(parent, toolkit, SWT.NONE);
	}

	/**
	 * Constructor that creates a table with preset style bits and a
	 * CheckboxTableViewer for the table.
	 *
	 * @param parent
	 *            parent composite
	 * @param toolkit
	 *            optional toolkit to create UI elements with, required if the
	 *            table is being created in a form editor
	 */
	public FilteredCheckboxTable(Composite parent, FormToolkit toolkit, int tableStyle) {
		this(parent, toolkit, tableStyle, new PatternFilter());
	}

	/**
	 * Constructor that creates a table with preset style bits and a
	 * CheckboxTableViewer for the table.
	 *
	 * @param parent
	 *            parent composite
	 * @param toolkit
	 *            optional toolkit to create UI elements with, required if the
	 *            table is being created in a form editor
	 * @param filter
	 *            pattern filter to use in the filter control
	 */
	public FilteredCheckboxTable(Composite parent, FormToolkit toolkit, int tableStyle, PatternFilter filter) {
		super(parent);
		this.parent = parent;
		this.toolkit = toolkit;
		init(tableStyle, filter);
	}

	@Override
	protected TableViewer doCreateTableViewer(Composite parent, int style) {
		int tableStyle = style | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER;
		Table table = null;
		if (toolkit != null) {
			table = toolkit.createTable(parent, tableStyle);
		} else {
			table = new Table(parent, tableStyle);
		}

		return new CachedCheckboxTableViewer(table);
	}

	/*
	 * Overridden to hook a listener on the job and set the deferred content
	 * provider to synchronous mode before a filter is done.
	 *
	 * @see
	 * org.eclipse.e4.ui.dialogs.filteredtree.FilteredTable#doCreateRefreshJob()
	 */
	@Override
	protected Job doCreateRefreshJob() {
		Job filterJob = super.doCreateRefreshJob();
		filterJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				getDisplay().asyncExec(() -> {
					if (getViewer().getTable().isDisposed()) {
						return;
					}
					getViewer().restoreCachedState();
				});
			}
		});
		return filterJob;
	}

	@Override
	protected Text doCreateFilterText(Composite parent) {
		// Overridden so the text gets create using the toolkit if we have one
		Text parentText = super.doCreateFilterText(parent);
		if (toolkit != null) {
			int style = parentText.getStyle();
			parentText.dispose();
			return toolkit.createText(parent, null, style);
		}
		return parentText;
	}

	@Override
	public CachedCheckboxTableViewer getViewer() {
		return (CachedCheckboxTableViewer) super.getViewer();
	}

	@Override
	protected long getRefreshJobDelay() {
		return FILTER_DELAY;
	}

	/**
	 * Add wildcard at the beginning of filter string if user did not added
	 * wildcard himself.
	 */
	@Override
	protected String getFilterString() {
		String original = super.getFilterString();
		String asterisk = "*"; //$NON-NLS-1$
		if (original != null && !original.equals(getInitialText()) && !original.startsWith(asterisk)) {
			return asterisk + original;
		}
		return original;
	}
}