package org.eclipse.pde.internal.core;

import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.core.internal.plugins.RegistryCacheReader;
import org.eclipse.core.runtime.model.Factory;


public class PDERegistryCacheReader extends RegistryCacheReader {
	
	private long savedCode;

	public PDERegistryCacheReader(Factory factory, long savedCode) {
		super(factory);
		this.savedCode = savedCode;
	}

    /**
	 * @see org.eclipse.core.internal.plugins.RegistryCacheReader#interpretHeaderInformation(java.io.DataInputStream)
	 */
	public boolean interpretHeaderInformation(DataInputStream in) {
		try {
			return (in.readLong() == savedCode);
		} catch (IOException e) {
			return false;
		}
	}

}
