/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;

/**
 * Visitor for class files
 *
 * @since 1.0.0
 */
public class ClassFileVisitor extends ClassAdapter {

	/**
	 * Constant used for controlling tracing in the visitor
	 */
	private static boolean DEBUG = Util.DEBUG;

	/**
	 * Method used for initializing tracing in the visitor
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	/**
	 * A visitor for visiting java 5+ signatures
	 * TODO this visitor does not currently visit annotations
	 *
	 * ClassSignature = (visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitSuperClass visitInterface* )
	 * MethodSignature = (visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitParameterType visitReturnType visitExceptionType* )
	 * TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | (visitClassType visitTypeArgument* (visitInnerClassType visitTypeArgument* )* visitEnd</tt> ) )
	 */
	class ClassFileSignatureVisitor implements SignatureVisitor {

		protected int kind = -1;
		protected int originalkind = -1;
		protected int argumentcount = 0;
		protected int type = 0;
		protected String signature = null;
		protected String name = null;
		protected List references;

		public ClassFileSignatureVisitor() {
			this.references = new ArrayList();
		}

		/**
		 * Resets the visitor to its initial state.
		 * This method should be called after processing is done with the visitor
		 */
		protected void reset() {
			//do not reset argument count, as it is needed once the signature visitor is done
			this.kind = -1;
			this.originalkind = -1;
			this.name = null;
			this.signature = null;
			this.type = 0;
			this.references.clear();
		}

