/*******************************************************************************
 * Copyright (c) 2015, 2022 OPCoach and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #451116)
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 466785
 *******************************************************************************/
package org.eclipse.pde.spy.bundle;

import java.util.Iterator;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.spy.bundle.internal.BundleDataFilter;
import org.eclipse.pde.spy.bundle.internal.BundleDataProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * This class is the main part of the bundle spy. It displays a tableviewer with
 * all bundles
 */
public class BundleSpyPart {

	private static final String ICON_REFRESH = "icons/refresh.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_ACTIVE = "icons/state_active.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_STARTING = "icons/state_starting.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_STOPPING = "icons/state_stopping.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_RESOLVED = "icons/state_resolved.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_INSTALLED = "icons/state_installed.svg"; //$NON-NLS-1$
	public static final String ICON_STATE_UNINSTALLED = "icons/state_uninstalled.svg"; //$NON-NLS-1$
	public static final String ICON_START = "icons/start.svg"; //$NON-NLS-1$
	public static final String ICON_STOP = "icons/stop.svg"; //$NON-NLS-1$

	private TableViewer bundlesTableViewer;

	private Text filterText;

	private Button showOnlyFilteredElements;

	private BundleDataFilter bundleFilter;

	@Inject
	private IEclipseContext ctx;

	/** Store the values to set it when it is reopened */
	private static String lastFilterText = null;
	private static boolean lastShowFiltered = false;

