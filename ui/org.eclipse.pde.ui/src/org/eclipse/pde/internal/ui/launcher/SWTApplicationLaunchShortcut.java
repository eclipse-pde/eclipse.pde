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
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;

public class SWTApplicationLaunchShortcut implements ILaunchShortcut {

	/**
	 * @param search the java elements to search for a main type
	 * @param mode the mode to launch in
	 * @param editor activated on an editor (or from a selection in a viewer)
	 */
	public void searchAndLaunch(Object[] search, String mode, boolean editor) {
		IType[] types = null;
		if (search != null) {
			try {
				IJavaElement[] elements = getJavaElements(search);
				MainMethodSearchEngine engine = new MainMethodSearchEngine();
				IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, false);
				types = engine.searchMainMethods(PlatformUI.getWorkbench().getProgressService(),
						scope, IJavaElementSearchConstants.CONSIDER_BINARIES | IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS,
						true);
			} catch (InterruptedException e) {
				return;
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getShell(), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.failed"), e.getMessage()); //$NON-NLS-1$
				return;
			}
			IType type = null;
			if (types.length == 0) {
				String message = null;
				if (editor) {
					message = PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.noMainInEditor");  //$NON-NLS-1$
				} else {
					message = PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.noMainInSelection"); //$NON-NLS-1$
				}
				MessageDialog.openError(getShell(), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.failed"), message); //$NON-NLS-1$
			} else if (types.length > 1) {
				type = chooseType(types, mode);
			} else {
				type = types[0];
			}
			if (type != null) {
				launch(type, mode);
			}
		}

	}	
	
	/**
	 * Returns the Java elements corresponding to the given objects.
	 * 
	 * @param objects selected objects
	 * @return corresponding Java elements
	 */
	private IJavaElement[] getJavaElements(Object[] objects) {
		ArrayList list= new ArrayList(objects.length);
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof IAdaptable) {
				IJavaElement element = (IJavaElement) ((IAdaptable)object).getAdapter(IJavaElement.class);
				if (element != null) {
					if (element instanceof IMember) {
						// Use the declaring type if available
						IJavaElement type= ((IMember)element).getDeclaringType();
						if (type != null) {
							element= type;
						}
					}
					list.add(element);
				}
			}
		}
		return (IJavaElement[]) list.toArray(new IJavaElement[list.size()]);
	}

	/**
	 * Prompts the user to select a type
	 * 
	 * @return the selected type or <code>null</code> if none.
	 */
	protected IType chooseType(IType[] types, String mode) {
		MainTypeSelectionDialog dialog= new MainTypeSelectionDialog(getShell(), types);		
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setTitle(PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.debug")); //$NON-NLS-1$
		} else {
			dialog.setTitle(PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.run")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			return (IType)dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Launches a configuration for the given type
	 */
	protected void launch(IType type, String mode) {
		ILaunchConfiguration config = findLaunchConfiguration(type, mode);
		if (config != null) {
			DebugUITools.launch(config, mode);
		}			
	}
	
	/**
	 * Locate a configuration to relaunch for the given type.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(IType type, String mode) {
		ILaunchConfigurationType configType = getSWTLaunchConfigType();
		java.util.List candidateConfigs = Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
					if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(type.getJavaProject().getElementName())) { //$NON-NLS-1$
						candidateConfigs.add(config);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		
		// If there are no existing configs associated with the IType, create one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the IType, prompt the
		// user to choose one.
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1) {
			return createConfiguration(type);
		} else if (candidateCount == 1) {
			return (ILaunchConfiguration) candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config.  A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching anything.
			ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
			if (config != null) {
				return config;
			}
		}
		
		return null;
	}
	
	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(java.util.List configList, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.launch"));  //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.chooseRun")); //$NON-NLS-1$
		} else {
			dialog.setMessage(PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.chooseDebug")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;		
	}
	
	/**
	 * Create & return a new configuration based on the specified <code>IType</code>.
	 */
	protected ILaunchConfiguration createConfiguration(IType type) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			ILaunchConfigurationType configType = getSWTLaunchConfigType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(type.getElementName()));
		} catch (CoreException exception) {
			reportCreatingConfiguration(exception);
			return null;		
		} 
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, type.getFullyQualifiedName());
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type.getJavaProject().getElementName());
		try {
			config = wc.doSave();		
		} catch (CoreException exception) {
			reportCreatingConfiguration(exception);			
		}
		return config;
	}
	
	protected void reportCreatingConfiguration(final CoreException exception) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(getShell(), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.error"), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.exception"), exception.getStatus());  //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getSWTLaunchConfigType() {
		return getLaunchManager().getLaunchConfigurationType("org.eclipse.pde.ui.swtLaunchConfig");		 //$NON-NLS-1$
	}
	
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return PDEPlugin.getActiveWorkbenchShell();
	}
	
	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement je = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (je != null) {
			searchAndLaunch(new Object[] {je}, mode, true);
		} else {
			MessageDialog.openError(getShell(), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.failed"), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.noMainInEditor"));   //$NON-NLS-1$//$NON-NLS-2$
		}
		
	}

	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection)selection).toArray(), mode, false);
		} else {
			MessageDialog.openError(getShell(), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.failed"), PDEPlugin.getResourceString("SWTApplicationLaunchShortcut.noMainInSelection"));  //$NON-NLS-1$//$NON-NLS-2$
		}
	}

}