/*******************************************************************************
 * Copyright (c)  Lacherp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lacherp - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.pde.spy.adapter.model.AdapterData;
import org.eclipse.pde.spy.adapter.model.AdapterRepository;
import org.eclipse.pde.spy.adapter.tools.AdapterHelper;
import org.eclipse.pde.spy.adapter.viewer.AdapterContentProvider;
import org.eclipse.pde.spy.adapter.viewer.AdapterDataComparator;
import org.eclipse.pde.spy.adapter.viewer.AdapterFilter;
import org.eclipse.pde.spy.adapter.viewer.ColumnViewerToolTipSupportCustom;
import org.eclipse.pde.spy.adapter.viewer.FilterData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;


/**
 * Adapter Spy part 
 * @author pascal
 *
 */
public class AdapterSpyPart {

	private TreeViewer adapterTreeViewer;

	private AdapterContentProvider adapterContentProvider;

	private static final String NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION = "updateTreeSourceToDestination";
	private static final String NAMED_UPDATE_TREE_DESTINATION_TO_SOURCE = "updateTreeDestinationToSource";
	private static final String SWITCH_TO_SOURCE = "switch to Source Type first";
	private static final String SWITCH_TO_DESTINATION = "switch to Destination Type first";
	
	@Inject
	UISynchronize uisync;

	@Inject
	IEclipseContext context;


	@Inject
	AdapterRepository adapterRepo;

	@Inject
	ESelectionService selectService;
	
	AdapterFilter adapterFilter;

	boolean sourceToDestination = true;

	private TreeViewerColumn sourceOrDestinationTvc;

	private AdapterDataComparator comparator;

	@Inject
	public AdapterSpyPart(IEclipseContext context) {
		// wrap eclipse adapter
		AdapterHelper.wrapperEclipseAdapter();
		adapterFilter = ContextInjectionFactory.make(AdapterFilter.class, context);
		context.set(ImageRegistry.class, AdapterHelper.getImageRegistry(this));
	}

