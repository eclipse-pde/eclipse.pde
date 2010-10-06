/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import org.eclipse.jdt.core.Flags;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ITreeModel;
import org.eclipse.pde.api.tools.internal.provisional.ITreeNode;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.ibm.icu.text.DateFormat;

public class DeltaSession implements ISession {
	static Object[] NO_CHILDREN = new Object[0];

	static class TreeNode implements ITreeNode, IPropertySource2, IAdaptable {
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
		private static final Object ID_CURRENT_RESTRICTIONS = "IDelta.CurrentRestrictions"; //$NON-NLS-1$
		private static final Object ID_PREVIOUS_RESTRICTIONS = "IDelta.PreviousRestrictions"; //$NON-NLS-1$
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
		public static final String P_CURRENT_RESTRICTIONS = ActionMessages.PropertyCurrentRestrictionsKey;
		public static final String P_PREVIOUS_RESTRICTIONS = ActionMessages.PropertyPreviousRestrictionsKey;
		public static final String P_TYPENAME = ActionMessages.PropertyTypeNameKey;

		private static List Descriptors;
		static {
			Descriptors = new ArrayList();

			PropertyDescriptor propertyDescriptor = new PropertyDescriptor(ID_MESSAGE, P_MESSAGE);
			propertyDescriptor.setCategory(P_MESSAGE_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_COMPONENT, P_COMPONENT);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_KEY, P_KEY);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			propertyDescriptor.setFilterFlags(new String[] { IPropertySheetEntry.FILTER_ID_EXPERT });
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_KIND, P_KIND);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_NEW_MODIFIERS, P_NEW_MODIFIERS);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_OLD_MODIFIERS, P_OLD_MODIFIERS);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_CURRENT_RESTRICTIONS, P_CURRENT_RESTRICTIONS);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_PREVIOUS_RESTRICTIONS, P_PREVIOUS_RESTRICTIONS);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_TYPENAME, P_TYPENAME);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);

			propertyDescriptor = new PropertyDescriptor(ID_FLAGS, P_FLAGS);
			propertyDescriptor.setCategory(P_INFO_CATEGORY);
			Descriptors.add(propertyDescriptor);
			
			propertyDescriptor = new PropertyDescriptor(ID_ELEMENT_TYPE, P_ELEMENT_TYPE);
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
		DeltaSession.TreeNode parent;
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
		public DeltaSession.TreeNode getNode(String name) {
			if (this.children == null) {
				return null;
			}
			return (DeltaSession.TreeNode) this.children.get(name);
		}
		public int getId() {
			return this.id;
		}
		public void add(DeltaSession.TreeNode node) {
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
			return this;
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
			if (ID_CURRENT_RESTRICTIONS.equals(propKey)) {
				int restrictions = delta.getCurrentRestrictions();
				return getDisplayRestrictions(restrictions);
			}
			if (ID_PREVIOUS_RESTRICTIONS.equals(propKey)) {
				int restrictions = delta.getPreviousRestrictions();
				return getDisplayRestrictions(restrictions);
			}
			if (ID_TYPENAME.equals(propKey)) {
				return delta.getTypeName();
			}
			return null;
		}
		private Object getDisplayRestrictions(int restrictions) {
			StringBuffer buffer = new StringBuffer(RestrictionModifiers.getRestrictionText(restrictions));
			buffer.append(" (0x").append(Integer.toHexString(restrictions)).append(')'); //$NON-NLS-1$
			return String.valueOf(buffer);
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
		public boolean isPropertyResettable(Object id) {
			return false;
		}
	}

	static class TreeModel implements ITreeModel {
		DeltaSession.TreeNode root;

		TreeModel(DeltaSession.TreeNode root) {
			this.root = root;
		}
		public ITreeNode getRoot() {
			return this.root;
		}
	}

	IDelta delta;
	String baselineName;
	String timestamp;
	String description;

	public DeltaSession(String description, IDelta delta, String baselineName) {
		this.delta = delta;
		this.baselineName = baselineName;
		this.timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date(System.currentTimeMillis()));
		this.description = description;
	}
	public ITreeModel getModel() {
		DeltaSession.TreeNode root = new TreeNode(0, null, this.delta);
		DeltaSession.TreeModel model = new TreeModel(root);
		class TreeBuilder extends DeltaVisitor {
			DeltaSession.TreeNode node;
			
			public TreeBuilder(DeltaSession.TreeNode node) {
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
						DeltaSession.TreeNode node2 = this.node.getNode(packageName);
						if (node2 == null) {
							node2 = new TreeNode(ITreeNode.PACKAGE, packageName, null);
							this.node.add(node2);
						}
						DeltaSession.TreeNode node3 = node2.getNode(actualTypeName);
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
											int modifiers = retrieveTypeModifiers(
													delta,
													typeName,
													componentVersionId);
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
			private int retrieveTypeModifiers(IDelta delta,
					String typeName,
					String componentVersionId) throws CoreException {
				int indexOfOpen = componentVersionId.lastIndexOf('(');
				String componentID = componentVersionId.substring(0, indexOfOpen);
				IApiBaseline baseline = ApiBaselineManager.getManager().getApiBaseline(baselineName);
				int modifiers = 0;
				if (baseline != null) {
					IApiComponent apiComponent = baseline.getApiComponent(componentID);
					int kind = delta.getKind();
					if (apiComponent != null && (kind == IDelta.REMOVED)) {
						// need to handle reexported types
						IApiTypeRoot typeRoot = null;
						String id = apiComponent.getSymbolicName();
						switch(delta.getFlags()) {
							case IDelta.REEXPORTED_TYPE :
							case IDelta.REEXPORTED_API_TYPE :
								// handle re-exported types
								// check if the type is provided by a required component (it could have been moved/re-exported)
								String packageName = Util.EMPTY_STRING;
								int indexOf = typeName.lastIndexOf('.');
								if (indexOf != -1) {
									packageName = typeName.substring(0, indexOf);
								}
								IApiComponent[] providers = apiComponent.getBaseline().resolvePackage(apiComponent, packageName);
								int index = 0;
								while (typeRoot == null && index < providers.length) {
									IApiComponent p = providers[index];
									if (!p.equals(apiComponent)) {
										String id2 = p.getSymbolicName();
										if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
											typeRoot = p.findTypeRoot(typeName);
										} else {
											typeRoot = p.findTypeRoot(typeName, id2);
										}
									}
									index++;
								}
								break;
							default :
								if (Util.ORG_ECLIPSE_SWT.equals(id)) {
									typeRoot = apiComponent.findTypeRoot(typeName);
								} else{
									typeRoot = apiComponent.findTypeRoot(typeName, id);
								}
						}
						if (typeRoot != null) {
							IApiType structure = typeRoot.getStructure();
							if(structure != null) {
								modifiers = structure.getModifiers();
							}
						}
					}
				}
				if (modifiers == 0) {
					// try the workspace baseline
					baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
					if (baseline != null) {
						IApiComponent apiComponent = baseline.getApiComponent(componentID);
						if (apiComponent != null) {
							IApiTypeRoot typeRoot = null;
							String id = apiComponent.getSymbolicName();
							switch(delta.getFlags()) {
								case IDelta.REEXPORTED_TYPE :
								case IDelta.REEXPORTED_API_TYPE :
									// handle re-exported types
									// check if the type is provided by a required component (it could have been moved/re-exported)
									String packageName = Util.EMPTY_STRING;
									int indexOf = typeName.lastIndexOf('.');
									if (indexOf != -1) {
										packageName = typeName.substring(0, indexOf);
									}
									IApiComponent[] providers = apiComponent.getBaseline().resolvePackage(apiComponent, packageName);
									int index = 0;
									while (typeRoot == null && index < providers.length) {
										IApiComponent p = providers[index];
										if (!p.equals(apiComponent)) {
											String id2 = p.getSymbolicName();
											if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
												typeRoot = p.findTypeRoot(typeName);
											} else {
												typeRoot = p.findTypeRoot(typeName, id2);
											}
										}
										index++;
									}
									break;
								default :
									if (Util.ORG_ECLIPSE_SWT.equals(id)) {
										typeRoot = apiComponent.findTypeRoot(typeName);
									} else{
										typeRoot = apiComponent.findTypeRoot(typeName, id);
									}
							}
							if (typeRoot != null) {
								IApiType structure = typeRoot.getStructure();
								modifiers = structure.getModifiers();
							}
						}
					}
				}
				return modifiers;
			}
		}
		if (this.delta == ApiComparator.NO_DELTA) {
			root.add(new TreeNode(0, ActionMessages.CompareTaskNoChanges, null));
		} else {
			this.delta.accept(new TreeBuilder(root));
		}
		return model;
	}
	
	public String getDescription() {
		return NLS.bind(ActionMessages.SessionDescription, new String[] { this.timestamp, this.description });
	}
}