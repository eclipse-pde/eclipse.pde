/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.ListIterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeContentProposalListener;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeContentProposalProvider;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeProposalLabelProvider;
import org.eclipse.pde.internal.ui.editor.contentassist.display.JavaDocCommentReader;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;
import org.eclipse.pde.internal.ui.editor.text.HTMLPrinter;
import org.eclipse.pde.internal.ui.editor.text.TextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.ide.IDE;

public class PDEJavaHelper {
	
	private static HashMap fDocMap = new HashMap();
	
	public static String selectType(IResource resource, int scope) {
		if (resource == null) return null;
		IProject project = resource.getProject();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					PlatformUI.getWorkbench().getProgressService(),
					getSearchScope(project),
					scope, 
			        false, ""); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle); 
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
		}
		return null;
	}
	
	public static String selectType(IResource resource, int scope, String filter) {
		if (resource == null) return null;
		IProject project = resource.getProject();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					PlatformUI.getWorkbench().getProgressService(),
					getSearchScope(project),
					scope, 
			        false, filter); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle); 
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
		}
		return null;
	}	
	
	public static IJavaSearchScope getSearchScope(IProject project) {
		return SearchEngine.createJavaSearchScope(getNonJRERoots(JavaCore.create(project)));
	}

	public static IPackageFragmentRoot[] getNonJRERoots(IJavaProject project) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (!isJRELibrary(roots[i])) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}	
	
	public static boolean isJRELibrary(IPackageFragmentRoot root) {
		try {
			IPath path = root.getRawClasspathEntry().getPath();
			if (path.equals(new Path(JavaRuntime.JRE_CONTAINER))
					|| path.equals(new Path(JavaRuntime.JRELIB_VARIABLE))) {
				return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}	
	
	/**
	 * Open/Create a java class
	 * 
	 * @param name fully qualified java classname
	 * @param project
	 * @param value for creation of the class
	 * @param createIfNoNature will create the class even if the project has no java nature
	 * @return null if the class exists or the name of the newly created class
	 */
	public static String createClass(String name, IProject project, JavaAttributeValue value, boolean createIfNoNature) {
		name = trimNonAlphaChars(name).replace('$', '.');
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = null;
				if (name.length() > 0)
					result = javaProject.findType(name);
				if (result != null)
					JavaUI.openInEditor(result);
				else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK)
						return wizard.getQualifiedNameWithArgs();
				}
			} else if (createIfNoNature) {
				IResource resource = project.findMember(new Path(name));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				} else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						String newValue = wizard.getQualifiedName();
						name = newValue.replace('.', '/') + ".java"; //$NON-NLS-1$
						resource = project.findMember(new Path(name));
						if (resource != null && resource instanceof IFile) {
							IWorkbenchPage page = PDEPlugin.getActivePage();
							IDE.openEditor(page, (IFile) resource, true);
						}
						return newValue;
					}
				}
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}
	
	public static String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}
	
	
	/**
	 * @param packageName - the name of the package
	 * @param pluginID - the id of the containing plug-in - can be null if <code>project</code> is not null
	 * @param project - if null will search for an external package fragment, otherwise will search in project
	 * @return
	 */
    public static IPackageFragment getPackageFragment(String packageName, String pluginID, IProject project) {
		if (project == null) 
			return getExternalPackageFragment(packageName, pluginID);
		
		IJavaProject jp = JavaCore.create(project);
		if (jp != null)
			try {
				IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					IPackageFragment frag = roots[i].getPackageFragment(packageName);
					if (frag.exists()) {
						return frag;
					}
				}
			} catch (JavaModelException e) {
			}
		return null;
    }
    
    private static IPackageFragment getExternalPackageFragment(String packageName, String pluginID) {
    	if (pluginID == null)
    		return null;
    	IPluginModelBase base = null;
    	try {
    		IPluginModelBase plugin = PluginRegistry.findModel(pluginID);
    		if (plugin == null)
    			return null;
    		ImportPackageSpecification[] packages = plugin.getBundleDescription().getImportPackages();
    		for (int i =0; i < packages.length; i++)
    			if (packages[i].getName().equals(packageName)) {
    				ExportPackageDescription desc = (ExportPackageDescription) packages[i].getSupplier();
    				base = PluginRegistry.findModel(desc.getExporter().getSymbolicName());
    				break;
    			}
    		if (base == null)
    			return null;
    		IResource res = base.getUnderlyingResource();
    		if (res != null) {
    			IJavaProject jp = JavaCore.create(res.getProject());
    			if (jp != null)
    				try {
    					IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
    					for (int i = 0; i < roots.length; i++) {
    						IPackageFragment frag = roots[i].getPackageFragment(packageName);
    						if (frag.exists()) 
    							return frag;
    					}
    				} catch (JavaModelException e) {
    				}
    		}
			IProject proj = PDEPlugin.getWorkspace().getRoot().getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
			if (proj == null)
				return searchWorkspaceForPackage(packageName, base);
			IJavaProject jp = JavaCore.create(proj);
			IPath path = new Path(base.getInstallLocation());
			// if model is in jar form
			if (!path.toFile().isDirectory()) {
				IPackageFragmentRoot root = jp.findPackageFragmentRoot(path);
				if (root != null) {
					IPackageFragment frag = root.getPackageFragment(packageName);
					if (frag.exists())
						return frag;
				}
			// else model is in folder form, try to find model's libraries on filesystem
			} else {
				IPluginLibrary[] libs = base.getPluginBase().getLibraries();
				for (int i = 0; i < libs.length; i++) {
					if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
						continue;
					String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
					IPackageFragmentRoot root = jp.findPackageFragmentRoot(path.append(libName));
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			}
		} catch (JavaModelException e){
		}
		return searchWorkspaceForPackage(packageName, base);
    }
    
    private static IPackageFragment searchWorkspaceForPackage(String packageName, IPluginModelBase base) {
    	IPluginLibrary[] libs = base.getPluginBase().getLibraries();
    	ArrayList libPaths = new ArrayList();
    	IPath path = new Path(base.getInstallLocation());
    	if (libs.length == 0) {
    		libPaths.add(path);
    	}
		for (int i = 0; i < libs.length; i++) {
			if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
				continue;
			String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
			libPaths.add(path.append(libName));
		}
		IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			try {
				if(!projects[i].hasNature(JavaCore.NATURE_ID) || !projects[i].isOpen())
					continue;
				IJavaProject jp = JavaCore.create(projects[i]);
				ListIterator li = libPaths.listIterator();
				while (li.hasNext()) {
					IPackageFragmentRoot root = jp.findPackageFragmentRoot((IPath)li.next());
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			} catch (CoreException e) {
			}
		}
		return null;
    }
    
    public static IPackageFragment[] getPackageFragments (IJavaProject jProject, Collection existingPackages, boolean allowJava) {
		HashMap map = new HashMap();
		try {
			IPackageFragmentRoot[] roots = getRoots(jProject);
			for (int i = 0; i < roots.length; i++) {
				IJavaElement[] children = roots[i].getChildren();
				for (int j = 0; j < children.length; j++) {
					IPackageFragment fragment = (IPackageFragment)children[j];
					String name = fragment.getElementName();
					if (name.length() == 0)
						name = "."; //$NON-NLS-1$
					if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0 )&& !existingPackages.contains(name)) {
						if (!name.equals("java") || !name.startsWith("java.") || allowJava) //$NON-NLS-1$ //$NON-NLS-2$
							map.put(fragment.getElementName(), fragment);
					}
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragment[]) map.values().toArray(new IPackageFragment[map.size()]);
	}
	
	private static IPackageFragmentRoot[] getRoots(IJavaProject jProject) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| jProject.getProject().equals(roots[i].getCorrespondingResource())
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[])result.toArray(new IPackageFragmentRoot[result.size()]);	
	}
	
	public static String getOSGIConstantJavaDoc(String constant, IJavaProject jp) {
		return getJavaDoc(constant, jp, "org.osgi.framework.Constants"); //$NON-NLS-1$
	}
	
	public static String getJavaDoc(String constant, IJavaProject jp, String className) {
		HashMap map = (HashMap)fDocMap.get(className);
		if (map == null)
			fDocMap.put(className, map = new HashMap());
		String javaDoc = (String)map.get(constant);
		
		if (javaDoc == null) {
			try {
				IType type = jp.findType(className); 
				if (type != null) {
					char[] chars = constant.toCharArray();
					for (int i = 0; i < chars.length; i++)
						chars[i] = chars[i] == '-' ? '_' : Character.toUpperCase(chars[i]);
					IField field = type.getField(new String(chars));
					ISourceRange range = field.getJavadocRange();
					if (range == null)
						return null;
					IBuffer buff = type.getOpenable().getBuffer();
					JavaDocCommentReader reader = new JavaDocCommentReader(buff, range.getOffset(), 
							range.getOffset() + range.getLength() - 1);
					String text = getString(reader);
					javaDoc = formatJavaDoc(text);
					map.put(constant, javaDoc);
				}
			} catch (JavaModelException e) {
			}
		}
		return javaDoc;
	}

	private static String formatJavaDoc(String text) {
		StringBuffer buffer = new StringBuffer();
		HTMLPrinter.insertPageProlog(buffer, 0, TextUtil.getJavaDocStyleSheerURL());
		buffer.append(text);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/**
	 * Gets the reader content as a String
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}
	
	/**
	 * Disposer returned used to dispose of label provider and remove listeners
	 * Callers responsibility to call dispose method when underlying text 
	 * widget is being disposed
	 * @param text
	 * @param project
	 * @return
	 */
	public static TypeFieldAssistDisposer addTypeFieldAssistToText(
			Text text, IProject project, int searchScope) {
		// Decorate the text widget with the light-bulb image denoting content
		// assist
		int bits = SWT.TOP | SWT.LEFT;
		ControlDecoration controlDecoration = new ControlDecoration(text, bits);
		// Configure text widget decoration
		// No margin
		controlDecoration.setMarginWidth(0);
		// Custom hover tip text
		controlDecoration.setDescriptionText(
				PDEUIMessages.PDEJavaHelper_msgContentAssistAvailable);
		// Custom hover properties
		controlDecoration.setShowHover(true);
		controlDecoration.setShowOnlyOnFocus(true);
		// Hover image to use
		FieldDecoration contentProposalImage = 
			FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);			
		controlDecoration.setImage(contentProposalImage.getImage());
		
		// Create the proposal provider
		TypeContentProposalProvider proposalProvider = 
			new TypeContentProposalProvider(project, 
					searchScope);
		// Default text widget adapter for field assist
		TextContentAdapter textContentAdapter = new TextContentAdapter();
		// Content assist command
		String command = "org.eclipse.ui.edit.text.contentAssist.proposals"; //$NON-NLS-1$
		// Set auto activation character to be a '.'
		char[] autoActivationChars = new char[]{TypeContentProposalProvider.F_DOT};
		// Create the adapter
		ContentAssistCommandAdapter adapter = 
			new ContentAssistCommandAdapter(
					text, 
					textContentAdapter, 
					proposalProvider, 
					command, 
					autoActivationChars);	
		// Configure the adapter
		// Add label provider
		ILabelProvider labelProvider = new TypeProposalLabelProvider();		
		adapter.setLabelProvider(labelProvider);
		// Replace text field contents with accepted proposals
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		// Disable default filtering - custom filtering done
		adapter.setFilterStyle(ContentProposalAdapter.FILTER_NONE);
		// Add listeners required to reset state for custom filtering
		TypeContentProposalListener proposalListener = 
			new TypeContentProposalListener();		
		adapter.addContentProposalListener((IContentProposalListener)proposalListener);	
		adapter.addContentProposalListener((IContentProposalListener2)proposalListener);
		
		return new TypeFieldAssistDisposer(adapter, proposalListener);
	}

	/**
	 * @param point
	 * @param fModel
	 * @return
	 */
	public static String getFullId(IPluginExtensionPoint point,
			IPluginModelBase model) {
		
		if ((point instanceof PluginExtensionPointNode) &&
				(model != null)) {
			String pointId = point.getId();
			if ("3.2".equals(model.getPluginBase().getSchemaVersion()) && pointId.indexOf('.') > 0) //$NON-NLS-1$
				return pointId;
			String id = null;
			if (model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel)model).getFragment();
				if (fragment != null)
					id = fragment.getPluginId();
			}
			if (id == null)
				id = model.getPluginBase().getId();
			return id + '.' + pointId;
		}
		return point.getFullId();
	}	
	
	/**
	 * @param project
	 * @return
	 */
	public static String getJavaSourceLevel(IProject project) {
		return getJavaLevel(project, JavaCore.COMPILER_SOURCE);	
	}
	
	/**
	 * @param project
	 * @return
	 */
	public static String getJavaComplianceLevel(IProject project) {
		return getJavaLevel(project, JavaCore.COMPILER_COMPLIANCE);		
	}	
	
	/**
	 * Precedence order from high to low:  (1) Project specific option;
	 * (2) General preference option; (3) Default option; (4) Java 1.3
	 * @param project
	 * @param optionName
	 * @return
	 */
	public static String getJavaLevel(IProject project, String optionName) {
		// Returns the corresponding java project
		// No need to check for null, will return null		
		IJavaProject javaProject = JavaCore.create(project);
		String value = null;
		// Preferred to use the project
		if ((javaProject != null) && 
				javaProject.exists()) {
			// Get the project specific option if one exists. Rolls up to the 
			// general preference option if no project specific option exists.
			value = javaProject.getOption(optionName, true);
			if (value != null) {
				return value;
			}
		} 
		// Get the general preference option
		value = JavaCore.getJavaCore().getPluginPreferences().getString(optionName);
		if (value != null) {
			return value;
		}
		// Get the default option
		value = JavaCore.getOption(optionName);
		if (value != null) {
			return value;
		}
		// Return the default
		return JavaCore.VERSION_1_3;
	}
	
}