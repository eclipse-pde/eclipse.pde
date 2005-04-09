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

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.graphics.*;


public class PluginValidationOperation implements IRunnableWithProgress {
	
	private IPluginModelBase[] fModels;
	private PDEState fState;
	private ArrayList fInvalidModels = new ArrayList();
	private String fProductID;
	private String fApplicationID;

	class InvalidNode {
		public String toString() {
			if (fInvalidModels.size() > 1)
				return PDEUIMessages.PluginValidationOperation_invalidPlural; //$NON-NLS-1$
			return PDEUIMessages.PluginValidationOperation_invalidSingular; //$NON-NLS-1$
		}
	}
	
	class MissingCore {
		public String toString() {
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingCore, getCorePluginID()); //$NON-NLS-1$
		}
	}
	
	class MissingApplication {
		public String toString() {
			String pluginID = getApplicationPlugin();
			if (getState().getBundles(pluginID).length == 0)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingApp, (new String[] {fApplicationID, pluginID})); //$NON-NLS-1$
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingApp2, (new String[] {fApplicationID, pluginID})); //$NON-NLS-1$
		}
	}
	
	class MissingProduct {
		public String toString() {
			String pluginID = getProductPlugin();
			if (getState().getBundles(pluginID).length == 0)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingProduct, (new String[] {fProductID, pluginID})); //$NON-NLS-1$
			return NLS.bind(PDEUIMessages.PluginValidationOperation_missingProduct2, (new String[] {fProductID, pluginID})); //$NON-NLS-1$
		}
	}
	
	class ConstraintLabelProvider extends PDELabelProvider {
		
		private Image fImage;
		private Image fInfo;

		public ConstraintLabelProvider() {
			PDEPlugin.getDefault().getLabelProvider().connect(this);
			fImage = PDEPluginImages.DESC_ERROR_ST_OBJ.createImage();
			fInfo = PDEPluginImages.DESC_INFO_ST_OBJ.createImage();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof BundleDescription) {
				String id = ((BundleDescription)element).getSymbolicName();
				if (((BundleDescription)element).getHost() != null)
					return NLS.bind(PDEUIMessages.PluginValidationOperation_disableFragment, id); //$NON-NLS-1$
				return NLS.bind(PDEUIMessages.PluginValidationOperation_disablePlugin, id); //$NON-NLS-1$
			}
			if (element instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)element;
				return model.getPluginBase().getId();
			}
			return element.toString();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof IPluginModelBase)
				return super.getImage(element);
			/*if (element instanceof String)
				return fInfo;*/
			return fImage;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		public void dispose() {
			fImage.dispose();
			fInfo.dispose();
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		}
	}
	
	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parent) {
			ArrayList list = new ArrayList();
			if (parent instanceof BundleDescription) {
				StateHelper helper = PDECore.getDefault().acquirePlatform().getStateHelper();		
				VersionConstraint[] unsatisfiedConstraints = helper.getUnsatisfiedConstraints((BundleDescription)parent);
				for (int i = 0; i < unsatisfiedConstraints.length; i++) {
					list.add(toString(unsatisfiedConstraints[i]));
				}
			} else if (parent instanceof InvalidNode) {
				return fInvalidModels.toArray();
			}
			return list.toArray();
		}
		
		private String toString(VersionConstraint constraint) {
			State state = getState();
			String name = constraint.getName();
			if (constraint instanceof BundleSpecification) {
				if (state.getBundles(name).length == 0)
					return NLS.bind(PDEUIMessages.PluginValidationOperation_missingRequired, name); //$NON-NLS-1$
				return NLS.bind(PDEUIMessages.PluginValidationOperation_disabledRequired, name); //$NON-NLS-1$
			}
			if (constraint instanceof ImportPackageSpecification)
				return NLS.bind(PDEUIMessages.PluginValidationOperation_missingImport, name); //$NON-NLS-1$
			if (constraint instanceof HostSpecification)  {
				if (state.getBundles(name).length == 0)
					return NLS.bind(PDEUIMessages.PluginValidationOperation_missingParent, name); //$NON-NLS-1$
				return NLS.bind(PDEUIMessages.PluginValidationOperation_disabledParent, name); //$NON-NLS-1$
			}
			return name;
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
			if (isProductMissing())
				result.add(new MissingProduct());
			if (isApplicationMissing())
				result.add(new MissingApplication());
			if (isCoreMissing())
				result.add(new MissingCore());
			if (fInvalidModels.size() > 0)
				result.add(new InvalidNode());
			BundleDescription[] all = getState().getBundles();
			for (int i = 0; i < all.length; i++) {
				if (!all[i].isResolved())
					result.add(all[i]);
			}
			return result.toArray();
		}
	}
	public PluginValidationOperation(IPluginModelBase[] models, String product, String application) {
		super();
		fModels = models;
		fProductID = product;
		fApplicationID = application;
		fState = new PDEState();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		for (int i = 0; i < fModels.length; i++) {
			BundleDescription desc = fState.addBundle(fModels[i]);
			if (desc == null)
				fInvalidModels.add(fModels[i]);
		}
		fState.resolveState();
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