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
package org.eclipse.pde.api.tools.internal;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.stubs.Converter;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common implementation of an API component as a composite class file container.
 * 
 * @since 1.0.0
 */
public abstract class AbstractApiComponent extends AbstractClassFileContainer implements IApiComponent {
	
	/**
	 * API description
	 */
	private IApiDescription fApiDescription = null;
		
	/**
	 * Api Filter store
	 */
	private IApiFilterStore fFilterStore = null;
	
	/**
	 * Owning profile
	 */
	private IApiProfile fProfile = null;
	
	/**
	 * Used to compute checksums
	 */
	private static CRC32 fgCRC32 = new CRC32(); 
	
	/**
	 * Exports class files to an archive.
	 * 
	 * @since 1.0.0
	 */
	class Exporter extends ClassFileContainerVisitor {
		
		/**
		 * Stream to write to
		 */
		private ZipOutputStream fZip;
		
		/**
		 * Whether to compress
		 */
		private boolean fCompress;
		
		/**
		 * Whether to generate stub
		 */
		private boolean fStub;
		
		/**
		 * Number of entries written
		 */
		private int fEntriesWritten = 0;
		
		/**
		 * Constructs a new exporter to write class files to the archive stream.
		 * 
		 * @param zip stream to write to
		 * @param compress whether to compress
		 * @param stub TODO
		 */
		public Exporter(ZipOutputStream zip, boolean compress, boolean stub) {
			fZip = zip;
			fCompress = compress;
			fStub = stub;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.model.component.IClassFile)
		 */
		public void visit(String packageName, IClassFile classFile) {
			try {
				byte[] contents = null;
				if (fStub) {
					contents = Converter.createStub(classFile, Converter.MODIFIER_MASK | Converter.REPORT_REFS);
				} else {
					contents = classFile.getContents();
				}
				// contents can be null when creating stubs - as anonymous inner classes are ignored
				if (contents != null) {
					String entryName = classFile.getTypeName().replace('.', '/') + Util.DOT_CLASS_SUFFIX;
					writeZipFileEntry(fZip, entryName, contents, fCompress);
					fEntriesWritten++;
				}
			} catch (CoreException e) {
				// TODO: abort
			} catch (IOException e) {
				// TODO: abort
			}
		}
		
		/**
		 * Returns the number of entries written to the archive.
		 * 
		 * @return the number of entries written to the archive
		 */
		public int getEntriesWritten() {
			return fEntriesWritten;
		}
	};
	
	/**
	 * Constructs an API component in the given profile.
	 * 
	 * @param profile API profile
	 */
	public AbstractApiComponent(IApiProfile profile) {
		fProfile = profile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			super.accept(visitor);
		}
		visitor.end(this);
	}	
		
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getProfile()
	 */
	public IApiProfile getProfile() {
		return fProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#dispose()
	 */
	public void dispose() {
		try {
			close();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		fApiDescription = null;
		fProfile = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getApiDescription()
	 */
	public synchronized IApiDescription getApiDescription() throws CoreException {
		if (fApiDescription == null) {
			fApiDescription = createApiDescription();
		}
		return fApiDescription;
	}
	
	/**
	 * Returns whether this component has created an API description.
	 * 
	 * @return whether this component has created an API description
	 */
	protected synchronized boolean isApiDescriptionInitialized() {
		return fApiDescription != null;
	}

	/**
	 * Returns if this component has created an API filter store
	 * 
	 * @return true if a store has been created, false other wise
	 */
	protected synchronized boolean hasApiFilterStore() {
		return fFilterStore != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public IClassFileContainer[] getClassFileContainers() {
		return super.getClassFileContainers();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public IClassFileContainer[] getClassFileContainers(String id) {
		if (this.hasFragments()) {
			return super.getClassFileContainers(id);
		} else {
			return super.getClassFileContainers();
		}
	}
	
	/**
	 * Creates and returns the API description for this component.
	 * 
	 * @return newly created API description for this component
	 */
	protected abstract IApiDescription createApiDescription() throws CoreException;
	
	/**
	 * Writes an entry into a zip file. Used when exporting API component.
	 * 
	 * @param outputStream zip file to write to
	 * @param entryName path of the entry to write
	 * @param bytes bytes to write
	 * @param boolean compress whether to compress the entry
	 * @throws IOException
	 */
	protected void writeZipFileEntry(ZipOutputStream outputStream, String entryName, byte[] bytes, boolean compress) throws IOException {
		fgCRC32.reset();
		int byteArraySize = bytes.length;
		fgCRC32.update(bytes, 0, byteArraySize);
		ZipEntry entry = new ZipEntry(entryName);
		entry.setMethod(compress ? ZipEntry.DEFLATED : ZipEntry.STORED);
		entry.setSize(byteArraySize);
		entry.setCrc(fgCRC32.getValue());
		outputStream.putNextEntry(entry);
		outputStream.write(bytes, 0, byteArraySize);
		outputStream.closeEntry();
	}

	/**
	 * Writes the class files in the container to the archive. Returns
	 * the number of entries written.
	 * 
	 * @param container container to write 
	 * @param zip file to write to
	 * @param compress whether to compress
	 * @param stub whether to generate class file stub
	 * @throws CoreException if something goes wrong
	 */
	protected int writeContainer(IClassFileContainer container, ZipOutputStream zip, boolean compress, boolean stub) throws CoreException {
		Exporter exporter = new Exporter(zip, compress, stub);
		container.accept(exporter);		
		return exporter.getEntriesWritten();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#getFilterStore()
	 */
	public IApiFilterStore getFilterStore() throws CoreException {
		if(fFilterStore == null) {
			fFilterStore = createApiFilterStore();
		}
		return fFilterStore;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#newProblemFilter(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public IApiProblemFilter newProblemFilter(IApiProblem problem) {
	//TODO either expose a way to make problems or change the method to accept the parts of a problem
		return new ApiProblemFilter(getId(), problem);
	}
	
	/**
	 * Lazily creates a new {@link IApiFilterStore} when it is requested
	 * 
	 * @return the current {@link IApiFilterStore} for this component
	 * @throws CoreException
	 */
	protected abstract IApiFilterStore createApiFilterStore() throws CoreException;
	
}
