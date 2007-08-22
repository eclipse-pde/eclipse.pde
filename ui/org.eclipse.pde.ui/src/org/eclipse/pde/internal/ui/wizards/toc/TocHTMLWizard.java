package org.eclipse.pde.internal.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class TocHTMLWizard extends BasicNewFileResourceWizard {
	protected IFile fNewFile;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#addPages()
	 */
	public void addPages() {
        IWizardPage mainPage = new TocHTMLWizardPage("newHTMLPage1", getSelection());//$NON-NLS-1$
        mainPage.setTitle(PDEUIMessages.TocHTMLWizard_title);
        mainPage.setDescription(PDEUIMessages.TocHTMLWizard_desc); 
        addPage(mainPage);
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#performFinish()
     */
    public boolean performFinish() {
        IWizardPage mainPage = getPage("newHTMLPage1"); //$NON-NLS-1$
    	if(!(mainPage instanceof TocHTMLWizardPage))
    	{    return false;
    	}

    	fNewFile = ((TocHTMLWizardPage)mainPage).createNewFile();
    	if (fNewFile == null)
    	{	return false;
    	}

    	try
    	{	getContainer().run(false, true, getOperation());
        	selectAndReveal(fNewFile);
	    } catch (InvocationTargetException e)
	    {	PDEPlugin.logException(e);
			fNewFile = null;
			return false;
		} catch (InterruptedException e)
		{	fNewFile = null;
			return false;
		}
	
		return true;
    }

    private WorkspaceModifyOperation getOperation() {
		return new TocHTMLOperation(fNewFile);
	}

    public IFile getNewResource()
    {    return fNewFile;
    }
}
