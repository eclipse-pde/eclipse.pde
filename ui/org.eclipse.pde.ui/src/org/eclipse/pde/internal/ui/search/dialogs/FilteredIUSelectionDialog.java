/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import java.util.Comparator;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.Messages;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class FilteredIUSelectionDialog extends FilteredItemsSelectionDialog {

	private Button fShowLatestVersionOnlyButton;
	private boolean fShowLatestVersionOnly = true;
	private final IQuery query;
	private final ILabelProvider fLabelProvider = new IUWrapperLabelProvider();

//	private static final String S_PLUGINS = "showPlugins"; //$NON-NLS-1$
//	private static final String S_FEATURES = "showFeatures"; //$NON-NLS-1$
//	private static final String S_PACKAGES = "showPackages"; //$NON-NLS-1$
//
//	private static final int TYPE_PLUGIN = 0;
//	private static final int TYPE_FEATURE = 1;
//	private static final int TYPE_PACKAGE = 2;

	private class IUWrapperLabelProvider extends LabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {

		private PDELabelProvider labelProvider = PDEPlugin.getDefault().getLabelProvider();

		public StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();

			if (element instanceof IUPackage) {
				IUPackage iuPackage = (IUPackage) element;
				styledString.append(iuPackage.getId());
				styledString.append(' ');
				styledString.append("(", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styledString.append(iuPackage.getVersion().toString(), StyledString.QUALIFIER_STYLER);
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

		public String getText(Object element) {
			StyledString string = getStyledText(element);
			return string.getString();
		}
	}

	public FilteredIUSelectionDialog(Shell shell, IQuery query) {
		super(shell, true);
		this.query = query;
		setTitle(PDEUIMessages.FilteredIUSelectionDialog_title);
		setMessage(PDEUIMessages.FilteredIUSelectionDialog_message);
		setListLabelProvider(fLabelProvider);
		setDetailsLabelProvider(fLabelProvider);
	}

	protected Control createExtendedContentArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		fShowLatestVersionOnlyButton = new Button(composite, SWT.CHECK);
		fShowLatestVersionOnlyButton.setSelection(true);
		fShowLatestVersionOnlyButton.setText(PDEUIMessages.FilteredIUSelectionDialog_showLatestVersionOnly);
		fShowLatestVersionOnlyButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fShowLatestVersionOnly = fShowLatestVersionOnlyButton.getSelection();
				applyFilter();
			}
		});
		return composite;
	}

	class IUItemsFilter extends ItemsFilter {

		boolean latest = false;

		public IUItemsFilter() {
			latest = fShowLatestVersionOnly;
		}

		public boolean matchItem(Object item) {
			if (item instanceof IUPackage)
				return patternMatcher.matches(((IUPackage) item).getId());
			else if (item instanceof IInstallableUnit)
				return isIUMatch((IInstallableUnit) item);

			return false;
		}

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean isSubFilter(ItemsFilter filter) {
			if (latest != ((IUItemsFilter) filter).latest)
				return false;
			return super.isSubFilter(filter);
		}

		public boolean equalsFilter(ItemsFilter obj) {
			if (latest != ((IUItemsFilter) obj).latest)
				return false;
			return super.equals(obj);
		}

		public boolean isIUMatch(IInstallableUnit iu) {
			if (iu.getFragments() != null && iu.getFragments().size() > 0)
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	protected ItemsFilter createFilter() {
		return new IUItemsFilter();
	}

	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		// TODO clean up this code a bit...
		IProvisioningAgent agent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_7));
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);

		//URI[] knownRepositories = metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		IQuery pipedQuery;
		if (fShowLatestVersionOnly)
			pipedQuery = QueryUtil.createPipeQuery(query, QueryUtil.createLatestIUQuery());
		else
			pipedQuery = query;

		Iterator iter = manager.query(pipedQuery, progressMonitor).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			Iterator pcIter = iu.getProvidedCapabilities().iterator();
			while (pcIter.hasNext()) {
				IProvidedCapability pc = (IProvidedCapability) pcIter.next();
				if (pc.getNamespace().equals("java.package")) { //$NON-NLS-1$
					IUPackage pkg = new IUPackage(iu, pc.getName(), pc.getVersion());
					contentProvider.add(pkg, itemsFilter);
				}
			}
			contentProvider.add(iu, itemsFilter);
		}
	}

	protected IDialogSettings getDialogSettings() {
		return new DialogSettings("org.eclipse.pde.internal.ui.search.dialogs.FilteredTargetRepoIUSelectionDialog"); //$NON-NLS-1$
	}

	public String getElementName(Object item) {
		// TODO Auto-generated method stub

		return null;
	}

	protected Comparator getItemsComparator() {
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((IVersionedId) o1).getId().compareTo(((IVersionedId) o2).getId());
			}
		};
	}

	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

//	private ITargetHandle[] getAvailableTargets() {
//		List names = new ArrayList();
//		ITargetHandle[] targetHandles = new ITargetHandle[0];
//		ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
//		if (service != null) {
//			ITargetHandle[] handles = service.getTargets(null);
//			List defs = new ArrayList();
//			for (int i = 0; i < handles.length; i++) {
//				try {
//					defs.add(handles[i].getTargetDefinition());
//				} catch (CoreException e) {
//					// Suppress
//				}
//			}
//			Collections.sort(defs, new Comparator() {
//				public int compare(Object o1, Object o2) {
//					ITargetDefinition d1 = (ITargetDefinition) o1;
//					ITargetDefinition d2 = (ITargetDefinition) o2;
//					return d1.getName().compareTo(d2.getName());
//				}
//			});
//			targetHandles = new ITargetHandle[defs.size()];
//			for (int i = 0; i < defs.size(); i++) {
//				ITargetDefinition def = (ITargetDefinition) defs.get(i);
//				targetHandles[i] = def.getHandle();
//				names.add(def.getName());
//			}
//		}
//		return targetHandles;
//	}

}