		/**
		 * Processes the type specified by the name for the current signature context.
		 * The kind flag is set to a parameterized type as subsequent calls to this method without visiting other nodes only occurs
		 * when we are processing parameterized types of generic declarations
		 * @param name the name of the type
		 */
		protected void processType(String name) {
			Type type = ClassFileVisitor.this.resolveType(Type.getObjectType(name).getDescriptor());
			if(type != null) {
				String tname = type.getClassName();
				if(tname.equals("E") || tname.equals("T")) {  //$NON-NLS-1$//$NON-NLS-2$
					type = Type.getObjectType("java.lang.Object"); //$NON-NLS-1$
					tname = type.getClassName();
				}
				if(ClassFileVisitor.this.consider(tname) && this.kind != -1) {
					if(this.name != null && this.signature != null) {
						IReferenceTypeDescriptor target = Util.getType(tname);
						target = target.getPackage().getType(target.getName(), this.signature);
						this.references.add(new Reference(new Location(ClassFileVisitor.this.fComponent, ClassFileVisitor.this.getMember()),
								new Location(null, target),
								this.kind));
					}
				}
			}
			this.kind = this.originalkind;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitClassType(java.lang.String)
		 */
		public void visitClassType(String name) {
			this.processType(name);
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitFormalTypeParameter(java.lang.String)
		 */
		public void visitFormalTypeParameter(String name) {
			if(this.type != TYPE) {
				this.processType(name);
			}
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitTypeVariable(java.lang.String)
		 */
		public void visitTypeVariable(String name) {
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitInnerClassType(java.lang.String)
		 */
		public void visitInnerClassType(String name) {
			this.processType(name);
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitParameterType()
		 */
		public SignatureVisitor visitParameterType() {
			this.argumentcount++;
			this.kind = ReferenceModifiers.REF_PARAMETER;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitInterface()
		 */
		public SignatureVisitor visitInterface() {
			this.kind = ReferenceModifiers.REF_IMPLEMENTS;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitExceptionType()
		 */
		public SignatureVisitor visitExceptionType() {
			this.kind = ReferenceModifiers.REF_THROWS;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitArrayType()
		 */
		public SignatureVisitor visitArrayType() {
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitReturnType()
		 */
		public SignatureVisitor visitReturnType() {
			this.kind = ReferenceModifiers.REF_RETURNTYPE;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitClassBound()
		 */
		public SignatureVisitor visitClassBound() {
			this.kind = ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitInterfaceBound()
		 */
		public SignatureVisitor visitInterfaceBound() {
			this.kind = ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitSuperclass()
		 */
		public SignatureVisitor visitSuperclass() {
			this.kind = ReferenceModifiers.REF_EXTENDS;
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitTypeArgument(char)
		 */
		public SignatureVisitor visitTypeArgument(char wildcard) {
			return this;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.signature.SignatureVisitor#visitEnd()
		 */
		public void visitEnd() {}

		public void visitBaseType(char descriptor) {
			switch(descriptor) {
				case 'J' :
				case 'D' :
					argumentcount += 2;
					break;
				default :
					this.argumentcount++;
			}
		}
		public void visitTypeArgument() {}
	}

	/**
	 * Visitor used to visit the methods of a type
	 * [ visitCode ( visitFrame | visit<i>X</i>Insn | visitLabel | visitTryCatchBlock | visitLocalVariable | visitLineNumber)* visitMaxs ] visitEnd
	 */
	class ClassFileMethodVisitor extends MethodAdapter {
		int argumentcount = 0;
		LinePositionTracker linePositionTracker;
		/**
		 * Most recent string literal encountered. Used to infer Class.forName("...") references.
		 */
		String stringLiteral;
		String methodName;
		int lastLineNumber;
		boolean implicitConstructor = false;
		LocalLineNumberMarker localVariableMarker;

		HashMap labelsToLocalMarkers;

		/**
		 * Constructor
		 * @param mv
		 */
		public ClassFileMethodVisitor(MethodVisitor mv, String name, int argumentcount) {
			super(mv);
			this.argumentcount = argumentcount;
			this.linePositionTracker = new LinePositionTracker();
			this.lastLineNumber = -1;
			this.labelsToLocalMarkers = new HashMap();
			this.methodName = name;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitEnd()
		 */
		public void visitEnd() {
			this.implicitConstructor = false;
			this.argumentcount = 0;
			ClassFileVisitor.this.exitMember();
			this.linePositionTracker.computeLineNumbers();
			this.labelsToLocalMarkers = null;
		}
		
		public void visitVarInsn(int opcode, int var) {
			switch(opcode) {
				case Opcodes.ASTORE :
					if (this.lastLineNumber != -1) {
						this.localVariableMarker = new LocalLineNumberMarker(this.lastLineNumber, var);
					}
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if(opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD || opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
				IReference reference = ClassFileVisitor.this.addFieldReference(Type.getObjectType(owner), name, (opcode == Opcodes.PUTFIELD ? ReferenceModifiers.REF_PUTFIELD : ReferenceModifiers.REF_PUTSTATIC));
				if (reference != null) {
					this.linePositionTracker.addLocation(reference.getSourceLocation());
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitTryCatchBlock(org.objectweb.asm.Label, org.objectweb.asm.Label, org.objectweb.asm.Label, java.lang.String)
		 */
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			if(type != null) {
				Type ctype = Type.getObjectType(type);
				IReference reference = ClassFileVisitor.this.addTypeReference(ctype, ReferenceModifiers.REF_CATCHEXCEPTION);
				if (reference != null) {
					ILocation sourceLocation = reference.getSourceLocation();
					this.linePositionTracker.addCatchLabelInfos(sourceLocation, handler);
					this.linePositionTracker.addLocation(sourceLocation);
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLabel(Label)
		 */
		public void visitLabel(Label label) {
			this.linePositionTracker.addLabel(label);
			if (this.localVariableMarker != null) {
				this.localVariableMarker.label = label;
				Object object = this.labelsToLocalMarkers.get(label);
				if (object != null) {
					// add in the list
					if (object instanceof List) {
						((List) object).add(this.localVariableMarker);
					} else {
						List list = new ArrayList();
						list.add(object);
						list.add(this.localVariableMarker);
						this.labelsToLocalMarkers.put(label, list);
					}
				} else {
					this.labelsToLocalMarkers.put(label, this.localVariableMarker);
				}
				this.localVariableMarker = null;
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			Type declaringType = Type.getObjectType(owner);
			int kind = -1;
			switch(opcode){
				case Opcodes.INVOKESPECIAL: {
					kind = ("<init>".equals(name) ? ReferenceModifiers.REF_CONSTRUCTORMETHOD : ReferenceModifiers.REF_SPECIALMETHOD); //$NON-NLS-1$
					if (kind == ReferenceModifiers.REF_CONSTRUCTORMETHOD) {
						if(!implicitConstructor && this.methodName.equals("<init>") && !fSuperStack.isEmpty() && ((IReferenceTypeDescriptor)fSuperStack.peek()).getQualifiedName().equals(declaringType.getClassName())) { //$NON-NLS-1$
							implicitConstructor = true;
							kind = ReferenceModifiers.REF_SUPER_CONSTRUCTORMETHOD;
						}
						else {
							IReference reference = ClassFileVisitor.this.addTypeReference(declaringType, ReferenceModifiers.REF_INSTANTIATE);
							if (reference != null) {
								this.linePositionTracker.addLocation(reference.getSourceLocation());
							}
						}
					}
					break;
				}
				case Opcodes.INVOKESTATIC: {
					kind = ReferenceModifiers.REF_STATICMETHOD;
					// check for reference to a class literal
					if (name.equals("forName")) { //$NON-NLS-1$
						if (ClassFileVisitor.this.processName(owner).equals("java.lang.Class")) { //$NON-NLS-1$
							if (this.stringLiteral != null) {
								Type classLiteral = Type.getObjectType(this.stringLiteral);
								IReference reference = ClassFileVisitor.this.addTypeReference(classLiteral, ReferenceModifiers.REF_CONSTANTPOOL);
								if (reference != null) {
									this.linePositionTracker.addLocation(reference.getSourceLocation());
								}
							}
						}
					}
					break;
				}
				case Opcodes.INVOKEVIRTUAL: {
					kind = ReferenceModifiers.REF_VIRTUALMETHOD;
					break;
				}
				case Opcodes.INVOKEINTERFACE: {
					kind = ReferenceModifiers.REF_INTERFACEMETHOD;
					break;
				}
			}
			if(kind != -1) {
				IReference reference = ClassFileVisitor.this.addMethodReference(declaringType, name, desc, kind);
				if (reference != null) {
					this.linePositionTracker.addLocation(reference.getSourceLocation());
				}
			}
			this.stringLiteral = null;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitMultiANewArrayInsn(java.lang.String, int)
		 */
		public void visitMultiANewArrayInsn(String desc, int dims) {
			Type type = this.getTypeFromDescription(desc);
			IReference reference = ClassFileVisitor.this.addTypeReference(type, ReferenceModifiers.REF_ARRAYALLOC);
			if (reference != null) {
				this.linePositionTracker.addLocation(reference.getSourceLocation());
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLineNumber(int, org.objectweb.asm.Label)
		 */
		public void visitLineNumber(int line, Label start) {
			this.lastLineNumber = line;
			this.linePositionTracker.addLineInfo(line, start);
		}

		/**
		 * Creates a type from a type description. Works around bugs creating
		 * types from array type signatures in ASM.
		 *
		 * @param desc signature
		 * @return Type
		 */
		private Type getTypeFromDescription(String desc) {
			while (desc.charAt(0) == '[') {
				desc = desc.substring(1);
			}
			Type type = null;
			if (desc.length() == 1 && Signature.getTypeSignatureKind(desc) == Signature.BASE_TYPE_SIGNATURE) {
				type = Type.getType(desc);
			} else {
				if (desc.endsWith(";")) { //$NON-NLS-1$
					type = Type.getType(desc);
				} else {
					type = Type.getObjectType(desc);
				}
			}
			return type;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitTypeInsn(int, java.lang.String)
		 */
		public void visitTypeInsn(int opcode, String desc) {
			Type type = this.getTypeFromDescription(desc);
			if(type.getSort() == Type.OBJECT) {
				int kind = -1;
				//we can omit the NEW case as it is caught by the constructor call
				switch(opcode) {
					case Opcodes.ANEWARRAY: {
						kind = ReferenceModifiers.REF_ARRAYALLOC;
						break;
					}
					case Opcodes.CHECKCAST: {
						kind = ReferenceModifiers.REF_CHECKCAST;
						break;
					}
					case Opcodes.INSTANCEOF: {
						kind = ReferenceModifiers.REF_INSTANCEOF;
						break;
					}
				}
				if(kind != -1) {
					IReference reference = ClassFileVisitor.this.addTypeReference(type, kind);
					if (reference != null) {
						this.linePositionTracker.addLocation(reference.getSourceLocation());
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLocalVariable(java.lang.String, java.lang.String, java.lang.String, org.objectweb.asm.Label, org.objectweb.asm.Label, int)
		 */
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			if (desc.length() == 1) {
				// base type
				return;
			}
			if(index > this.argumentcount) {
				Object object = this.labelsToLocalMarkers.get(start);
				int lineNumber = -1;
				if (object != null) {
					if (object instanceof List) {
						// list of potential localMarker
						// iterate the list to find the one that matches the index
						List markersList = (List) object;
						LocalLineNumberMarker removeMarker = null;
						loop: for(Iterator iterator = markersList.iterator(); iterator.hasNext(); ) {
							LocalLineNumberMarker marker = (LocalLineNumberMarker) iterator.next();
							if (marker.varIndex == index) {
								lineNumber = marker.lineNumber;
								removeMarker = marker;
								break loop;
							}
						}
						if (removeMarker != null) {
							markersList.remove(removeMarker);
							if (markersList.isEmpty()) {
								this.labelsToLocalMarkers.remove(start);
							}
						}
					} else {
						// single marker
						LocalLineNumberMarker marker = (LocalLineNumberMarker) object;
						if (marker.varIndex == index) {
							lineNumber = marker.lineNumber;
							this.labelsToLocalMarkers.remove(start);
						}
					}
				}
				if (lineNumber == -1) return;
				if(signature != null) {
					List references = ClassFileVisitor.this.processSignature(name, signature, ReferenceModifiers.REF_PARAMETERIZED_VARIABLE, METHOD);
					for (Iterator iterator = references.iterator(); iterator.hasNext();) {
						IReference reference = (IReference) iterator.next();
						ILocation sourceLocation = reference.getSourceLocation();
						sourceLocation.setLineNumber(lineNumber);
						ILocation targetLocation = reference.getReferencedLocation();
						targetLocation.setLineNumber(lineNumber);
					}
				} else {
					Type type = Type.getType(desc);
					if(type.getSort() == Type.OBJECT) {
						IReference reference = ClassFileVisitor.this.addTypeReference(type, ReferenceModifiers.REF_LOCALVARIABLEDECL);
						if (reference != null) {
							ILocation sourceLocation = reference.getSourceLocation();
							sourceLocation.setLineNumber(lineNumber);
						}
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLdcInsn(java.lang.Object)
		 */
		public void visitLdcInsn(Object cst) {
			if(cst instanceof Type) {
				Type type = (Type) cst;
				IReference reference = ClassFileVisitor.this.addTypeReference(type, ReferenceModifiers.REF_CONSTANTPOOL);
				if (reference != null) {
					this.linePositionTracker.addLocation(reference.getSourceLocation());
				}
			} else if (cst instanceof String) {
				this.stringLiteral = (String) cst;
			}
		}
		
		
	}

	static class LinePositionTracker {
		List labelsAndLocations;
		SortedSet lineInfos;
		List catchLabelInfos;
		HashMap lineMap;

		public LinePositionTracker() {
			this.labelsAndLocations = new ArrayList();
			this.lineInfos = new TreeSet();
			this.catchLabelInfos = new ArrayList();
			this.lineMap = new HashMap();
		}

		void addLocation(ILocation location) {
			this.labelsAndLocations.add(location);
		}

		void addLineInfo(int line, Label label) {
			this.lineInfos.add(new LineInfo(line, label));
			this.lineMap.put(label, new Integer(line));
		}

		void addCatchLabelInfos(ILocation location, Label label) {
			this.catchLabelInfos.add(new LabelInfo(location, label));
		}

		void addLabel(Label label) {
			this.labelsAndLocations.add(label);
		}
		
		public void computeLineNumbers() {

			if (this.lineInfos.size() < 1 || this.labelsAndLocations.size() < 1) {
				// nothing to do
				return;
			}
			Iterator lineInfosIterator = this.lineInfos.iterator();
			LineInfo firstLineInfo = (LineInfo) lineInfosIterator.next();
			int currentLineNumber = firstLineInfo.line;

			List remainingCatchLabelInfos = new ArrayList();
			for (Iterator iterator = this.catchLabelInfos.iterator(); iterator.hasNext();) {
				LabelInfo catchLabelInfo = (LabelInfo) iterator.next();
				Integer lineValue = (Integer) this.lineMap.get(catchLabelInfo.label);
				if (lineValue != null) {
					catchLabelInfo.location.setLineNumber(lineValue.intValue());
				} else {
					remainingCatchLabelInfos.add(catchLabelInfo);
				}
			}
			// Iterate over List of Labels and SourceLocations.
			List computedEntries = new ArrayList();
			for (Iterator iterator = this.labelsAndLocations.iterator(); iterator.hasNext();) {
				Object current = iterator.next();
				if (current instanceof Label) {
					// label
					Integer lineValue = (Integer) this.lineMap.get(current);
					if (lineValue != null) {
						computedEntries.add(new LineInfo(lineValue.intValue(), (Label) current));
					} else {
						computedEntries.add(current);
					}
				} else {
					// location
					computedEntries.add(current);
				}
			}
			List remaingEntriesTemp;
			for (Iterator iterator = computedEntries.iterator(); iterator.hasNext();) {
				Object current = iterator.next();
				if (current instanceof Label) {
					// try to set the line number for remaining catch labels
					if (remainingCatchLabelInfos != null) {
						remaingEntriesTemp = new ArrayList();
						loop: for (Iterator catchLabelInfosIterator = remainingCatchLabelInfos.iterator(); catchLabelInfosIterator.hasNext();) {
							LabelInfo catchLabelInfo = (LabelInfo) catchLabelInfosIterator.next();
							if (!current.equals(catchLabelInfo.label)) {
								remaingEntriesTemp.add(catchLabelInfo);
								continue loop;
							}
							catchLabelInfo.location.setLineNumber(currentLineNumber);
						}
						if (remaingEntriesTemp.size() == 0) {
							remainingCatchLabelInfos = null;
						} else {
							remainingCatchLabelInfos = remaingEntriesTemp;
						}
					}
				} else if (current instanceof ILocation) {
					ILocation location = (ILocation) current;
					if (location.getLineNumber() == -1) {
						((ILocation) current).setLineNumber(currentLineNumber);
					} else {
						currentLineNumber = location.getLineNumber();
					}
				} else if (current instanceof LineInfo) {
					LineInfo lineInfo = (LineInfo) current;
					currentLineNumber = lineInfo.line;
				}
			}
		}
	}

	static class LabelInfo {
		public ILocation location;
		public Label label;

		public LabelInfo(ILocation location, Label label) {
			this.location = location;
			this.label = label;
		}

		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append('(').append(this.label).append(',').append(this.location).append(')');
			return String.valueOf(buffer);
		}
	}

	static class LineInfo implements Comparable {
		int line;
		Label label;

		LineInfo(int line, Label label) {
			this.line = line;
			this.label = label;
		}

		public int compareTo(Object o) {
			return this.line - ((LineInfo) o).line;
		}

		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append('(').append(this.line).append(',').append(this.label).append(')');
			return String.valueOf(buffer);
		}
	}

	static class LocalLineNumberMarker {
		int lineNumber;
		int varIndex;
		Label label;
		
		public LocalLineNumberMarker(int line, int varIndex) {
			this.lineNumber = line;
			this.varIndex = varIndex;
		}
		public boolean equals(Object obj) {
			LocalLineNumberMarker marker = (LocalLineNumberMarker) obj;
			return this.lineNumber == marker.lineNumber && this.varIndex == marker.varIndex;
		}

		public int hashCode() {
			return this.varIndex;
		}
	}

	private List collector = null;
	private IApiComponent fComponent = null;
	String classname = null;

	/**
	 * Current type being visited.
	 */
	private IReferenceTypeDescriptor fType;

	/**
	 * Stack of members being visited. When a member is entered its
	 * element descriptor is pushed onto the stack. When a member
	 * is exited, the stack is popped.
	 */
	private Stack fMemberStack = new Stack();

	/**
	 * Stack of super types of types being visited. When a type is
	 * entered, its super type is pushed onto the stack. When a type
	 * is exited, the stack is popped.
	 */
	private Stack fSuperStack = new Stack();

	/**
	 * Whether to extract references to elements within the classfile
	 * being scanned.
	 */
	private boolean fIncludeLocalRefs = false;
	
	/**
	 * Bit mask of {@link ReferenceModifiers} to extract.
	 */
	private int fReferenceKinds = 0;

	/**
	 * Bit mask that determines if we need to visit members
	 */
	private static final int VISIT_MEMBERS_MASK = 
		ReferenceModifiers.MASK_REF_ALL ^
			(ReferenceModifiers.REF_EXTENDS | ReferenceModifiers.REF_IMPLEMENTS);
	
	private boolean fIsVisitMembers = false;
	
	/**
	 * Current field being visited, or <code>null</code> (when
	 * not within a field).
	 */
	private ClassFileSignatureVisitor signaturevisitor = new ClassFileSignatureVisitor();
	private static int TYPE = 0, FIELD = 1, METHOD = 2;

	/**
	 * Constructor
	 * @param component the component this scanned class file resides in
	 * while visiting the class
	 * @param collector the listing of references to annotate from this pass
	 * @param referenceKinds kinds of references to extract as defined by {@link ReferenceModifiers}
	 */
	public ClassFileVisitor(IApiComponent component, List collector, int referenceKinds) {
		super(new ClassNode());
		this.fComponent = component;
		this.collector = collector;
		fReferenceKinds = referenceKinds;
		fIsVisitMembers = (VISIT_MEMBERS_MASK & fReferenceKinds) > 0; 
	}

	/**
	 * Returns whether to consider a reference to the specified type.
	 * Configured by setting to include references within the same
	 * class file.
	 *
	 * @param owner
	 * @return true if considered, false otherwise
	 */
	protected boolean consider(String owner) {
		if (this.fIncludeLocalRefs) {
			return true;
		}
		return !(this.classname.equals(owner) || this.classname.startsWith(owner) || "<clinit>".equals(owner) || "this".equals(owner)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns whether the specified reference to the target element should be
	 * considered when extracting references. Configured by setting on whether
	 * to include references within the same class file.
	 *
	 * @param refKind kind of reference
	 * @param element element
	 * @return true if references should be considered
	 */
	protected boolean consider(int refKind, IMemberDescriptor element) {
		if (this.fIncludeLocalRefs) {
			return true;
		}
		if (element.getElementType() == IElementDescriptor.T_REFERENCE_TYPE) {
			if (((IReferenceTypeDescriptor)element).isAnonymous()) {
				// don't consider references to anonymous types
				return false;
			}
		} else {
			IReferenceTypeDescriptor enclosingType = element.getEnclosingType();
			if (enclosingType.isAnonymous()) {
				// don't consider references to elements in an anonymous type
				return false;
			}
		}
		if (refKind == ReferenceModifiers.REF_VIRTUALMETHOD || refKind == ReferenceModifiers.REF_OVERRIDE) {
			return true;
		}
		IElementDescriptor temp = element;
		while (temp.getElementType() != IElementDescriptor.T_PACKAGE) {
			if (this.fType.equals(temp)) {
				return true;
			}
			temp = temp.getParent();
		}
		IReferenceTypeDescriptor enclosing = this.fType.getEnclosingType();
		while (enclosing != null) {
			if (element.equals(enclosing)) {
				return false;
			}
			enclosing = enclosing.getEnclosingType();
		}
		return true;
	}

	/**
	 * Returns the full internal name (if available) from the given simple name.
	 * The returned name has been modified to be '.' separated
	 * @param name
	 * @return
	 */
	protected String processName(String name) {
		String newname = name;
		Type type = Type.getObjectType(name);
		if(type != null && type.getSort() == Type.OBJECT) {
			newname = type.getInternalName();
		}
		return newname.replaceAll("/", "."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Adds a reference to the given type from the current member. Discards
	 * the reference if the type corresponds to the class file being scanned
	 * or if the type is a primitive type.
	 *
	 * @param type referenced type
	 * @param linenumber line number where referenced
	 * @param kind kind of reference
	 * @return reference added, or <code>null</code> if none
	 */
	protected IReference addTypeReference(Type type, int kind) {
		Type rtype = this.resolveType(type.getDescriptor());
		if(rtype != null) {
			IReferenceTypeDescriptor target = Util.getType(rtype.getClassName());
			return this.addReference(target, kind);
		}
		return null;
	}

	/**
	 * Adds a reference to the given field from the current member. Discards
	 * the reference if the field is defined in the class file being scanned.
	 *
	 * @param declaringType type declaring the field being referenced
	 * @param name of the field being referenced
	 * @param linenumber line number where referenced
	 * @param kind kind of reference
	 * @return reference added, or <code>null</code> if none
	 */
	protected IReference addFieldReference(Type declaringType, String name, int kind) {
		Type rtype = this.resolveType(declaringType.getDescriptor());
		if(rtype != null) {
			IReferenceTypeDescriptor target = Util.getType(rtype.getClassName());
			return this.addReference(target.getField(name), kind);
		}
		return null;
	}

	/**
	 * Adds a reference to the given method from the current member. Discards
	 * the reference if the method is defined in the class file being scanned.
	 *
	 * @param declaringType type declaring the method (but could be a virtual lookup)
	 * @param name of the method being referenced
	 * @param signature signature of the method
	 * @param linenumber line number where referenced
	 * @param kind kind of reference
	 * @return reference added, or <code>null</code> if none
	 */
	protected IReference addMethodReference(Type declaringType, String name, String signature, int kind) {
		Type rtype = this.resolveType(declaringType.getDescriptor());
		if(rtype != null) {
			IReferenceTypeDescriptor target = Util.getType(rtype.getClassName());
			return this.addReference(target.getMethod(name, signature), kind);
		}
		return null;
	}

	/**
	 * Adds the given method declaration as a potential reference to an
	 * overridden method.
	 *
	 * @param method method declared
	 * @return reference added, or <code>null</code> if none
	 */
	protected IReference addMethodDeclaration(IMethodDescriptor method) {
		return this.addReference(method, ReferenceModifiers.REF_OVERRIDE);
	}

	/**
	 * Adds a reference to the given target member from the given line number
	 * in the class file being scanned. If the target member is contained
	 * in the class file being scanned it is discarded based on the
	 * setting to include local references.
	 *
	 * @param member the target member being referenced
	 * @param linenumber line number the reference was made from
	 * @param kind the kind of reference
	 * @param reference added, or <code>null</code> if none
	 */
	protected IReference addReference(IMemberDescriptor target, int kind) {
		if(this.consider(kind, target)) {
			Reference ref = new Reference(
				new Location(this.fComponent, this.getMember()),
				new Location(null, target),
				kind);
			this.collector.add(ref);
			return ref;
		}
		return null;
	}

	/**
	 * Processes the member signature from the specified type with the given signature and kind.
	 * A member can be either a type, method, field or local variable
	 *
	 * @param name the name of the member to process
	 * @param signature the signature of the member to process
	 * @param kind the kind
	 * @param type the type of member wanting to use the visitor
	 *
	 * @return the collection of references created for this signature
	 */
	protected List processSignature(String name, String signature, int kind, int type) {
		SignatureReader reader = new SignatureReader(signature);
		this.signaturevisitor.kind = kind;
		this.signaturevisitor.name = this.processName(name);
		this.signaturevisitor.signature = signature;
		this.signaturevisitor.originalkind = kind;
		this.signaturevisitor.argumentcount = 0;
		this.signaturevisitor.type = type;
		if(kind == ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL || kind == ReferenceModifiers.REF_PARAMETERIZED_METHODDECL) {
			reader.accept(this.signaturevisitor);
		} else {
			reader.acceptType(this.signaturevisitor);
		}
		List result = new ArrayList();
		result.addAll(this.signaturevisitor.references);
		this.collector.addAll(this.signaturevisitor.references);
		this.signaturevisitor.reset();
		return result;
	}

	/**
	 * Resolves the type from the string description. This method takes only type descriptions
	 * as a parameter, all else will throw an exception from the ASM framework
	 * If the description is an array, the underlying type of the array is returned.
	 * @param desc
	 * @return the {@link Type} of the description or <code>null</code>
	 */
	protected Type resolveType(String desc) {
		Type type = Type.getType(desc);
		if(type.getSort() == Type.OBJECT) {
			return type;
		}
		if(type.getSort() == Type.ARRAY) {
			type = type.getElementType();
			if(type.getSort() == Type.OBJECT) {
				return type;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classname = this.processName(name);
		this.fType = Util.getType(this.classname, access);
		if(DEBUG) {
			System.out.println("Starting visit of type: ["+this.fType.getQualifiedName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.enterMember(this.fType);
		//if there is a signature we get more information from it, so we don't need to do both
		if(signature != null) {
			this.processSignature(name, signature, ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, TYPE);
		}
		else {
			if((access & Opcodes.ACC_INTERFACE) != 0) {
				//the type is an interface and we need to treat the interfaces set as extends, not implements
				Type supertype = null;
				for(int i = 0; i < interfaces.length; i++) {
					supertype = Type.getObjectType(interfaces[i]);
					IReference typeReference = this.addTypeReference(supertype, ReferenceModifiers.REF_EXTENDS);
					if (typeReference != null) {
						this.fSuperStack.add(typeReference.getReferencedLocation().getType());
					}
				}
			}
			else {
				Type supertype = null;
				if(superName != null) {
					supertype = Type.getObjectType(superName);
					IReference typeReference = this.addTypeReference(supertype, ReferenceModifiers.REF_EXTENDS);
					if (typeReference != null) {
						this.fSuperStack.add(typeReference.getReferencedLocation().getType());
					}
				}
				for(int i = 0; i < interfaces.length; i++) {
					supertype = Type.getObjectType(interfaces[i]);
					this.addTypeReference(supertype, ReferenceModifiers.REF_IMPLEMENTS);
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitEnd()
	 */
	public void visitEnd() {
		this.exitMember();
		if (!this.fSuperStack.isEmpty()) {
			IReferenceTypeDescriptor type = (IReferenceTypeDescriptor) this.fSuperStack.pop();
			if(DEBUG) {
				System.out.println("ending visit of type: ["+type.getQualifiedName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (fIsVisitMembers) {
			IReferenceTypeDescriptor owner = (IReferenceTypeDescriptor) this.getMember();
			IFieldDescriptor field = owner.getField(name, access);
			this.enterMember(field);
			if((access & Opcodes.ACC_SYNTHETIC) == 0) {
				if(signature != null) {
					this.processSignature(name, signature, ReferenceModifiers.REF_PARAMETERIZED_FIELDDECL, FIELD);
				} else {
					this.addTypeReference(Type.getType(desc), ReferenceModifiers.REF_FIELDDECL);
				}
			}
			this.exitMember();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (fIsVisitMembers) {
			IMemberDescriptor member = this.getMember();
			IReferenceTypeDescriptor owner = null;
			if (member instanceof IReferenceTypeDescriptor) {
				owner = (IReferenceTypeDescriptor) member;
			} else {
				owner = member.getEnclosingType();
			}
			IMethodDescriptor method = owner.getMethod(name, desc, access);
			this.enterMember(method);
			// record potential method override reference
			if ((access & (Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC)) > 0) {
				if (!this.fSuperStack.isEmpty()) {
					IReferenceTypeDescriptor superType = (IReferenceTypeDescriptor) this.fSuperStack.peek();
					this.addMethodDeclaration(superType.getMethod(method.getName(), method.getSignature(), method.getModifiers()));
				}
			}
			if((access & Opcodes.ACC_SYNTHETIC) == 0 && !"<clinit>".equals(name)) { //$NON-NLS-1$
				int argumentcount = 0;
				if(signature != null) {
					this.processSignature(name, signature, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, METHOD);
					argumentcount = this.signaturevisitor.argumentcount;
				}
				else {
					Type[] arguments = Type.getArgumentTypes(desc);
					for(int i = 0; i < arguments.length; i++) {
						Type type = arguments[i];
						this.addTypeReference(type, ReferenceModifiers.REF_PARAMETER);
						argumentcount += type.getSize();
					}
					this.addTypeReference(Type.getReturnType(desc), ReferenceModifiers.REF_RETURNTYPE);
					if(exceptions != null) {
						for(int i = 0; i < exceptions.length; i++) {
							this.addTypeReference(Type.getObjectType(exceptions[i]),ReferenceModifiers.REF_THROWS);
						}
					}
				}
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				if(mv != null && ((access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0)) {
					return new ClassFileMethodVisitor(mv, name, argumentcount);
				}
			}
		}
		return null;
	}

	/**
	 * Called when a member is entered. Pushes the member onto the member
	 * stack.
	 *
	 * @param member current member
	 */
	protected void enterMember(IMemberDescriptor member) {
		this.fMemberStack.push(member);
	}

	/**
	 * Called when a member is exited. Pops the top member off the stack.
	 */
	protected void exitMember() {
		this.fMemberStack.pop();
	}

	/**
	 * Returns the element descriptor for the current member being
	 * visited.
	 *
	 * @return current member
	 */
	protected IMemberDescriptor getMember() {
		return (IMemberDescriptor) this.fMemberStack.peek();
	}
}
