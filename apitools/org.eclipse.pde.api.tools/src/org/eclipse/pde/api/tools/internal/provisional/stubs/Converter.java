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
package org.eclipse.pde.api.tools.internal.provisional.stubs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * Utility to convert 'normal' class files into 'stubs', removing the body declarations
 * but (optionally) keeping references
 * 
 * @since 1.0.0
 */
public class Converter {
	static final String STUB_MARKER_NAME = "StubMarker"; //$NON-NLS-1$
	
	/**
	 * Class adapter
	 */
	static class StubClassAdapter extends ClassAdapter {
		int flags;
		String name;

		/**
		 * Constructor
		 * @param visitor
		 * @param flags
		 */
		public StubClassAdapter(ClassVisitor visitor, int flags) {
			super(visitor);
			this.flags = flags;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
		 */
		public void visit(int version,
				int access,
				String className,
				String signature,
				String superName,
				String[] interfaces) {
			this.name = className;
			cv.visit(version, access, className, signature, superName, interfaces);
			cv.visitAttribute(new StubMarkerAttribute());
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
		 */
		public FieldVisitor visitField(int access, String fieldName, String desc, String signature, Object value) {
			if ((this.flags & MODIFIER_PRIVATE) == 0 && (access & Opcodes.ACC_PRIVATE) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PUBLIC) == 0 && (access & Opcodes.ACC_PUBLIC) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PROTECTED) == 0 && (access & Opcodes.ACC_PROTECTED) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PACKAGE) == 0 && access == 0) {
				return null;
			}
			if ((this.flags & MODIFIER_SYNTHETIC) == 0 && (access & Opcodes.ACC_SYNTHETIC) != 0) {
				return null;
			}
			return super.visitField(access, fieldName, desc, signature, value);
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
		 */
		public MethodVisitor visitMethod(int access,
				String methodName,
				String desc,
				String signature,
				String[] exceptions) {
			boolean reportRefs = (this.flags & REPORT_REFS) != 0;
			int accessFlags;
			if ((access & Opcodes.ACC_ABSTRACT) == 0) {
				// this is not an abstract method
				accessFlags = reportRefs ? access : access | Opcodes.ACC_NATIVE;
			} else {
				accessFlags = access;
			}
			if (!reportRefs && "<clinit>".equals(methodName)) { //$NON-NLS-1$
				return null;
			}
			if ((this.flags & MODIFIER_PRIVATE) == 0 && (access & Opcodes.ACC_PRIVATE) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PUBLIC) == 0 && (access & Opcodes.ACC_PUBLIC) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PROTECTED) == 0 && (access & Opcodes.ACC_PROTECTED) != 0) {
				return null;
			}
			if ((this.flags & MODIFIER_PACKAGE) == 0 && access == 0) {
				return null;
			}
			if ((this.flags & MODIFIER_SYNTHETIC) == 0 && (access & Opcodes.ACC_SYNTHETIC) != 0) {
				return null;
			}
			
			if (reportRefs) {
				return new ReferencesStubMethodAdapter(cv, accessFlags, methodName, desc, signature, exceptions);
		    }
			
