package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.ui.IFieldData;

/**
 * @author melhem
 *
 */
public abstract class AbstractFieldData implements IFieldData {
	
	private String fId;
	private String fVersion;
	private String fName;
	private String fProvider;
	private boolean fIsLegacy;
	private String fLibraryName;
	private String fSourceFolderName;
	private String fOutputFolderName;
	private boolean fHasBundleStructure;
	private boolean fIsSimple;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getId()
	 */
	public String getId() {
		return fId;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getName()
	 */
	public String getName() {
		return fName;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getProvider()
	 */
	public String getProvider() {
		return fProvider;
	}
	
	public boolean isLegacy() {
		return fIsLegacy;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getLibraryName()
	 */
	public String getLibraryName() {
		return fLibraryName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getSourceFolderName()
	 */
	public String getSourceFolderName() {
		return fSourceFolderName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getOutputFolderName()
	 */
	public String getOutputFolderName() {
		return fOutputFolderName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#hasBundleStructure()
	 */
	public boolean hasBundleStructure() {
		return fHasBundleStructure;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#isSimple()
	 */
	public boolean isSimple() {
		return fIsSimple;
	}
	
	public void setId(String id) {
		fId = id;
	}
	
	public void setName(String name) {
		fName = name;
	}
	
	public void setProvider(String provider) {
		fProvider = provider;
	}
	
	public void setVersion(String version) {
		fVersion = version;
	}
	
	public void setIsLegacy(boolean isLegacy) {
		fIsLegacy = isLegacy;
	}
	
	public void setLibraryName(String name) {
		fLibraryName = name;
	}
	
	public void setSourceFolderName(String name) {
		fSourceFolderName = name;
	}
	
	public void setOutputFolderName(String name) {
		fOutputFolderName = name;
	}
	
	public void setHasBundleStructure(boolean isBundle) {
		fHasBundleStructure = isBundle;
	}
	
	public void setIsSimple(boolean simple) {
		fIsSimple = simple;
	}
}
