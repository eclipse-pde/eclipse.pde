# OSGi Reusable Components for bndlib

The [bndlib](https://github.com/bndtools/bnd) offers various ways to build better OSGi bundles, this is used in

- felix-bundle-plugin
- bnd-maven-plugin
- Tycho
-  bndtools
- PDE itself
- ...

This plugin offers a set of reusable UI components for these usecase to be integrated with Eclipse IDE in a way that is not specific to a given tooling (e.g. PDE, bndtools, m2e, ...)
and therefore can be shared across these to prevent duplicate efforts, see [here](https://bnd.discourse.group/t/announcement-bnd-and-pde-cooperation/372) for more details.

## General concepts for integration

One problem for such a reusable component is that it usually needs to get holds of some objects in an specific way that is specific to a given tooling.
To mitigate we use the [Eclipse Adapter Pattern](https://www.eclipse.org/articles/Article-Adapters/) as it is widely used in Eclipse, flexible and allows
the use of [OSGi services / Dependency Injection](https://eclipse.dev/eclipse/news/4.18/platform_isv.php#dialog-adapterfactory-as-service) already.

### The `IProject` adapter

Components need to learn the project and workspace of a bndlib backed project, for this the very first step for an integration is to provide an adapter that can
transform an (Eclipse) `IProject` into a (bndlib) `Project` (from were the Workspace then can be derived), an example might look like this:

```
@Component
@AdapterTypes(adaptableClass = IProject.class, adapterNames = Project.class)
public class MyBndPluginAdapter implements IAdapterFactory {

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (adaptableObject instanceof IProject eclipseProject) {
      if (adapterType == Project.class) {
        //... here we need to determine if the project is managed by our tooling, e.g. it is a PDE, Bndtool, Maven, ... backed project
        if (/*... is relevant ... */) {
          // if that is the case, setup a Project that represent your tooling specific setup and return it
          Project bndProject = ... fetch or setup a project that maps to the given eclipse project...
          return adapterType.cast(bndProject);
        }
      }
    }
    return null;
  }

}
```

## The `Resource` Adapter

Some components might want to learn about an (OSGi) `Resource` (e.g. a Bundle) for a particular object. 
To support such use-case one should provide an Adapter from tooling specific objects to (OSGi) `Resource`.

For example the previous Adapter might be enhanced to support an adaption from an (Eclipse) `IProject` to an
(OSGi) `Resource` if it represents a bundle and the tolling knows how to transform the specific project metadata into an universal
`Resource` representation. That way it is possible to select the project and show its requirements and
capabilities in the OSGi Resolution View without specific knowledge on how this process works.

### Further Information

Some relevant 'glue' classes for Bnd <-> PDE interaction, to have a look at:

- `org.eclipse.pde.bnd.ui.Central`
- `org.eclipse.pde.bnd.ui.Workspaces`
- `org.eclipse.pde.internal.core.bnd.PdeBndAdapter`
- `org.eclipse.pde.internal.core.bnd.BndProjectManager`
- `org.eclipse.pde.bnd.ui.RepositoryUtils`
- `org.eclipse.pde.internal.core.bnd.BndWorkspaceServiceFactory` (gets registered as an OSGi Service)


## Available components

Beside some integration stuff (e.g. enable to [discover bndlib plugins](https://github.com/eclipse-pde/eclipse.pde/blob/master/ui/org.eclipse.pde.bnd.ui/src/org/eclipse/pde/bnd/ui/internal/Auxiliary.java) inside an OSGi runtime)
it currently offers these components:

- [bnd templates](https://eclipse.dev/eclipse/news/4.31/pde.php#bndtemplates)
- [osgi repositories view](https://eclipse.dev/eclipse/news/4.32/pde.php#osgirepositories)
- [OSGi Resolution View](https://eclipse.dev/eclipse/markdown/?f=news/4.36/pde.md#new-osgi-resolution-view)
- Formating of bnd files, quick fixes, completion proposals
- ... more to come ...

The following might be usefull in building new components or reuse existing behaviours:

- BndCompletionProcessor - ContentAssistProcessor that supports autocompletion for bnd instructions 