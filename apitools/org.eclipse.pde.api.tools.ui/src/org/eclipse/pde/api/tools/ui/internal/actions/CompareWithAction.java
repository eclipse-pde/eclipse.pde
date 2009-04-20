/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.actions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ITreeModel;
import org.eclipse.pde.api.tools.internal.provisional.ITreeNode;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.ibm.icu.text.DateFormat;

public class CompareWithAction implements IObjectActionDelegate {

	public static class DeltaSession implements ISession {
		static Object[] NO_CHILDREN = new Object[0];

		static class TreeNode implements ITreeNode, IPropertySource, IAdaptable {
			private static final IPropertyDescriptor[] NO_PROPERTY_DESCRIPTORS = new IPropertyDescriptor[0];
			// Property id keys
			public static final String ID_MESSAGE = "IDelta.Message"; //$NON-NLS-1$
			public static final String ID_COMPONENT = "IDelta.Component"; //$NON-NLS-1$
			public static final String ID_ELEMENT_TYPE = "IDelta.ElementType"; //$NON-NLS-1$
			public static final String ID_FLAGS = "IDelta.Flags"; //$NON-NLS-1$
			private static final Object ID_KEY = "IDelta.Key"; //$NON-NLS-1$
			private static final Object ID_KIND = "IDelta.Kind"; //$NON-NLS-1$
			private static final Object ID_NEW_MODIFIERS = "IDelta.NewModifiers"; //$NON-NLS-1$
			private static final Object ID_OLD_MODIFIERS = "IDelta.OldModifiers"; //$NON-NLS-1$
			private static final Object ID_RESTRICTIONS = "IDelta.Restrictions"; //$NON-NLS-1$
			private static final Object ID_TYPENAME = "IDelta.TypeName"; //$NON-NLS-1$

			// categories
			public static final String P_MESSAGE_CATEGORY = ActionMessages.MessageCategory;
			public static final String P_INFO_CATEGORY = ActionMessages.InfoCategory;

			public static final String P_MESSAGE = ActionMessages.PropertyMessageKey;
			public static final String P_COMPONENT = ActionMessages.PropertyComponentKey;
			public static final String P_ELEMENT_TYPE = ActionMessages.PropertyElementTypeKey;
			public static final String P_FLAGS = ActionMessages.PropertyFlagsKey;
			public static final String P_KEY = ActionMessages.PropertyKeyKey;
			public static final String P_KIND = ActionMessages.PropertyKindKey;
			public static final String P_NEW_MODIFIERS = ActionMessages.PropertyNewModifiersKey;
			public static final String P_OLD_MODIFIERS = ActionMessages.PropertyOldModifiersKey;
			public static final String P_RESTRICTIONS = ActionMessages.PropertyRestrictionsKey;
			public static final String P_TYPENAME = ActionMessages.PropertyTypeNameKey;

