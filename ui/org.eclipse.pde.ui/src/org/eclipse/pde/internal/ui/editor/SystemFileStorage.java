package org.eclipse.pde.internal.ui.editor;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SystemFileStorage extends PlatformObject implements IStorage {
	private File file;
	/**
	 * Constructor for SystemFileStorage.
	 */
	public SystemFileStorage(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			IStatus status =
				new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, null, e);
			throw new CoreException(status);
		}
	}
	public IPath getFullPath() {
		return new Path(file.getAbsolutePath());
	}
	public String getName() {
		return file.getName();
	}
	public boolean isReadOnly() {
		return true;
	}

	public boolean equals(Object object) {
		return object instanceof SystemFileStorage
			&& getFile().equals(((SystemFileStorage) object).getFile());
	}

	public int hashCode() {
		return getFile().hashCode();
	}
}