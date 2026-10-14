/*******************************************************************************
 * Copyright (c) 2010, 2017 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *	   Lars Vogel <lars.vogel@gmail.com> - Enhancements
 *     Steven Spungin <steven@spungin.tv> - Modified to be generic picker, Bug 424730, Ongoing Maintenance
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.ContributionData;
import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.ContributionResultHandler;
import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.Filter;
import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.common.ClassContributionCollector;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.ContributionDataLabelProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.TableKeyNavigation;
import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.empty.E;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A find dialog for model contributions. List can be filtered by bundle and/or
 * package
 *
 * @author Steven Spungin
 */
public class FindContributionDialog extends TitleAreaDialog {
	private final IProject project;
	private Image javaClassImage;
	private TableViewer viewer;
	private final Messages Messages;
	private String bundleclassUri;
	private final String packageFilter;
	private final String bundleFilter;
	private final Bundle bundle;
	private final String mode;
	private String platformUri;

	public FindContributionDialog(IEclipseContext context) {
		super(context.get(Shell.class));
		this.project = context.get(IProject.class);
		this.Messages = context.get(Messages.class);
		packageFilter = (String) context.get("package"); //$NON-NLS-1$
		bundleFilter = (String) context.get("bundle"); //$NON-NLS-1$
		bundle = context.get(Bundle.class);
		mode = (String) context.get("mode"); //$NON-NLS-1$
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		// TODO param
		getShell().setText(Messages.ContributionClassDialog_ShellTitle);

		final ResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), getShell());
		if ("show-bundles".equals(mode)) { //$NON-NLS-1$
			// TODO create icon
		} else if ("show-packages".equals(mode)) { //$NON-NLS-1$
			// TODO create icon
		} else if ("show-icons".equals(mode)) { //$NON-NLS-1$
			// TODO create icon
		} else {
			setTitleImage(resources.create(
					ImageDescriptor.createFromFile(FindContributionDialog.class, "/icons/full/wizban/newclass_wiz.png"))); //$NON-NLS-1$
		}

		javaClassImage = resources
				.create(ImageDescriptor.createFromFile(FindContributionDialog.class, "/icons/full/obj16/class_obj.png")); //$NON-NLS-1$

		Composite container = new Composite(comp, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(2, false));

		Label l = new Label(container, SWT.NONE);
		// TODO param
		l.setText(Messages.ContributionClassDialog_Label_Classname);

		final Text t = new Text(container, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
		t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// TODO param
		t.setMessage(Messages.ContributionClassDialog_FilterText_Message);

		new Label(container, SWT.NONE);

		viewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(gd);
		viewer.setContentProvider(new ObservableListContentProvider<>());
		final ContributionDataLabelProvider classLabelProvider = new ContributionDataLabelProvider(javaClassImage);
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ContributionData data = (ContributionData) cell.getElement();
				StyledString styledString = new StyledString();
				if ("show-bundles".equals(mode)) { //$NON-NLS-1$
					styledString.append(data.bundleName, StyledString.DECORATIONS_STYLER);
				} else if ("show-packages".equals(mode)) { //$NON-NLS-1$
					styledString.append(packageOf(data.className), StyledString.DECORATIONS_STYLER);
				} else if ("show-icons".equals(mode)) { //$NON-NLS-1$
					styledString.append(data.iconPath, null);

					if (data.bundleName != null) {
						styledString.append(" - " + data.bundleName, StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
					}

					if (data.sourceType != null) {
						styledString.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
						styledString.append(data.sourceType + "", StyledString.COUNTER_STYLER); //$NON-NLS-1$
					}

					if (data.iconPath == null) {
						cell.setImage(javaClassImage);
					}
				} else {
					classLabelProvider.update(cell);
					return;
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});
		viewer.addDoubleClickListener(event -> okPressed());

		final WritableList<ContributionData> list = new WritableList<>();
		viewer.setInput(list);

		final ClassContributionCollector collector = getCollector();
		TableKeyNavigation.installOnText(t, () -> viewer.getTable());
		TableKeyNavigation.installOnTable(viewer.getTable(), t);

		t.addModifyListener(new ModifyListener() {
			private ContributionResultHandlerImpl currentResultHandler;

			@Override
			public void modifyText(ModifyEvent e) {
				if (currentResultHandler != null) {
					currentResultHandler.cancled = true;
				}
				list.clear();
				currentResultHandler = new ContributionResultHandlerImpl(list);
				Filter filter = new Filter(project, t.getText());
				collector.findContributions(filter, currentResultHandler);
			}
		});

		viewer.setFilters(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				ContributionData cd = (ContributionData) element;
				if ("show-bundles".equals(mode)) { //$NON-NLS-1$
					return isFirstWithKey(list, cd, data -> data.bundleName, data -> true);
				} else if ("show-packages".equals(mode)) { //$NON-NLS-1$
					if (bundleFilter != null && bundleFilter.isEmpty() == false) {
						if (!bundleFilter.equals(cd.bundleName)) {
							return false;
						}
					}
					return isFirstWithKey(list, cd, data -> packageOf(data.className),
							data -> E.isEmpty(bundleFilter) || bundleFilter.equals(data.bundleName));
				} else if ("show-icons".equals(mode)) { //$NON-NLS-1$
					if (cd.iconPath == null) {
						return false;
					}
					if (bundleFilter != null && bundleFilter.isEmpty() == false) {
						if (!bundleFilter.equals(cd.bundleName)) {
							return false;
						}
					}
					if (packageFilter != null && packageFilter.isEmpty() == false) {
						if (!cd.className.startsWith(packageFilter + ".")) { //$NON-NLS-1$
							return false;
						}
					}
					return true;
				} else {
					if (bundleFilter != null && bundleFilter.isEmpty() == false) {
						if (!bundleFilter.equals(cd.bundleName)) {
							return false;
						}
					}
					if (packageFilter != null && packageFilter.isEmpty() == false) {
						if (!cd.className.startsWith(packageFilter + ".")) { //$NON-NLS-1$
							return false;
						}
					}
					return true;
				}
			}
		});

		StringBuilder sbFind = new StringBuilder();

		if ("show-bundles".equals(mode)) { //$NON-NLS-1$
			setTitle(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findBundle);
			sbFind.append(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findBundle);
		} else if ("show-packages".equals(mode)) { //$NON-NLS-1$
			setTitle(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findPackage);
			sbFind.append(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findPackage);
			if (E.notEmpty(bundleFilter)) {
				sbFind.append(" " + org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_inBundle + " " + bundleFilter); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if ("show-icons".equals(mode)) { //$NON-NLS-1$
			setTitle(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findIcon);
			sbFind.append(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findIcon);
			if (E.notEmpty(bundleFilter)) {
				sbFind.append(" " + org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_inBundle + " " + bundleFilter); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (E.notEmpty(packageFilter)) {
				sbFind.append(" " + org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_inPackage + " " + packageFilter); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			setTitle(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findClass);
			sbFind.append(org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_findClass);
			if (E.notEmpty(bundleFilter)) {
				sbFind.append(" " + org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_inBundle + " " + bundleFilter); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (E.notEmpty(packageFilter)) {
				sbFind.append(" " + org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages.FindContributionDialog_inPackage + " " + packageFilter); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		setMessage(sbFind.toString());

		// preload list if few items
		if (list.size() <= 30) {
			t.notifyListeners(SWT.Modify, null);
		}
		return comp;
	}

	@Override
	protected void okPressed() {
		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
		if (!s.isEmpty()) {
			ContributionData cd = (ContributionData) s.getFirstElement();
			String uri = "bundleclass://" + cd.bundleName + "/" + cd.className; //$NON-NLS-1$ //$NON-NLS-2$
			platformUri = "platform:/plugin/" + cd.bundleName + "/" + cd.className; //$NON-NLS-1$ //$NON-NLS-2$
			setBundleclassUri(uri);
			super.okPressed();
		} else {
			super.cancelPressed();
		}
	}

	/** Returns the package of a fully qualified class name, empty for the default package. */
	public static String packageOf(String className) {
		final int dot = className.lastIndexOf('.');
		return dot >= 0 ? className.substring(0, dot) : ""; //$NON-NLS-1$
	}

	/** Returns true if no earlier candidate in the list has the same key, so each key is shown once. */
	public static boolean isFirstWithKey(List<ContributionData> list, ContributionData data,
			Function<ContributionData, String> key, Predicate<ContributionData> candidate) {
		final String wanted = key.apply(data);
		for (ContributionData other : list) {
			if (candidate.test(other) && Objects.equals(key.apply(other), wanted)) {
				return other == data;
			}
		}
		return true;
	}

	private ClassContributionCollector getCollector() {
		if (bundle == null) {
			return null;
		}
		BundleContext context = bundle.getBundleContext();
		ServiceReference<?> ref = context.getServiceReference(ClassContributionCollector.class.getName());
		if (ref != null) {
			return (ClassContributionCollector) context.getService(ref);
		}
		return null;
	}

	public String getBundleclassUri() {
		return bundleclassUri;
	}

	public String getPlatformUri() {
		return platformUri;
	}

	public void setBundleclassUri(String bundleclass) {
		this.bundleclassUri = bundleclass;
	}

	private static class ContributionResultHandlerImpl implements ContributionResultHandler {
		private boolean cancled = false;
		private final IObservableList<ContributionData> list;

		public ContributionResultHandlerImpl(IObservableList<ContributionData> list) {
			this.list = list;
		}

		@Override
		public void result(ContributionData data) {
			if (!cancled) {
				list.add(data);
			}
		}

		@Override
		public void moreResults(int hint, Filter filter) {
		}

	}
}