			private static List Descriptors;
			static {
				Descriptors = new ArrayList();

				PropertyDescriptor propertyDescriptor = new TextPropertyDescriptor(ID_MESSAGE, P_MESSAGE);
				propertyDescriptor.setCategory(P_MESSAGE_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_COMPONENT, P_COMPONENT);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_KEY, P_KEY);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				propertyDescriptor.setFilterFlags(new String[] { IPropertySheetEntry.FILTER_ID_EXPERT });
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_KIND, P_KIND);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_NEW_MODIFIERS, P_NEW_MODIFIERS);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_OLD_MODIFIERS, P_OLD_MODIFIERS);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_RESTRICTIONS, P_RESTRICTIONS);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_TYPENAME, P_TYPENAME);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);

				propertyDescriptor = new TextPropertyDescriptor(ID_FLAGS, P_FLAGS);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);
				
				propertyDescriptor = new TextPropertyDescriptor(ID_ELEMENT_TYPE, P_ELEMENT_TYPE);
				propertyDescriptor.setCategory(P_INFO_CATEGORY);
				Descriptors.add(propertyDescriptor);
			}
			/**
			 * Returns the descriptors
			 */
			static List getDescriptors() {
				return Descriptors;
			}
			Map children;
			String name;
			TreeNode parent;
			Object data;
			int id;

			public TreeNode(int id, String name, Object data) {
				this.name = name;
				this.id = id;
				this.data = data;
			}
			public Object[] getChildren() {
				if (this.children == null) {
					return NO_CHILDREN;
				}
				return this.children.values().toArray(new Object[this.children.size()]);
			}
			public TreeNode getNode(String name) {
				if (this.children == null) {
					return null;
				}
				return (TreeNode) this.children.get(name);
			}
			public int getId() {
				return this.id;
			}
			public void add(TreeNode node) {
				if (this.children == null) {
					this.children = new HashMap();
				}
				this.children.put(node.name, node);
			}
			public boolean hasChildren() {
				return this.children != null && !this.children.isEmpty();
			}
			public String toString() {
				return String.valueOf(this.name);
			}
			public Object getData() {
				return this.data;
			}
			public Object getEditableValue() {
				return null;
			}
			public IPropertyDescriptor[] getPropertyDescriptors() {
				if (this.data != null) {
					return (IPropertyDescriptor[]) getDescriptors().toArray(
							new IPropertyDescriptor[getDescriptors().size()]);
				}
				return NO_PROPERTY_DESCRIPTORS;
			}
			public Object getPropertyValue(Object propKey) {
				if (this.data == null) return null;
				IDelta delta = (IDelta) this.data;
				if (ID_MESSAGE.equals(propKey)) {
					return delta.getMessage();
				}
				if (ID_COMPONENT.equals(propKey)) {
					return delta.getComponentVersionId();
				}
				if (ID_ELEMENT_TYPE.equals(propKey)) {
					int elementType = delta.getElementType();
					StringBuffer buffer = new StringBuffer(Util.getDeltaElementType(elementType));
					buffer.append(" (").append(elementType).append(')'); //$NON-NLS-1$
					return String.valueOf(buffer);
				}
				if (ID_FLAGS.equals(propKey)) {
					int flags = delta.getFlags();
					StringBuffer buffer = new StringBuffer(Util.getDeltaFlagsName(flags));
					buffer.append(" (").append(flags).append(')'); //$NON-NLS-1$
					return String.valueOf(buffer);
				}
				if (ID_KEY.equals(propKey)) {
					return delta.getKey();
				}
				if (ID_KIND.equals(propKey)) {
					int kind = delta.getKind();
					StringBuffer buffer = new StringBuffer(Util.getDeltaKindName(kind));
					buffer.append(" (").append(kind).append(')'); //$NON-NLS-1$
					return String.valueOf(buffer);
				}
				if (ID_NEW_MODIFIERS.equals(propKey)) {
					return getDisplayedModifiers(delta.getNewModifiers());
				}
				if (ID_OLD_MODIFIERS.equals(propKey)) {
					return getDisplayedModifiers(delta.getOldModifiers());
					}
				if (ID_RESTRICTIONS.equals(propKey)) {
					int restrictions = delta.getRestrictions();
					StringBuffer buffer = new StringBuffer(RestrictionModifiers.getRestrictionText(restrictions));
					buffer.append(" (0x").append(Integer.toHexString(restrictions)).append(')'); //$NON-NLS-1$
					return String.valueOf(buffer);
				}
				if (ID_TYPENAME.equals(propKey)) {
					return delta.getTypeName();
				}
				return null;
			}
			public boolean isPropertySet(Object id) {
				return false;
			}
			public void resetPropertyValue(Object id) {
				// nothing to do
			}
			public void setPropertyValue(Object id, Object value) {
				// nothing to do
			}
			/* (non-Javadoc)
			 * Method declared on IAdaptable
			 */
			public Object getAdapter(Class adapter) {
				if (adapter == IPropertySource.class) {
					return this;
				}
				return null;
			}
			private static String getDisplayedModifiers(int newModifiers) {
				StringBuffer buffer = new StringBuffer();
				if(newModifiers == 0) {
					buffer.append(ActionMessages.PropertyPackageVisibility);
				} else {
					if (Flags.isAbstract(newModifiers)) {
						buffer.append("abstract"); //$NON-NLS-1$
					}
					String separator = " | "; //$NON-NLS-1$
					if(Flags.isFinal(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("final"); //$NON-NLS-1$
					}
					if(Flags.isNative(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("native"); //$NON-NLS-1$
					}
					if(Flags.isPrivate(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("private"); //$NON-NLS-1$
					}
					if(Flags.isProtected(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("protected"); //$NON-NLS-1$
					}
					if(Flags.isPublic(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("public"); //$NON-NLS-1$
					}
					if(Flags.isStatic(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("static"); //$NON-NLS-1$
					}
					if(Flags.isStrictfp(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("strictfp"); //$NON-NLS-1$
					}
					if(Flags.isSynchronized(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("synchronized"); //$NON-NLS-1$
					}
					if(Flags.isTransient(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("transient"); //$NON-NLS-1$
					}
					if(Flags.isVolatile(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("volatile"); //$NON-NLS-1$
					}
					if(Flags.isVarargs(newModifiers)) {
						if(buffer.length() > 0) {
							buffer.append(separator);
						}
						buffer.append("vargars"); //$NON-NLS-1$
					}
				}
				buffer.append(" (0x").append(Integer.toHexString(newModifiers)).append(')'); //$NON-NLS-1$
				return buffer.toString();
			}
		}

		static class TreeModel implements ITreeModel, IAdaptable {
			TreeNode root;

			TreeModel(TreeNode root) {
				this.root = root;
			}
			public ITreeNode getRoot() {
				return this.root;
			}
			public Object getAdapter(Class adapter) {
				// TODO Auto-generated method stub
				return null;
			}
		}

		IDelta delta;
		String baselineName;
		String timestamp;
		
		public DeltaSession(IDelta delta, String baselineName) {
			this.delta = delta;
			this.baselineName = baselineName;
			this.timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date(System.currentTimeMillis()));
		}
		public ITreeModel getModel() {
			TreeNode root = new TreeNode(0, null, this.delta);
			TreeModel model = new TreeModel(root);
			class TreeBuilder extends DeltaVisitor {
				TreeNode node;
				
				public TreeBuilder(TreeNode node) {
					this.node = node;
				}
				public void endVisit(IDelta delta) {
					if (delta.getChildren().length == 0) {
						String typeName = delta.getTypeName();
						if (typeName == null) {
							this.node.add(new TreeNode(0, delta.getKey(), delta));
						} else if (typeName.length() == 0) {
							this.node.add(new TreeNode(0, delta.getMessage(), delta));
						} else {
							// split the type name (package - type)
							int index = typeName.lastIndexOf('.');
							String packageName = "<default package>"; //$NON-NLS-1$
							String actualTypeName = null;
							if (index != -1) {
								packageName = typeName.substring(0, index);
								actualTypeName = typeName.substring(index + 1);
							} else {
								actualTypeName = typeName;
							}
							TreeNode node2 = this.node.getNode(packageName);
							if (node2 == null) {
								node2 = new TreeNode(ITreeNode.PACKAGE, packageName, null);
								this.node.add(node2);
							}
							TreeNode node3 = node2.getNode(actualTypeName);
							if (node3 == null) {
								int id = 0;
								switch(delta.getElementType()) {
									case IDelta.ANNOTATION_ELEMENT_TYPE :
										id = ITreeNode.ANNOTATION;
										break;
									case IDelta.INTERFACE_ELEMENT_TYPE :
										id = ITreeNode.INTERFACE;
										break;
									case IDelta.CLASS_ELEMENT_TYPE :
										id = ITreeNode.CLASS;
										break;
									case IDelta.ENUM_ELEMENT_TYPE :
										id = ITreeNode.ENUM;
									default :
										// we need to retrieve the type kind
										try {
											String componentVersionId = delta.getComponentVersionId();
											if (componentVersionId != null) {
												int indexOfOpen = componentVersionId.lastIndexOf('(');
												String componentID = componentVersionId.substring(0, indexOfOpen);
												String version = componentVersionId.substring(indexOfOpen + 1, componentVersionId.length() - 1);
												IApiBaseline baseline = ApiBaselineManager.getManager().getApiBaseline(DeltaSession.this.baselineName);
												int modifiers = 0;
												if (baseline != null) {
													IApiComponent apiComponent = baseline.getApiComponent(componentID);
													if (apiComponent != null && version.equals(apiComponent.getVersion())) {
														IApiTypeRoot typeRoot = apiComponent.findTypeRoot(typeName);
														if (typeRoot != null) {
															IApiType structure = typeRoot.getStructure();
															modifiers = structure.getModifiers();
														}
													}
												}
												if (modifiers == 0) {
													// try the workspace baseline
													baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
													if (baseline != null) {
														IApiComponent apiComponent = baseline.getApiComponent(componentID);
														if (apiComponent != null && version.equals(apiComponent.getVersion())) {
															IApiTypeRoot typeRoot = apiComponent.findTypeRoot(typeName);
															if (typeRoot != null) {
																IApiType structure = typeRoot.getStructure();
																modifiers = structure.getModifiers();
															}
														}
													}
												}
												if (Flags.isEnum(modifiers)) {
													id = ITreeNode.ENUM;
												} else if (Flags.isAnnotation(modifiers)) {
													id = ITreeNode.ANNOTATION;
												} else if (Flags.isInterface(modifiers)) {
													id = ITreeNode.INTERFACE;
												} else {
													id = ITreeNode.CLASS;
												}
											}
										} catch (CoreException e) {
											// ignore
										}
								}
								node3 = new TreeNode(id, actualTypeName, null);
								node2.add(node3);
							}
							node3.add(new TreeNode(0, delta.getMessage(), delta));
						}
					}
				}
			}
			if (this.delta == ApiComparator.NO_DELTA) {
				root.add(new TreeNode(0, ActionMessages.CompareTaskNoChanges, null));
			} else {
				this.delta.accept(new TreeBuilder(root));
			}
			return model;
		}
		
		public String getTimestamp() {
			return this.timestamp;
		}
	}

	private IWorkbenchPartSite workbenchPartSite;
	private ISelection selection = null;
	
	/**
	 * Constructor for Action1.
	 */
	public CompareWithAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		workbenchPartSite = targetPart.getSite();
	}
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (this.selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection=(IStructuredSelection) this.selection;
			CompareDialog dialog = new CompareDialog(workbenchPartSite, ActionMessages.CompareDialogTitle);
			int returnCode = dialog.open();
			if (returnCode == Window.CANCEL) return;
			final String baselineName = dialog.baseline;
			if (baselineName == null) return;
			final IApiBaseline baseline = ApiBaselineManager.getManager().getApiBaseline(baselineName);
			if (baseline == null) {
				return;
			}
			Job job = new Job(ActionMessages.CompareWithAction_comparing_apis){
				protected IStatus run(IProgressMonitor monitor) {
					SubMonitor progress = SubMonitor.convert(monitor, 100);
					progress.subTask(ActionMessages.CompareDialogCollectingElementTaskName);
					SubMonitor loopProgress = progress.newChild(10).setWorkRemaining(structuredSelection.size());
					final IApiScope scope = walkStructureSelection(structuredSelection, loopProgress);
					try {
						progress.subTask(ActionMessages.CompareDialogComputeDeltasTaskName);
						SubMonitor compareProgress = progress.newChild(98).setWorkRemaining(scope.getApiElements().length);
						try {
							IDelta delta = ApiComparator.compare(scope, baseline, VisibilityModifiers.API, false, compareProgress);
							ApiPlugin.getDefault().getSessionManager().addSession(new DeltaSession(delta, baselineName), true);
							progress.worked(1);
							return Status.OK_STATUS;
						} catch (CoreException e) {
							ApiPlugin.log(e);
						} catch(OperationCanceledException e) {
							// ignore
						}
					} finally {
						monitor.done();
					}
					return Status.CANCEL_STATUS;
				}
			};
			job.setSystem(false);
			job.setPriority(Job.LONG);
			job.schedule();
			return;
		}
	}

	public static ApiScope walkStructureSelection(
			IStructuredSelection structuredSelection,
			IProgressMonitor monitor) {
		Object[] selected=structuredSelection.toArray();
		ApiScope scope = new ApiScope();
		IApiBaseline workspaceBaseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		if (workspaceBaseline == null) {
			return scope;
		}
		for (int i=0, max = selected.length; i < max; i++) {
			Object currentSelection = selected[i];
			if (currentSelection instanceof IJavaElement) {
				monitor.worked(1);
				IJavaElement element =(IJavaElement) currentSelection;
				IJavaProject javaProject = element.getJavaProject();
				try {
					switch (element.getElementType()) {
						case IJavaElement.COMPILATION_UNIT: {
							ICompilationUnit compilationUnit = (ICompilationUnit) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								addElementFor(compilationUnit, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.PACKAGE_FRAGMENT: {
							IPackageFragment fragment = (IPackageFragment) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) fragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
							boolean isArchive = false;
							if (packageFragmentRoot != null) {
								isArchive = packageFragmentRoot.isArchive();
							}
							if (apiComponent != null) {
								addElementFor(fragment, isArchive, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
							IPackageFragmentRoot fragmentRoot = (IPackageFragmentRoot) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								addElementFor(fragmentRoot, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.JAVA_PROJECT:
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								scope.add(apiComponent);
//								IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
//								for (int j = 0, max2 = roots.length; j < max2; j++) {
//									addElementFor(roots[j], apiComponent, scope);
//								}
							}
							break;
					}
				} catch (JavaModelException e) {
					ApiPlugin.log(e);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return scope;
	}

	private static void addElementFor(
			IPackageFragmentRoot fragmentRoot, IApiComponent apiComponent,
			ApiScope scope) throws JavaModelException, CoreException {
		boolean isArchive = fragmentRoot.isArchive();
		IJavaElement[] packageFragments = fragmentRoot.getChildren();
		for (int j = 0, max2 = packageFragments.length; j < max2; j++) {
			IPackageFragment packageFragment = (IPackageFragment) packageFragments[j];
			addElementFor(packageFragment, isArchive, apiComponent, scope);
		}
	}

	private static void addElementFor(
			IPackageFragment packageFragment,
			boolean isArchive,
			IApiComponent apiComponent,
			ApiScope scope)
		throws JavaModelException, CoreException {

		// add package fragment elements only if this is an API package
		IApiDescription apiDescription = apiComponent.getApiDescription();
		IApiAnnotations annotations = apiDescription.resolveAnnotations(Factory.packageDescriptor(packageFragment.getElementName()));
		if (annotations == null || !VisibilityModifiers.isAPI(annotations.getVisibility())) {
			return;
		}
		if (isArchive) {
			IClassFile[] classFiles = packageFragment.getClassFiles();
			for (int i = 0, max= classFiles.length; i < max; i++) {
				addElementFor(classFiles[i], apiComponent, scope);
			}
		} else {
			ICompilationUnit[] units = packageFragment.getCompilationUnits();
			for (int i = 0, max= units.length; i < max; i++) {
				addElementFor(units[i], apiComponent, scope);
			}
		}
	}

	private static void addElementFor(IClassFile classFile,
			IApiComponent apiComponent, ApiScope scope) {
		try {
			IApiTypeRoot typeRoot = apiComponent.findTypeRoot(classFile.getType().getFullyQualifiedName());
			if (typeRoot != null) {
				scope.add(typeRoot);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	private static void addElementFor(ICompilationUnit compilationUnit, IApiComponent component, ApiScope scope) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		for (int i = 0, max = types.length; i < max; i++) {
			try {
				IApiTypeRoot typeRoot = component.findTypeRoot(types[i].getFullyQualifiedName());
				if (typeRoot != null) {
					scope.add(typeRoot);
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}
