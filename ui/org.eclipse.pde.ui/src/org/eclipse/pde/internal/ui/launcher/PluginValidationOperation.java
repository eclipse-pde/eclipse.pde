/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.MinimalState;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.graphics.Image;


public class PluginValidationOperation implements IRunnableWithProgress {
	
	private static Object[] NO_CHILDREN = new Object[0];
	
	private IPluginModelBase[] fModels;
	private MinimalState fState;
	private ArrayList fInvalidModels = new ArrayList();
	private String fProductID;
	private String fApplicationID;

	class InvalidNode {
		public String toString() {
			if (fInvalidModels.size() > 1)
				return PDEUIMessages.PluginValidationOperation_invalidPlural; 
			return PDEUIMessages.PluginValidationOperation_invalidSingular; 
		}
	}
	
	class MissingCore {
		public String toString() {
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingCore, getCorePluginID()); 
		}
	}
	
	class MissingApplication {
		public String toString() {
			String pluginID = getApplicationPlugin();
			if (getState().getBundles(pluginID).length == 0)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingApp, (new String[] {fApplicationID, pluginID})); 
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingApp2, (new String[] {fApplicationID, pluginID})); 
		}
	}
	
	class MissingProduct {
		public String toString() {
			String pluginID = getProductPlugin();
			if (getState().getBundles(pluginID).length == 0)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingProduct, (new String[] {fProductID, pluginID})); 
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingProduct2, (new String[] {fProductID, pluginID})); 
		}
	}
	
	class ConstraintLabelProvider extends PDELabelProvider {
		
		private Image fImage;

		public ConstraintLabelProvider() {
			PDEPlugin.getDefault().getLabelProvider().connect(this);
			fImage = PDEPluginImages.DESC_ERROR_ST_OBJ.createImage();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof BundleDescription) {
				String id = ((BundleDescription)element).getSymbolicName();
				if (((BundleDescription)element).getHost() != null)
					return NLS.bind(PDEUIMessages.PluginValidationOperation_disableFragment, id); 
				return NLS.bind(PDEUIMessages.PluginValidationOperation_disablePlugin, id); 
			}
			
			if (element instanceof ResolverError)
				return toString((ResolverError)element);
				
			if (element instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)element;
				return model.getPluginBase().getId();
			}
			return element.toString();
		}
			
		private String toString(ResolverError error) {			
			int type = error.getType();
			VersionConstraint constraint = error.getUnsatisfiedConstraint();
			switch (type) {
			case ResolverError.PLATFORM_FILTER:
				String filter = error.getBundle().getPlatformFilter();
				return NLS.bind(PDEUIMessages.PluginValidationOperation_platformFilter, filter);
			case ResolverError.MISSING_EXECUTION_ENVIRONMENT:
				String[] ee = error.getBundle().getExecutionEnvironments();
				return NLS.bind(PDEUIMessages.PluginValidationOperation_ee, ee[0]);
			case ResolverError.SINGLETON_SELECTION:
				return PDEUIMessages.PluginValidationOperation_singleton;
			case ResolverError.IMPORT_PACKAGE_USES_CONFLICT:
			case ResolverError.MISSING_IMPORT_PACKAGE :
				return toString((ImportPackageSpecification)constraint, type);
			case ResolverError.REQUIRE_BUNDLE_USES_CONFLICT:
			case ResolverError.MISSING_REQUIRE_BUNDLE :
				return toString((BundleSpecification)constraint, type);
			case ResolverError.MISSING_FRAGMENT_HOST :
				return toString((HostSpecification)constraint);	
			}
			return error.toString();
		}
		
		private String toString(BundleSpecification spec, int type) {
			String name = spec.getName();
			if (type == ResolverError.REQUIRE_BUNDLE_USES_CONFLICT)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_bundle_uses, spec.getName());
			
			BundleDescription[] bundles = getState().getBundles(name);
			for (int i = 0; i < bundles.length; i++) {
				if (spec.isSatisfiedBy(bundles[i]) && !bundles[i].isResolved())
					return NLS.bind(PDEUIMessages.PluginValidationOperation_disabledRequired, name); 
			}
			if (bundles.length == 0 || spec.getVersionRange().equals(VersionRange.emptyRange))
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingRequired, name); 
			return NLS.bind(PDEUIMessages.PluginValidationOperation_version, 
							new String[] {spec.getVersionRange().toString(), spec.getName()});
		}
		
		private String toString(ImportPackageSpecification spec, int type) {
			if (type == ResolverError.IMPORT_PACKAGE_USES_CONFLICT)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_import_uses, spec.getName());
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingImport, spec.getName()); 
		}
		
		private String toString(HostSpecification spec) {
			String name = spec.getName();
			BundleDescription[] bundles = getState().getBundles(name);
			for (int i = 0; i < bundles.length; i++) {
				if (spec.isSatisfiedBy(bundles[i]) && !bundles[i].isResolved())
					return NLS.bind(PDEUIMessages.PluginValidationOperation_disabledParent, name); 
			}
			if (bundles.length == 0 || spec.getVersionRange().equals(VersionRange.emptyRange))
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingParent, name); 
			return NLS.bind(PDEUIMessages.PluginValidationOperation_hostVersion, 
							new String[] {spec.getVersionRange().toString(), spec.getName()});
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof IPluginModelBase)
				return super.getImage(element);
			return fImage;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		public void dispose() {
			fImage.dispose();
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		}
	}
	
	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parent) {
			if (parent instanceof BundleDescription)
				return getState().getResolverErrors((BundleDescription)parent);
				
			return (parent instanceof InvalidNode) ? fInvalidModels.toArray() : NO_CHILDREN;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			return element instanceof BundleDescription || element instanceof InvalidNode;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			ArrayList result = new ArrayList();
			BundleDescription[] all = getState().getBundles();
			for (int i = 0; i < all.length; i++) {
				if (!all[i].isResolved())
					result.add(all[i]);
			}
			if (isProductMissing())
				result.add(new MissingProduct());
			if (isApplicationMissing())
				result.add(new MissingApplication());
			if (isCoreMissing())
				result.add(new MissingCore());
			if (fInvalidModels.size() > 0)
				result.add(new InvalidNode());
			return result.toArray();
		}
	}
	
	public PluginValidationOperation(IPluginModelBase[] models) {
		this(models, null, null);
	}
	
	public PluginValidationOperation(IPluginModelBase[] models, String product, String application) {
		fModels = models;
		fProductID = product;
		fApplicationID = application;
		fState = new MinimalState();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		for (int i = 0; i < fModels.length; i++) {
			if (fState.addBundle(fModels[i], -1) == null)
				fInvalidModels.add(fModels[i]);
		}
		fState.resolveState(false);
	}
	
	public State getState() {
		return fState.getState();
	}
	
	public boolean hasErrors() {
		State state = getState();
		if (fInvalidModels.size() > 0 || state.getBundles().length > state.getResolvedBundles().length)
			return true;	
		return isApplicationMissing() || isProductMissing();	
	}
	
	private boolean isProductMissing() {
		if (fProductID == null)
			return false;
		
		BundleDescription[] desc = getState().getBundles(getProductPlugin());
		for (int i = 0; i < desc.length; i++) {
			if (desc[i].isResolved()) 
				return false;
		}
		return true;
	}
	private boolean isApplicationMissing() {
		if (fApplicationID == null)
			return false;
		BundleDescription[] desc = getState().getBundles(getApplicationPlugin());
		for (int i = 0; i < desc.length; i++) {
			if (desc[i].isResolved()) 
				return false;
		}
		return true;
	}
	
	private String getProductPlugin() {
		return fProductID.substring(0, fProductID.lastIndexOf('.'));
	}
	
	private String getApplicationPlugin() {
		return fApplicationID.substring(0, fApplicationID.lastIndexOf('.'));
	}
	
	private boolean isCoreMissing() {
		return (getState().getBundles(getCorePluginID()).length == 0);
	}
	
	private String getCorePluginID() {
		return PDECore.getDefault().getModelManager().isOSGiRuntime() ? "org.eclipse.osgi" : "org.eclipse.core.boot"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public IContentProvider getContentProvider() {
		return new ContentProvider();
	}
	
	public ILabelProvider getLabelProvider() {
		return new ConstraintLabelProvider();
	}

}