	/**
	 * Create contents of the view part.
	 */
	@PostConstruct
	public void createControls(Composite parent) {
		ImageRegistry imgReg = initializeImageRegistry();

		// Set a filter in context (-> null at the begining).
		bundleFilter = new BundleDataFilter();
		ctx.set(BundleDataFilter.class, bundleFilter);

		parent.setLayout(new GridLayout(1, false));

		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));

		Button refreshButton = new Button(comp, SWT.FLAT);
		refreshButton.setImage(imgReg.get(ICON_REFRESH));
		refreshButton.setToolTipText(Messages.BundleSpyPart_9);
		refreshButton
				.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> bundlesTableViewer.refresh(true)));

		filterText = new Text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).applyTo(filterText);
		filterText.setMessage(Messages.BundleSpyPart_10);
		filterText.setToolTipText(
				Messages.BundleSpyPart_11);
		if (lastFilterText != null) {
			filterText.setText(lastFilterText);
		}
		bundleFilter.setPattern(lastFilterText);
		filterText.addKeyListener(KeyListener.keyReleasedAdapter(e -> {
			String textToSearch = filterText.getText();
			lastFilterText = textToSearch;
			boolean enableButton = textToSearch.length() > 0;
			// Enable/disable button for filtering
			showOnlyFilteredElements.setEnabled(enableButton);

			// Then update filters and viewers
			bundleFilter.setPattern(textToSearch);
			setFilter();
			bundlesTableViewer.refresh(true);
		}));

		showOnlyFilteredElements = new Button(comp, SWT.CHECK);
		showOnlyFilteredElements.setText(Messages.BundleSpyPart_12);
		showOnlyFilteredElements.setToolTipText(Messages.BundleSpyPart_13);
		showOnlyFilteredElements.setEnabled((lastFilterText != null) && (lastFilterText.length() > 0));
		showOnlyFilteredElements.setSelection(lastShowFiltered);
		showOnlyFilteredElements.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			lastShowFiltered = showOnlyFilteredElements.getSelection();
			setFilter();
		}));

		startButton = new Button(comp, SWT.FLAT);
		startButton.setImage(imgReg.get(ICON_START));
		startButton.setToolTipText(Messages.BundleSpyPart_14);
		startButton.setEnabled(false);
		startButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			IStructuredSelection sel = (IStructuredSelection) bundlesTableViewer.getSelection();
			Iterator<?> iter = sel.iterator();
			while (iter.hasNext()) {
				Bundle b = (Bundle) iter.next();
				try {
					b.start();
				} catch (BundleException e1) {
					e1.printStackTrace();
				}
			}
			bundlesTableViewer.refresh();
			updateButtonStatuses(sel);
		}));

		stopButton = new Button(comp, SWT.FLAT);
		stopButton.setImage(imgReg.get(ICON_STOP));
		stopButton.setToolTipText(Messages.BundleSpyPart_15);
		stopButton.setEnabled(false);
		stopButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (MessageDialog.openConfirm(((Control) e.getSource()).getShell(), Messages.BundleSpyPart_16,
					Messages.BundleSpyPart_17)) {
				IStructuredSelection sel = (IStructuredSelection) bundlesTableViewer.getSelection();
				Iterator<?> iter = sel.iterator();
				while (iter.hasNext()) {
					Bundle b = (Bundle) iter.next();
					try {
						b.stop();
					} catch (BundleException e1) {
						e1.printStackTrace();
					}
				}
				bundlesTableViewer.refresh();
				updateButtonStatuses(sel);
			}
		}));

		// Create the customer table with 2 columns: firstname and name
		bundlesTableViewer = new TableViewer(parent);
		final Table cTable = bundlesTableViewer.getTable();
		cTable.setHeaderVisible(true);
		cTable.setLinesVisible(true);
		GridData gd_cTable = new GridData(SWT.FILL, SWT.FILL, true, true);
		// gd_cTable.verticalAlignment = SWT.TOP;
		cTable.setLayoutData(gd_cTable);

		// Create the first column for bundle name
		addColumn(bundlesTableViewer, 35, Messages.BundleSpyPart_18, BundleDataProvider.COL_STATE);
		addColumn(bundlesTableViewer, 200, Messages.BundleSpyPart_19, BundleDataProvider.COL_NAME);
		addColumn(bundlesTableViewer, 200, Messages.BundleSpyPart_20, BundleDataProvider.COL_VERSION);

		// Set input data and content provider (default ArrayContentProvider)
		bundlesTableViewer.setContentProvider(ArrayContentProvider.getInstance());

		// Get the list of bundles in platform using bundle context...
		BundleContext bc = FrameworkUtil.getBundle(BundleSpyPart.class).getBundleContext();
		bundlesTableViewer.setInput(bc.getBundles());

		bundlesTableViewer.addSelectionChangedListener(event -> updateButtonStatuses((IStructuredSelection) event.getSelection()));

		ColumnViewerToolTipSupport.enableFor(bundlesTableViewer);

	}

	/** Update the stop and start buttons depending on current selection */
	protected void updateButtonStatuses(IStructuredSelection selection) {
		// startButton is enabled if at least one bundle is not active
		// stopButton is enabled if at least one bundle is active
		boolean oneBundleIsActive = false;
		boolean oneBundleIsNotActive = false;

		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Bundle b = (Bundle) iter.next();
			oneBundleIsActive = oneBundleIsActive || (b.getState() == Bundle.ACTIVE);
			oneBundleIsNotActive = oneBundleIsNotActive || (b.getState() != Bundle.ACTIVE);
		}
		startButton.setEnabled(oneBundleIsNotActive);
		stopButton.setEnabled(oneBundleIsActive);

	}

	private void addColumn(final TableViewer parentTable, int width, String title, final int column) {
		TableViewerColumn col = new TableViewerColumn(bundlesTableViewer, SWT.NONE);
		col.getColumn().setWidth(width);
		col.getColumn().setText(title);

		final BundleDataProvider bdp = ContextInjectionFactory.make(BundleDataProvider.class, ctx);
		bdp.setColumn(column);
		col.setLabelProvider(bdp);

		col.getColumn().addSelectionListener(new SelectionAdapter() {

			private int turnAround = 1;

			@Override
			public void widgetSelected(SelectionEvent e) {
				turnAround *= -1;
				parentTable.setComparator(new ViewerComparator() {
					@Override
					public int compare(Viewer viewer, Object e1, Object e2) {
						if (BundleDataProvider.COL_STATE == column) {
							Bundle b1 = (Bundle) e1;
							Bundle b2 = (Bundle) e2;
							return turnAround(Integer.compare(b1.getState(), b2.getState()));
						}

						return turnAround(bdp.getText(e1).compareTo(bdp.getText(e2)));
					}
				});
			}

			private int turnAround(int compare) {
				return compare * turnAround;
			}
		});

	}

	private static final ViewerFilter[] NO_FILTER = new ViewerFilter[0];
	private Button stopButton;
	private Button startButton;

	/** Set the filter on table */
	public void setFilter() {

		if (showOnlyFilteredElements.isEnabled() && showOnlyFilteredElements.getSelection()) {
			bundlesTableViewer.setFilters(bundleFilter);
		} else {
			bundlesTableViewer.setFilters(NO_FILTER);
		}
	}

	@Focus
	public void setFocus() {
		bundlesTableViewer.getControl().setFocus();
	}

	private ImageRegistry initializeImageRegistry() {
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		ImageRegistry imgReg = new ImageRegistry();
		imgReg.put(ICON_REFRESH, ImageDescriptor.createFromURL(b.getEntry(ICON_REFRESH)));
		imgReg.put(ICON_STATE_ACTIVE, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_ACTIVE)));
		imgReg.put(ICON_STATE_RESOLVED, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_RESOLVED)));
		imgReg.put(ICON_STATE_STARTING, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_STARTING)));
		imgReg.put(ICON_STATE_STOPPING, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_STOPPING)));
		imgReg.put(ICON_STATE_INSTALLED, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_INSTALLED)));
		imgReg.put(ICON_STATE_UNINSTALLED, ImageDescriptor.createFromURL(b.getEntry(ICON_STATE_UNINSTALLED)));
		imgReg.put(ICON_START, ImageDescriptor.createFromURL(b.getEntry(ICON_START)));
		imgReg.put(ICON_STOP, ImageDescriptor.createFromURL(b.getEntry(ICON_STOP)));

		ctx.set(ImageRegistry.class, imgReg);

		return imgReg;
	}

}