	@PostConstruct
	public void createControls(Composite parent, IExtensionRegistry extensionRegistry, ImageRegistry imgr) {

		parent.setLayout(new GridLayout(1, false));
		createToolBarZone(parent, imgr);

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.V_SCROLL | SWT.H_SCROLL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		AdapterHelper.getServicesContext().set(AdapterRepository.class, adapterRepo);
		adapterRepo.clear();
		
		Collection<AdapterData> adapterDatalist = adapterRepo.getAdapters();
		
		// Adapter TreeViewer
		adapterTreeViewer = new TreeViewer(sashForm);
		adapterContentProvider = ContextInjectionFactory.make(AdapterContentProvider.class, context);
		adapterContentProvider.setColumnIndex(0);
		adapterTreeViewer.setContentProvider(adapterContentProvider);
		adapterTreeViewer.setLabelProvider(adapterContentProvider);
		adapterTreeViewer.setFilters(adapterFilter);
		
		// add comparator
		comparator = new AdapterDataComparator(0);
		adapterTreeViewer.setComparator(comparator);
		
		// define columns
		final Tree cTree = adapterTreeViewer.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		sourceOrDestinationTvc = new TreeViewerColumn(adapterTreeViewer, SWT.NONE);
		sourceOrDestinationTvc.getColumn().setText("Source Type");
		sourceOrDestinationTvc.getColumn().setWidth(500);
		sourceOrDestinationTvc.setLabelProvider(adapterContentProvider);
		cTree.setSortColumn(sourceOrDestinationTvc.getColumn());
		sourceOrDestinationTvc.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(0);
				adapterTreeViewer.getTree().setSortDirection(comparator.getDirection());
				adapterTreeViewer.refresh();
			}
		});
		sourceOrDestinationTvc.setEditingSupport(new EditingSupport(adapterTreeViewer) {
			
			@Override
			protected void setValue(Object element, Object value) {
			}
			
			@Override
			protected Object getValue(Object element) {
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return null;
			}
			@Override
			protected boolean canEdit(Object element) {
				if(element instanceof AdapterData)
				{
					((AdapterData)element).setSelectedColumn(0);
					selectService.setSelection(element);
				}
				return false;
			}
		});
		
		
		TreeViewerColumn adapterFactoryClassTvc = new TreeViewerColumn(adapterTreeViewer, SWT.NONE);
		adapterFactoryClassTvc.getColumn().setText("AdapterFactory");
		adapterFactoryClassTvc.getColumn().setWidth(700);
		AdapterContentProvider adapterContentProvider2 = ContextInjectionFactory.make(AdapterContentProvider.class, context);
		adapterContentProvider2.setColumnIndex(1);
		adapterFactoryClassTvc.setLabelProvider(adapterContentProvider2);
		adapterFactoryClassTvc.setEditingSupport(new EditingSupport(adapterTreeViewer) {
			
			@Override
			protected void setValue(Object element, Object value) {
			}
			@Override
			protected Object getValue(Object element) {
				return null;
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return null;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				if(element instanceof AdapterData)
				{
					((AdapterData)element).setSelectedColumn(1);
					selectService.setSelection(element);
				}
				return false;
			}
		});
		
		
		ColumnViewerToolTipSupportCustom.enableFor(adapterTreeViewer);
		context.set(NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION, adapterDatalist);

	}

	@Inject
	@Optional
	private void updateAdapterTreeViewerSourceToType(
			@Named(NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION) Collection<AdapterData> adapaters) {
		if (adapaters == null) {
			return;
		}
		refreshAdapterTree(NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION, adapaters);
	}

	@Inject
	@Optional
	private void udpateAdapterTreeViewTypeToSource(
			@Named(NAMED_UPDATE_TREE_DESTINATION_TO_SOURCE) Collection<AdapterData> adapaters) {
		if (adapaters == null) {
			return;
		}
		// reduce source Type
		Collection<AdapterData> reduceresult = reduceType(adapaters);
		refreshAdapterTree(NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION, reduceresult);
	}
	
	@PreDestroy
	public void dispose() {
		adapterTreeViewer = null;
		if (adapterContentProvider != null) {
			ContextInjectionFactory.uninject(adapterContentProvider, context);
		}
		if (adapterFilter != null) {
			ContextInjectionFactory.uninject(adapterFilter, context);
		}
		AdapterHelper.restoreOriginalEclipseAdapter();
		context.set(AdapterFilter.UPDATE_CTX_FILTER, null);
		adapterRepo.clear();
		
	}

	@Inject
	@Optional
	public void handleSelection(@Named(IServiceConstants.ACTIVE_SELECTION) AdapterData adapterDataSelected) {
		if (adapterDataSelected == null) {
			return;
		}
		String toCopy ="";
		if( adapterDataSelected.getSelectedColumn() == 0 && adapterDataSelected.getParent() == null ) {
			toCopy = ((sourceToDestination)? adapterDataSelected.getSourceType():adapterDataSelected.getDestinationType());
		}
		if( adapterDataSelected.getSelectedColumn() == 0 && adapterDataSelected.getParent() != null ) {
			toCopy = ((sourceToDestination)? adapterDataSelected.getDestinationType():adapterDataSelected.getSourceType());
		}
		if( adapterDataSelected.getSelectedColumn() == 1 ) {
			if (!sourceToDestination)
				toCopy = adapterDataSelected.getAdapterDataParent().getAdapterClassName();
			else
				toCopy = adapterDataSelected.getAdapterClassName();
		}
		Clipboard clipboard = new Clipboard(null);
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			Transfer[] transfers = new Transfer[] { textTransfer };
			Object[] data = new Object[] { toCopy };
			clipboard.setContents(data, transfers);
		} finally {
			clipboard.dispose();
		}

	}
	
	private void createToolBarZone(Composite parent, ImageRegistry imgr) {
		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(4, false));

		Text filterText = new Text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(filterText);
		filterText.setMessage("Search data");
		filterText.setToolTipText("Find any element in tree");

		filterText.addModifyListener(e -> {
			FilterData fdata = getFilterData();
			if (filterText.getText().isEmpty()) {
				fdata.setTxtSeachFilter("");
			} else {
				fdata.setTxtSeachFilter(filterText.getText());
			}
			context.set(AdapterFilter.UPDATE_CTX_FILTER, fdata);
			adapterTreeViewer.refresh(true);

		});

		Button showPackageFilter = new Button(comp, SWT.CHECK);
		showPackageFilter.setText("Show package");

		showPackageFilter.setToolTipText("Show source type with packages name");
		showPackageFilter.setEnabled(true);
		showPackageFilter.setSelection(true);
		showPackageFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FilterData fdata = getFilterData();
				fdata.setShowPackage(!fdata.getShowPackage());
				context.set(AdapterFilter.UPDATE_CTX_FILTER, fdata);
				adapterTreeViewer.refresh(true);
			}
		});


		ToolBar toolBar = new ToolBar(comp, SWT.NONE);
		ToolItem switchButton = new ToolItem(toolBar, SWT.CHECK);
		switchButton.setImage(imgr.get(AdapterHelper.DESTINATION_TYPE_IMG_KEY));
		switchButton.setToolTipText(SWITCH_TO_DESTINATION);
		
		// sourceToType event
		switchButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				Object source = event.getSource();
				if (source instanceof ToolItem) {
					FilterData fdata = getFilterData();
					sourceToDestination = !sourceToDestination;
					String tooltiptext = sourceToDestination ? SWITCH_TO_DESTINATION :SWITCH_TO_SOURCE;
					String imageKey = sourceToDestination ? AdapterHelper.DESTINATION_TYPE_IMG_KEY
							: AdapterHelper.SOURCE_TYPE_IMG_KEY;
					switchButton.setToolTipText(tooltiptext);
					switchButton.setImage(imgr.get(imageKey));

					if (sourceToDestination) {
						sourceOrDestinationTvc.getColumn().setText("Source Type");
						context.set(NAMED_UPDATE_TREE_SOURCE_TO_DESTINATION, adapterRepo.getAdapters());
					} else {
						sourceOrDestinationTvc.getColumn().setText("Destination Type");
						context.set(NAMED_UPDATE_TREE_DESTINATION_TO_SOURCE, adapterRepo.revertSourceToType());
					}
					fdata.setSourceToDestination(sourceToDestination);
					context.set(AdapterFilter.UPDATE_CTX_FILTER, fdata);
					adapterTreeViewer.refresh(true);
				}

			}
		});
		uisync.asyncExec(() -> comp.pack());

	}

	private FilterData getFilterData() {
		if (context.get(AdapterFilter.UPDATE_CTX_FILTER) == null) {
			return new FilterData();
		}
		return new FilterData((FilterData) context.get(AdapterFilter.UPDATE_CTX_FILTER));
	}

	

	private Collection<AdapterData> reduceType(Collection<AdapterData> originalList) {
		Collection<AdapterData> reduceresult = originalList;

			Map<String, List<AdapterData>> resultmap = groupBy(originalList);
			reduceresult.clear();
			resultmap.forEach((k, v) -> {
				AdapterData firstElem = v.get(0);
				reduceresult.add(firstElem);
				for (int idx = 1; idx < v.size(); idx++) {
					firstElem.getChildrenList().addAll(v.get(idx).getChildrenList());
				}
			});

		return reduceresult;
	}

	private Map<String, List<AdapterData>> groupBy(Collection<AdapterData> originalList) {
		return originalList.stream().collect(Collectors.groupingBy(AdapterData::getDestinationType));
	}

	
	private void refreshAdapterTree(String namedContext, Collection<AdapterData> result) {
		uisync.syncExec(() -> {
			if (adapterTreeViewer != null) {
				adapterTreeViewer.setInput(result);
				context.set(namedContext, null);
			}
		});
	}

}
