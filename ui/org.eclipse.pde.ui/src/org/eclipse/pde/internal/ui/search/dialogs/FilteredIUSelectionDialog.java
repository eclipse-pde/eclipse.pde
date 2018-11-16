/*******************************************************************************
.
. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM - Ongoing development
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Comparator;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.Messages;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class FilteredIUSelectionDialog extends FilteredItemsSelectionDialog {

	private Button fShowLatestVersionOnlyButton;
	private boolean fShowLatestVersionOnly = true;
	private final IQuery<IInstallableUnit> query;
	private final ILabelProvider fLabelProvider = new IUWrapperLabelProvider();

//	private static final String S_PLUGINS = "showPlugins"; //$NON-NLS-1$
//	private static final String S_FEATURES = "showFeatures"; //$NON-NLS-1$
//	private static final String S_PACKAGES = "showPackages"; //$NON-NLS-1$
//
//	private static final int TYPE_PLUGIN = 0;
//	private static final int TYPE_FEATURE = 1;
//	private static final int TYPE_PACKAGE = 2;

	private class IUWrapperLabelProvider extends LabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {

		private PDELabelProvider labelProvider;

		public IUWrapperLabelProvider() {
			labelProvider = PDEPlugin.getDefault().getLabelProvider();
			labelProvider.connect(this);
		}

		@Override
		public StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();

			if (element instanceof IUPackage) {
				IUPackage iuPackage = (IUPackage) element;
				styledString.append(iuPackage.getId());
				styledString.append(' ');
				styledString.append("(", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(iuPackage.getVersion().toString(), StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				IInstallableUnit iu = iuPackage.getIU();
				styledString.append(" from ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(iu.getId(), StyledString.QUALIFIER_STYLER);
				styledString.append(' ');
				styledString.append("(", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(iu.getVersion().toString(), StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			} else if (element instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) element;
				String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				styledString.append(iu.getId());
				styledString.append(' ');
				styledString.append("(", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(iu.getVersion().toString(), StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(" - "); //$NON-NLS-1$
				styledString.append(name, StyledString.DECORATIONS_STYLER);
			}

			return styledString;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof IUPackage) {
				return labelProvider.get(PDEPluginImages.DESC_PACKAGE_OBJ);
			} else if (element instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) element;
				if (QueryUtil.isGroup(iu))
					return labelProvider.get(PDEPluginImages.DESC_FEATURE_OBJ);
				return labelProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			StyledString string = getStyledText(element);
			return string.getString();
		}

		@Override
		public void dispose() {
			labelProvider.disconnect(this);
		}
	}

	public FilteredIUSelectionDialog(Shell shell, IQuery<IInstallableUnit> query) {
		super(shell, true);
		this.query = query;
		setTitle(PDEUIMessages.FilteredIUSelectionDialog_title);
		setMessage(PDEUIMessages.FilteredIUSelectionDialog_message);
		setListLabelProvider(fLabelProvider);
		setDetailsLabelProvider(fLabelProvider);
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		fShowLatestVersionOnlyButton = new Button(composite, SWT.CHECK);
		fShowLatestVersionOnlyButton.setSelection(true);
		fShowLatestVersionOnlyButton.setText(PDEUIMessages.FilteredIUSelectionDialog_showLatestVersionOnly);
		fShowLatestVersionOnlyButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fShowLatestVersionOnly = fShowLatestVersionOnlyButton.getSelection();
			applyFilter();
		}));
		return composite;
	}

	class IUItemsFilter extends ItemsFilter {

		boolean latest = false;

		public IUItemsFilter() {
			latest = fShowLatestVersionOnly;
		}

		@Override
		public boolean matchItem(Object item) {
			if (item instanceof IUPackage)
				return patternMatcher.matches(((IUPackage) item).getId());
			else if (item instanceof IInstallableUnit)
				return isIUMatch((IInstallableUnit) item);

			return false;
		}

		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		@Override
		public boolean isSubFilter(ItemsFilter filter) {
			if (latest != ((IUItemsFilter) filter).latest)
				return false;
			return super.isSubFilter(filter);
		}

		@Override
		public boolean equalsFilter(ItemsFilter obj) {
			if (latest != ((IUItemsFilter) obj).latest)
				return false;
			return super.equals(obj);
		}

		public boolean isIUMatch(IInstallableUnit iu) {
			if (iu.getFragments() != null && !iu.getFragments().isEmpty())
				return false;

			String id = iu.getId();
			String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
			if (name == null || name.startsWith("%")) //$NON-NLS-1$
				name = ""; //$NON-NLS-1$
			if (patternMatcher.matches(id) || patternMatcher.matches(name)) {
				return true;
			}

			return false;
		}
	}

	@Override
	protected ItemsFilter createFilter() {
		return new IUItemsFilter();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		// TODO clean up this code a bit...
		IMetadataRepositoryManager manager = P2TargetUtils.getRepoManager();
		if (manager == null)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_2));

		//URI[] knownRepositories = metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		IQuery<IInstallableUnit> pipedQuery;
		if (fShowLatestVersionOnly)
			pipedQuery = QueryUtil.createPipeQuery(query, QueryUtil.createLatestIUQuery());
		else
			pipedQuery = query;

		Iterator<IInstallableUnit> iter = manager.query(pipedQuery, progressMonitor).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = iter.next();
			Iterator<IProvidedCapability> pcIter = iu.getProvidedCapabilities().iterator();
			while (pcIter.hasNext()) {
				IProvidedCapability pc = pcIter.next();
				if (pc.getNamespace().equals("java.package")) { //$NON-NLS-1$
					IUPackage pkg = new IUPackage(pc.getName(), pc.getVersion(), iu);
					contentProvider.add(pkg, itemsFilter);
				}
			}
			contentProvider.add(iu, itemsFilter);
		}
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return new DialogSettings("org.eclipse.pde.internal.ui.search.dialogs.FilteredTargetRepoIUSelectionDialog"); //$NON-NLS-1$
	}

	@Override
	public String getElementName(Object item) {
		return null;
	}

	@Override
	protected Comparator<Object> getItemsComparator() {
		return (o1, o2) -> {
			String id1 = null;
			String id2 = null;

			if (o1 instanceof IUPackage) {
				id1 = ((IUPackage) o1).getId();
			} else if (o1 instanceof IInstallableUnit) {
				id1 = ((IInstallableUnit) o1).getId();
			} else {
				return 0;
			}

			if (o2 instanceof IUPackage) {
				id2 = ((IUPackage) o2).getId();
			} else if (o2 instanceof IInstallableUnit) {
				id2 = ((IInstallableUnit) o2).getId();
			} else {
				return 0;
			}

			return id1.compareTo(id2);
		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

}