	    	MethodVisitor visitor = super.visitMethod(accessFlags, methodName, desc, signature, exceptions);
			if (visitor != null) {
				visitor.visitEnd(); // for safety
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.ClassAdapter#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
		 */
		public void visitInnerClass(String innerClassName, String outerName, String innerName, int access) {
			if (((this.flags & REPORT_REFS) != 0) || (outerName != null && innerName != null)) {
				super.visitInnerClass(innerClassName, outerName, innerName, access);
			} else if (this.name.equals(innerClassName) && (outerName == null || innerName == null)) {
				// local class
				this.flags |= IGNORE_CLASS_FILE;
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.ClassAdapter#visitAttribute(org.objectweb.asm.Attribute)
		 */
		public void visitAttribute(Attribute attr) {
			if ((this.flags & VERBOSE_MODE) != 0) {
				System.out.println(attr.type);
			}
			if ("Synthetic".equals(attr.type)) { //$NON-NLS-1$
				this.flags |= IGNORE_CLASS_FILE;
			}
			if (STUB_MARKER_NAME.equals(attr.type)) {
				this.flags |= IS_STUB_CLASS_FILE;
			}
			super.visitAttribute(attr);
		}

		/**
		 * @return if this class file should be ignored or not
		 */
		public boolean shouldIgnore() {
			return (this.flags & IGNORE_CLASS_FILE) != 0;
		}
		
		/**
		 * @return if this class file is a stub or not
		 */
		public boolean isStub() {
			return (this.flags & IS_STUB_CLASS_FILE) != 0;
		}
	}

	/**
	 * Method adapter for creating stub methods
	 */
	static class ReferencesStubMethodAdapter extends MethodNode {
		private final ClassVisitor cv;
		String stringLiteral;
		int line;
		Label label;
		int lastLine = -1;
		boolean hasRefs; 

		/**
		 * Constructor
		 * @param exceptions 
		 * @param signature 
		 * @param desc 
		 * @param methodName 
		 * @param accessFlags 
		 * @param cv 
		 * @param visitor
		 */
		public ReferencesStubMethodAdapter(ClassVisitor cv, int accessFlags, String methodName, String desc, String signature, String[] exceptions) {
			super(accessFlags, methodName, desc, signature, exceptions);
			this.cv = cv;
			this.hasRefs = false;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitIincInsn(int, int)
		 */
		public void visitIincInsn(int var, int increment) {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitMultiANewArrayInsn(String, int)
		 */
		public void visitMultiANewArrayInsn(String desc, int dims) {
			this.insertLineEntry();
			super.visitMultiANewArrayInsn(desc, dims);
			this.hasRefs = true;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitInsn(int)
		 */
		public void visitInsn(int opcode) {
			// nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, String, String, String)
		 */
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			this.insertLineEntry();
			super.visitFieldInsn(opcode, owner, name, desc);
			this.hasRefs = true;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitTypeInsn(int, String)
		 */
		public void visitTypeInsn(int opcode, String desc) {
			this.insertLineEntry();
			super.visitTypeInsn(opcode, desc);
			this.hasRefs = true;
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitIntInsn(int, int)
		 */
		public void visitIntInsn(int opcode, int operand) {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitJumpInsn(int, Label)
		 */
		public void visitJumpInsn(int opcode, Label label) {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLdcInsn(java.lang.Object)
		 */
		public void visitLdcInsn(Object cst) {
			if (cst instanceof Type) {
				this.insertLineEntry();
				super.visitLdcInsn(cst);
				this.hasRefs = true;
			} else if (cst instanceof String) {
				stringLiteral = (String) cst;
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitLookupSwitchInsn(org.objectweb.asm.Label, int[], org.objectweb.asm.Label[])
		 */
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitTableSwitchInsn(int, int, org.objectweb.asm.Label, org.objectweb.asm.Label[])
		 */
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitVarInsn(int, int)
		 */
		public void visitVarInsn(int opcode, int var) {
			switch(opcode) {
				case Opcodes.ASTORE :
					this.insertLineEntry();
					super.visitVarInsn(opcode, var);
					this.hasRefs = true;
			}
		}

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitFrame(int, int, Object[], int, Object[])
		 */
		public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
			// Nothing to do
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

		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			switch(opcode){
				case Opcodes.INVOKESTATIC: {
					// check for reference to a class literal
					if (name.equals("forName")) { //$NON-NLS-1$
						if (processName(owner).equals("java.lang.Class")) { //$NON-NLS-1$
							if (stringLiteral != null) {
								super.visitLdcInsn(this.stringLiteral);
							}
						}
					}
					break;
				}
			}
			stringLiteral = null;
			this.insertLineEntry();
			super.visitMethodInsn(opcode, owner, name, desc);
			this.hasRefs = true;
		}

		private void insertLineEntry() {
			if (this.lastLine != this.line && this.label != null) {
				super.visitLineNumber(this.line, this.label);
				this.lastLine = this.line;
			}
		}

		public void visitLineNumber(int line, Label start) {
			this.line = line;
			this.label = start;
		}
		
		public void visitEnd() {
			if (hasRefs) {
				MethodVisitor mv = cv.visitMethod(access, name, desc, signature, (String[]) exceptions.toArray(new String[exceptions.size()]));
				if (mv != null) {
					accept(mv);
				}
			} else {
				MethodVisitor mv = cv.visitMethod(access | Opcodes.ACC_NATIVE, name, desc, signature, (String[]) exceptions.toArray(new String[exceptions.size()]));
				if (mv != null) {
					mv.visitEnd();
				}
			}
		}
	}

	/**
	 * class to represent that the class file it is placed in is a stub
	 */
	static class StubMarkerAttribute extends Attribute {
		/**
		 * Constructor
		 */
		public StubMarkerAttribute() {
			super(STUB_MARKER_NAME);
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.Attribute#isUnknown()
		 */
		public boolean isUnknown() {
			return false;
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.Attribute#read(org.objectweb.asm.ClassReader, int, int, char[], int, org.objectweb.asm.Label[])
		 */
		protected Attribute read(ClassReader cr, int off, int len, char[] buf, int codeOff, Label[] labels) {
			return new StubMarkerAttribute();
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.Attribute#write(org.objectweb.asm.ClassWriter, byte[], int, int, int)
		 */
		protected ByteVector write(ClassWriter cw, byte[] code, int len, int maxStack, int maxLocals) {
			return new ByteVector();
		}
	}

	static class LineNumberMergerClassAdapter extends ClassAdapter {

		public LineNumberMergerClassAdapter(ClassVisitor visitor) {
			super(visitor);
		}
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor visitMethod = super.visitMethod(access, name, desc, signature, exceptions);
			return new LineNumberMergerMethodAdapter(visitMethod);
		}
	}

	static class LineNumberMergerMethodAdapter extends MethodAdapter {
		int lastOffset = -1;
		public LineNumberMergerMethodAdapter(MethodVisitor visitor) {
			super(visitor);
		}
		public void visitLineNumber(int line, Label start) {
			int offset = start.getOffset();
			if (offset != lastOffset) { 
				mv.visitLineNumber(line, start);
				lastOffset = offset;
			}
		}
	}

	static final int DEFAULT = 0;

	static final int ABORT = 0x01;

	static final int PROCESS_CLASS_FILES = 0x02;

	static final int PROCESS_ARCHIVES = 0x04;

	public static final int REPORT_REFS = 0x08;

	static final int PROCESS_SUB = 0x10;

	static final int PROCESS_ALL = 0x20;

	static final int COMPRESS_RESULTING_ARCHIVES = 0x80;

	static final int IGNORE_CLASS_FILE = 0x100;

	public static final int MODIFIER_PRIVATE = 0x200;

	public static final int MODIFIER_PUBLIC = 0x400;

	public static final int MODIFIER_PROTECTED = 0x800;

	public static final int MODIFIER_PACKAGE = 0x1000;

	public static final int MODIFIER_SYNTHETIC = 0x2000;

	public static final int MODIFIER_MASK =
		  MODIFIER_PRIVATE
		| MODIFIER_PUBLIC
		| MODIFIER_PROTECTED
		| MODIFIER_PACKAGE
		| MODIFIER_SYNTHETIC;

	static final int VERBOSE_MODE = 0x4000;

	static final int RESOURCES_FILES = 0x8000;

	static final int IS_STUB_CLASS_FILE = 0x10000;

	static final CRC32 CRC32 = new CRC32();

	/**
	 * Main method used to run from the command line
	 * @param args
	 */
	public static void main(String[] args) {
		Converter converter = new Converter();
		converter.configure(args);
		if (converter.isVerbose()) {
			long time = System.currentTimeMillis();
			converter.process();
			System.out.println("" + (System.currentTimeMillis() - time) + "ms spent"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			converter.process();
		}
	}

	/**
	 * Creates a stub class file from the specified 'normal' class file or <code>null</code>
	 * if the class file being converted is an anonymous or a local inner type.
	 * 
	 * @param classFile the class file to convert to a 'stub'
	 * @return a byte array representing the stub class file
	 * @throws CoreException
	 * @throws IOException
	 */
	public static byte[] createStub(IClassFile classFile) throws CoreException, IOException {
		return createStub(classFile, MODIFIER_MASK);
	}

	/**
	 * Creates a stub class file from the specified 'normal' class file, considering the or'd
	 * set of options. If the class file being converted is an anonymous or a local inner type, <code>null</code>
	 * is returned.
	 * 
	 * @param classFile to class file to convert to a 'stub'
	 * @param options the or'd set of options to use during the conversion
	 * @return a byte array representing the stub class file
	 * @throws CoreException
	 * @throws IOException
	 */
	public static byte[] createStub(IClassFile classFile, int options) throws CoreException, IOException {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new ByteArrayInputStream(classFile.getContents()));
			return process(inputStream, options);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch(IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
	}

	/**
	 * @return if the converter is in verbose mode
	 */
	private boolean isVerbose() {
		return (this.bits & VERBOSE_MODE) != 0;
	}

	/**
	 * Writes the given entry information into the output stream
	 * @param outputStream the output stream to write out to
	 * @param entryName
	 * @param bytes
	 * @param crc32
	 * @param bits
	 * @throws IOException
	 */
	private static void writeZipFileEntry(ZipOutputStream outputStream,
			String entryName,
			byte[] bytes,
			CRC32 crc32,
			int bits) throws IOException {
		crc32.reset();
		int byteArraySize = bytes.length;
		crc32.update(bytes, 0, byteArraySize);
		ZipEntry entry = new ZipEntry(entryName);
		entry.setMethod((bits & COMPRESS_RESULTING_ARCHIVES) != 0 ? ZipEntry.DEFLATED : ZipEntry.STORED);
		entry.setSize(byteArraySize);
		entry.setCrc(crc32.getValue());
		outputStream.putNextEntry(entry);
		outputStream.write(bytes, 0, byteArraySize);
		outputStream.closeEntry();
	}

	int bits;

	private String input;

	private String output;

	private void configure(String[] args) {
		// command line processing
		/*
		 * Recognized options:
		 * -verbose, -v: verbose mode
		 * -s : process all subdirectories
		 * -output: where to put the converted files
		 * -input: If a directory, -s is considered
		 * -all : process .class and archive files
		 * -refs : preserve references
		 * -classfiles : process only .class files
		 * -archives : process only archive files (.jar/.zip)
		 * -compress: compress resulting archives (default no)
		 * -keep:private,protected,public,package,synthetic,all,none (default all)
		 * -skipresourcefiles (default: off)
		 */
		this.bits |= (MODIFIER_MASK | RESOURCES_FILES);
		final int DEFAULT_MODE = 0;
		final int INPUT_MODE = 1;
		final int OUTPUT_MODE = 2;
		int mode = DEFAULT_MODE;
		for (int i = 0, max = args.length; i < max; i++) {
			String currentArg = args[i];
			switch (mode) {
				case DEFAULT_MODE:
					if ("-input".equals(currentArg)) { //$NON-NLS-1$
						mode = INPUT_MODE;
						continue;
					}
					if ("-output".equals(currentArg)) { //$NON-NLS-1$
						mode = OUTPUT_MODE;
						continue;
					}
					if ("-s".equals(currentArg)) { //$NON-NLS-1$
						this.bits |= PROCESS_SUB;
						continue;
					}
					if ("-all".equals(currentArg)) { //$NON-NLS-1$
						if ((this.bits & (PROCESS_ARCHIVES | PROCESS_CLASS_FILES | PROCESS_ALL)) != 0) {
							// invalid entry: -all cannot be used with -archives
							// or -classfiles
							this.bits |= ABORT;
							return;
						}
						this.bits |= PROCESS_ALL;
						continue;
					}
					if ("-archives".equals(currentArg)) { //$NON-NLS-1$
						if ((this.bits & (PROCESS_ARCHIVES | PROCESS_CLASS_FILES | PROCESS_ALL)) != 0) {
							// invalid entry: -all cannot be used with -archives
							// or -classfiles
							this.bits |= ABORT;
							return;
						}
						this.bits |= PROCESS_ARCHIVES;
						continue;
					}
					if ("-classfiles".equals(currentArg)) { //$NON-NLS-1$
						if ((this.bits & (PROCESS_ARCHIVES | PROCESS_CLASS_FILES | PROCESS_ALL)) != 0) {
							// invalid entry: -all cannot be used with -archives
							// or -classfiles
							this.bits |= ABORT;
							return;
						}
						this.bits |= PROCESS_CLASS_FILES;
						continue;
					}
					if ("-refs".equals(currentArg)) { //$NON-NLS-1$
						this.bits |= REPORT_REFS;
						continue;
					}
					if ("-compress".equals(currentArg)) { //$NON-NLS-1$
						this.bits |= COMPRESS_RESULTING_ARCHIVES;
						continue;
					}
					if (currentArg.startsWith("-keep:")) { //$NON-NLS-1$
						// clear all modifiers bits
						this.bits &= ~MODIFIER_MASK;
						String keepOption = currentArg;
						int length = keepOption.length();
						if (length <= 6) {
							System.err.println("Unknown option : " + currentArg); //$NON-NLS-1$
							continue;
						}
						StringTokenizer tokenizer = new StringTokenizer(keepOption.substring(6, length), ","); //$NON-NLS-1$
						int tokenCounter = 0;
						while (tokenizer.hasMoreTokens()) {
							String token = tokenizer.nextToken();
							tokenCounter++;
							if ("private".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_PRIVATE;
							} else if ("public".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_PUBLIC;
							} else if ("protected".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_PROTECTED;
							} else if ("synthetic".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_SYNTHETIC;
							} else if ("package".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_PACKAGE;
							} else if ("all".equals(token)) { //$NON-NLS-1$
								this.bits |= MODIFIER_MASK;
							} else if ("none".equals(token)) { //$NON-NLS-1$
								// no bits to set
								if ((this.bits & MODIFIER_MASK) != 0) {
									System.err.println("none option cannot be used with other visibility check : " //$NON-NLS-1$
											+ currentArg);
								}
							}
						}
						if (tokenCounter == 0) {
							System.err.println("Unknown option : " + currentArg); //$NON-NLS-1$
						}
						continue;
					}
					if ("-verbose".equals(currentArg) || "-v".equals(currentArg)) { //$NON-NLS-1$ //$NON-NLS-2$
						this.bits |= VERBOSE_MODE;
						continue;
					}
					if ("-skipresourcefiles".equals(currentArg)) { //$NON-NLS-1$
						this.bits &= ~RESOURCES_FILES;
						continue;
					}
					System.err.println("Unknown option : " + currentArg); //$NON-NLS-1$
					break;
				case INPUT_MODE:
					this.input = currentArg;
					mode = DEFAULT_MODE;
					break;
				case OUTPUT_MODE:
					this.output = currentArg;
					mode = DEFAULT_MODE;
			}
		}
		if (this.input == null || this.output == null) {
			this.bits |= ABORT;
			return;
		}
		File outputFile = new File(this.output);
		if (!outputFile.exists()) {
			// create the dir
			if (!outputFile.mkdirs()) {
				// could not create the output dir
				this.bits |= ABORT;
			}
		} else if (!outputFile.isDirectory()) {
			// output file must be a directory
			this.bits |= ABORT;
		}
		if ((this.bits & (PROCESS_ARCHIVES | PROCESS_CLASS_FILES | PROCESS_ALL)) != 0) {
			// add all by default
			this.bits |= PROCESS_ALL;
		}
	}

	/**
	 * Performs the main processing of the converter
	 */
	private void process() {
		if (shouldAbort()) {
			printUsage();
			return;
		}
		File root = new File(this.input);
		if (root.isDirectory()) {
			// process all entries
			final boolean processSubDirectories = (this.bits & PROCESS_SUB) != 0;
			File[] allFiles = Util.getAllFiles(root, new FileFilter() {
				public boolean accept(File path) {
					final String pathName = path.getAbsolutePath();
					if ((Converter.this.bits & PROCESS_ALL) != 0) {
						if (Util.isArchive(pathName) || Util.isClassFile(pathName)
								|| (processSubDirectories && path.isDirectory())) {
							return true;
						}
						if (path.isFile() && ((Converter.this.bits & RESOURCES_FILES) != 0)) {
							return true;
						}
						if ((Converter.this.bits & VERBOSE_MODE) != 0) {
							System.out.println("Skip resource file : " + pathName); //$NON-NLS-1$
						}
						return false;
					}
					if ((Converter.this.bits & PROCESS_CLASS_FILES) != 0) {
						return Util.isClassFile(pathName) || (processSubDirectories && path.isDirectory());
					}
					if ((Converter.this.bits & PROCESS_ARCHIVES) != 0) {
						return Util.isArchive(pathName) || (processSubDirectories && path.isDirectory());
					}
					return false;
				}
			});
			if (allFiles == null)
				return;
			String rootAbsolutePath = root.getAbsolutePath();
			for (int i = 0, max = allFiles.length; i < max; i++) {
				File inputFile = allFiles[i];
				File outputFile = new File(this.output, inputFile.getAbsolutePath()
						.substring(rootAbsolutePath.length()));
				processSingleEntry(inputFile.getAbsolutePath(), outputFile, this.bits);
			}
		} else {
			File inputFile = new File(this.input);
			File outputFile = new File(this.output, inputFile.getName());
			processSingleEntry(this.input, outputFile, this.bits);
		}
	}

	/**
	 * Prints out the usage for the converter tool
	 */
	private void printUsage() {
		String usage =
			"-input [file or folder name] -output [folder name] [options]\n" + //$NON-NLS-1$
			"\n" + //$NON-NLS-1$
			"Required arguments:\n" + //$NON-NLS-1$
			"-input: If a directory, -s, -classfiles and -archives are considered\n" +  //$NON-NLS-1$
			"-output: directory where to put the converted files\n" +  //$NON-NLS-1$
			"\n" + //$NON-NLS-1$
			"Recognized options:\n" +  //$NON-NLS-1$
			"-verbose, -v: verbose mode (default: off)\n" +  //$NON-NLS-1$
			"-s : process all subdirectories (default: off)\n" +  //$NON-NLS-1$
			"-all : process .class and archive files (default)\n" +  //$NON-NLS-1$
			"-refs : preserve references (default: off)\n" +  //$NON-NLS-1$
			"-classfiles : process only .class files\n" +  //$NON-NLS-1$
			"-archives : process only archive files (.jar/.zip)\n" +  //$NON-NLS-1$
			"-compress: compress resulting archives (default no)\n" +  //$NON-NLS-1$
			"-keep:private,protected,public,package,synthetic,all,none (default all)\n" + //$NON-NLS-1$
			"-skipresourcefiles (default: off)"; //$NON-NLS-1$
		System.err.println(usage);
	}

	/**
	 * Processes a single class file given its file name, the place to output the stub to and the 
	 * flags
	 * @param inputFileName the class file to convert
	 * @param outputFile the output location for the new stub
	 * @param flags the flags passed in when running the converter
	 */
	private static void processSingleEntry(String inputFileName, File outputFile, int flags) {
		File parentFile = outputFile.getParentFile();
		if (!parentFile.exists()) {
			if (!parentFile.mkdirs()) {
				System.err.println("Could not create " + outputFile); //$NON-NLS-1$
				return;
			}
		}
		if (Util.isArchive(inputFileName)) {
			if ((flags & VERBOSE_MODE) != 0) {
				System.out.println("Process " + inputFileName); //$NON-NLS-1$
			}
			ZipInputStream inputStream = null;
			ZipOutputStream zipOutputStream = null;
			try {
				zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
			} catch (FileNotFoundException e) {
				ApiPlugin.log(e);
			}
			if (zipOutputStream == null) {
				System.err.println("Could not create the output file : " + outputFile); //$NON-NLS-1$
				return;
			}
			try {
				inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
				processArchiveEntry(inputStream, zipOutputStream, flags);
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						// ignore
					}
				}
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Dumping " + outputFile); //$NON-NLS-1$
				}
				try {
					zipOutputStream.flush();
					zipOutputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		} else {
			// input is a class file
			InputStream inputStream = null;
			byte[] bytes = null;
			try {
				if (Util.isClassFile(inputFileName)) {
					if ((flags & VERBOSE_MODE) != 0) {
						System.out.println("Process " + inputFileName); //$NON-NLS-1$
					}
					inputStream = new BufferedInputStream(new FileInputStream(inputFileName));
					bytes = process(inputStream, flags);
				} else if ((flags & RESOURCES_FILES) != 0) {
					if ((flags & VERBOSE_MODE) != 0) {
						System.out.println("Process resource file : " + inputFileName); //$NON-NLS-1$
					}
					inputStream = new BufferedInputStream(new FileInputStream(inputFileName));
					bytes = Util.getInputStreamAsByteArray(inputStream, -1);
				} else if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Skip resource file : " + inputFileName); //$NON-NLS-1$
				}

			} catch (FileNotFoundException e) {
				ApiPlugin.log(e);
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						ApiPlugin.log(e);
					}
				}
			}
			if (bytes != null) {
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Dumping " + outputFile); //$NON-NLS-1$
				}
				OutputStream outputStream = null;
				try {
					outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
					outputStream.write(bytes);
					outputStream.flush();
					outputStream.close();
				} catch (FileNotFoundException e) {
					ApiPlugin.log(e);
				} catch (IOException e) {
					ApiPlugin.log(e);
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
		}
	}

	/**
	 * Processes the given archive entry and places the stubs (in a new archive entry) to the specified
	 * archive output stream using the given flags
	 * @param inputStream
	 * @param zipOutputStream
	 * @param flags
	 * @throws IOException
	 */
	private static void processArchiveEntry(ZipInputStream inputStream, ZipOutputStream zipOutputStream, int flags)
			throws IOException {
		byte[] bytes = null;
		ZipEntry zipEntry = inputStream.getNextEntry();
		while (zipEntry != null) {
			String name = zipEntry.getName();
			if (Util.isClassFile(name)) {
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Process entry : " + name); //$NON-NLS-1$
				}
				bytes = process(inputStream, flags);
			} else if (Util.isArchive(name)) {
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Process entry : " + name); //$NON-NLS-1$
				}
				ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ZipOutputStream zipOutputStream2 = new ZipOutputStream(new BufferedOutputStream(byteArrayOutputStream));
				processArchiveEntry(zipInputStream, zipOutputStream2, flags);
				zipOutputStream2.flush();
				zipOutputStream2.close();
				bytes = byteArrayOutputStream.toByteArray();
			} else if ((flags & RESOURCES_FILES) != 0) {
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Process entry : " + name); //$NON-NLS-1$
				}
				bytes = Util.getInputStreamAsByteArray(inputStream, (int) zipEntry.getSize());
			} else if ((flags & VERBOSE_MODE) != 0) {
				System.out.println("Skip entry : " + name); //$NON-NLS-1$
			}
			if (bytes != null) {
				if ((flags & VERBOSE_MODE) != 0) {
					System.out.println("Dump entry : " + name); //$NON-NLS-1$
				}
				writeZipFileEntry(zipOutputStream, name, bytes, CRC32, flags);
			}
			inputStream.closeEntry();
			zipEntry = inputStream.getNextEntry();
		}
	}

	/**
	 * Processes the given input stream with the given flags
	 * @param inputStream the class file as an input stream
	 * @param flags the flags 
	 * @return a byte array representing the new stub
	 * @throws IOException
	 */
	private static byte[] process(InputStream inputStream, int flags) throws IOException {
		byte[] contents = Util.getInputStreamAsByteArray(inputStream, -1);
		ClassReader classReader = new ClassReader(contents);
		ClassWriter classWriter = new ClassWriter(0);
		ClassAdapter classWriter2 =  new LineNumberMergerClassAdapter(classWriter);
		StubClassAdapter visitor = new StubClassAdapter(classWriter2, flags);
		classReader.accept(visitor, ClassReader.SKIP_FRAMES);
		if (visitor.isStub()) {
			// return original bytes unmodified
			return contents;
		}
		if (visitor.shouldIgnore()) {
			return null;
		}
		return classWriter.toByteArray();
	}

	private boolean shouldAbort() {
		return (this.bits & ABORT) != 0;
	}
}
