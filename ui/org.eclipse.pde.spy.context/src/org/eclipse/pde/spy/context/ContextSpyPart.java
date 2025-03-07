/*******************************************************************************
 * Copyright (c) 2013 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.context;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.spy.context.ContextDataFilter;
import org.eclipse.pde.internal.spy.context.ContextDataPart;
import org.eclipse.pde.internal.spy.context.ContextSpyHelper;
import org.eclipse.pde.internal.spy.context.ContextSpyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

/**
 * This class is the main part of the context spy. It creates a treeviewer and
 * the context data part listening to context selection
 */
public class ContextSpyPart {

	private static final String ICON_COLLAPSEALL = "icons/collapseall.png"; //$NON-NLS-1$
	private static final String ICON_EXPANDALL = "icons/expandall.png"; //$NON-NLS-1$
	private static final String ICON_REFRESH = "icons/refresh.png"; //$NON-NLS-1$

	// The ID for this part descriptor
	static final String CONTEXT_SPY_VIEW_DESC = "org.eclipse.e4.tools.context.spy.view"; //$NON-NLS-1$

	private TreeViewer contextTreeViewer;

	@Inject
	private ESelectionService selService;

	private ContextSpyProvider treeContentProvider;

	private ImageRegistry imgReg;

	@Inject
	private ContextDataFilter contextFilter;

	private ContextDataPart contextDataPart;
	private Button showOnlyFilteredElements;
	private Text filterText;

	/** Store the values to set it when it is reopened */
	private static String lastFilterText = null;
	private static boolean lastShowFiltered = false;

	@Inject
	private void initializeImageRegistry() {
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		imgReg = new ImageRegistry();
		imgReg.put(ICON_COLLAPSEALL, ImageDescriptor.createFromURL(b.getEntry(ICON_COLLAPSEALL)));
		imgReg.put(ICON_EXPANDALL, ImageDescriptor.createFromURL(b.getEntry(ICON_EXPANDALL)));
		imgReg.put(ICON_REFRESH, ImageDescriptor.createFromURL(b.getEntry(ICON_REFRESH)));
	}

	/**
	 * Create contents of the view part.
	 */
	@PostConstruct
	public void createControls(Composite parent, MApplication a, IEclipseContext ctx) {
		parent.setLayout(new GridLayout(1, false));

		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(7, false));

		Button refreshButton = new Button(comp, SWT.FLAT);
		refreshButton.setImage(imgReg.get(ICON_REFRESH));
		refreshButton.setToolTipText(Messages.ContextSpyPart_4);
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contextTreeViewer.refresh(true);
				contextDataPart.refresh(true);
			}
		});

		Button expandAll = new Button(comp, SWT.FLAT);
		expandAll.setImage(imgReg.get(ICON_EXPANDALL));
		expandAll.setToolTipText(Messages.ContextSpyPart_5);
		expandAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contextTreeViewer.expandAll();
			}
		});
		Button collapseAll = new Button(comp, SWT.FLAT);
		collapseAll.setImage(imgReg.get(ICON_COLLAPSEALL));
		collapseAll.setToolTipText(Messages.ContextSpyPart_6);
		collapseAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contextTreeViewer.collapseAll();
			}

		});

		filterText = new Text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).applyTo(filterText);
		filterText.setMessage(Messages.ContextSpyPart_7);
		filterText.setToolTipText(Messages.ContextSpyPart_8);
		if (lastFilterText != null) {
			filterText.setText(lastFilterText);
		}
		contextFilter.setPattern(lastFilterText);
		filterText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String textToSearch = filterText.getText();
				lastFilterText = textToSearch;
				boolean enableButton = textToSearch.length() > 0;
				// Enable/disable button for filtering
				showOnlyFilteredElements.setEnabled(enableButton);

				// Then update filters and viewers
				contextFilter.setPattern(textToSearch);
				setFilter();
				contextTreeViewer.refresh(true);
				contextDataPart.refresh(true);
			}

		});

		showOnlyFilteredElements = new Button(comp, SWT.CHECK);
		showOnlyFilteredElements.setText(Messages.ContextSpyPart_9);
		showOnlyFilteredElements.setToolTipText(Messages.ContextSpyPart_10);
		showOnlyFilteredElements.setEnabled((lastFilterText != null) && (lastFilterText.length() > 0));
		showOnlyFilteredElements.setSelection(lastShowFiltered);
		showOnlyFilteredElements.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lastShowFiltered = showOnlyFilteredElements.getSelection();
				setFilter();
			}
		});

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.V_SCROLL | SWT.H_SCROLL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// TreeViewer on the top
		contextTreeViewer = new TreeViewer(sashForm);
		treeContentProvider = ContextInjectionFactory.make(ContextSpyProvider.class, ctx);
		contextTreeViewer.setContentProvider(treeContentProvider);
		contextTreeViewer.setLabelProvider(treeContentProvider);
		contextTreeViewer.setComparator(new ViewerComparator());

		// tv.setInput(a);
		contextTreeViewer.setInput(ContextSpyHelper.getAllBundleContexts());

		contextTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				selService.setSelection((ss.size() == 1) ? ss.getFirstElement() : ss.toArray());
			}
		});

		IEclipseContext subCtx = ctx.createChild(Messages.ContextSpyPart_11);
		subCtx.set(Composite.class, sashForm);
		contextDataPart = ContextInjectionFactory.make(ContextDataPart.class, subCtx);
		setFilter();

		// Set the correct weight for SashForm
		sashForm.setWeights(new int[] { 35, 65 });

		// Open all the tree
		contextTreeViewer.expandAll();

	}

	/** Set the filter on context data part */
	public void setFilter() {
		if (showOnlyFilteredElements.isEnabled() && showOnlyFilteredElements.getSelection()) {
			contextDataPart.setFilter(contextFilter);
		} else {
			contextDataPart.setFilter(null);
		}
	}

	@PreDestroy
	public void dispose() {
	}

	@Focus
	public void setFocus() {
		contextTreeViewer.getControl().setFocus();
	}

}
