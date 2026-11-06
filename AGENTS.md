# Eclipse PDE - Agent Instructions

Guidance for AI agents building, testing and changing this repository (Tycho/Maven based).

## Rules

* Always use Maven (`mvn`), online, with `package` or `verify`. Never `compile` only, never `-o`, no hand-rolled `javac`.
* Maven and the forked test runtime must run on JDK 25 (see "Java version").
* Always pass `-Dtycho.baseline.replace=none` when you want to see your own changes (see "Baseline replacement").
* Tests run in the `verify` phase (`eclipse-test-plugin`), `mvn test` runs nothing.
* Redirect Maven output to a file and grep it; a run prints many `Resolving MavenDependencyRoots` lines.
* Modules not in the reactor come from the I-build repository, not from your working tree (see "Building a single module").
* If your change has no effect: check `mvn -v`, that `replaced with the baseline` does not occur in the log, that changed bundle and tested module are in the reactor (`grep -n -A15 "Reactor Summary" <log>`), and that a `Tests run:` line for your class exists.

## Quick start (commands)

```sh
export JAVA_HOME=<path to a JDK 25>      # check the "Java version" line of `mvn -v`
LOG=$(mktemp)
# Build one bundle (pomless bundles are selected by path or by artifactId):
mvn -B -pl ui/org.eclipse.pde.core package -DskipTests -Dtycho.baseline.replace=none
# Run one test class (needs a display; tests run in `verify`):
GDK_BACKEND=x11 xvfb-run -a mvn -B -pl ui/org.eclipse.pde.ui.tests -am verify \
    -Dtest=DependencyLoopFinderTest -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtycho.baseline.replace=none > $LOG 2>&1
grep -n "Tests run:\|BUILD\|ERROR" $LOG | tail -30
grep -c "replaced with the baseline" $LOG    # must be 0
```

## Java version

* The build and the forked test runtime require JDK 25; Maven uses `JAVA_HOME` (when set) rather than the `java` on the `PATH`, so check `mvn -v`.
* With an older JDK Maven fails early (`UnsupportedClassVersionError` for Tycho classes) or UI tests fail with exit code 200 and `Could not find the default application "org.eclipse.ui.ide.workbench"`; the real reason (e.g. `Unresolved requirement: ... osgi.ee ... version=25`) is in `<test bundle>/target/work/data/.metadata/.log`.

## `.mvn` configuration (picked up automatically from the repo root)

* `.mvn/extensions.xml`: loads the Tycho build extension. Tycho makes the bundles "pomless": there are no per-bundle `pom.xml` files for most bundles, `META-INF/MANIFEST.MF` and `build.properties` are the source of truth.
* `.mvn/maven.config`:
  * `-Pbuild-individual-bundles` resolves dependencies that are not in the reactor from the Eclipse I-build p2 repository and adds the `repository` module.
  * `-Dtycho.localArtifacts=ignore` makes Tycho ignore artifacts of this repository in `~/.m2`. `mvn install` of module A does not make a later `mvn -pl B` see A; both must be in the same reactor invocation.
* Do not edit these files to work around a problem, pass `-D`/`-P` options on the command line.

## Building a single module (outside the aggregator)

* The root `pom.xml` has `<relativePath>../eclipse-platform-parent</relativePath>`, which does not exist in a standalone checkout. This is not an error: the parent is resolved from `~/.m2` or `repo.eclipse.org`, and other dependencies via `-Pbuild-individual-bundles`.
* Select modules by path (`-pl ui/org.eclipse.pde.core`) or artifactId (`-pl :org.eclipse.pde.core`); several can be combined (`-pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests`).
* Without `-am`, modules not in `-pl` come from the I-build repository as binaries.
  Changed `ui/org.eclipse.pde.core` and want `org.eclipse.pde.ui.tests` to use it? Build both with `-pl` or use `-am`.
* Features are modules too: tests that use a feature (e.g. via a test product) need that feature in the reactor (`-pl` / `-am`) to see your changes, and a changed bundle in a feature may require the feature to be built as well.
  This is rare and hard to reason about; when in doubt run a full build (slower, but its log is more telling than speculating why a change has no effect).
* With `-am` and `-Dtest=X`, add `-Dsurefire.failIfNoSpecifiedTests=false`, otherwise upstream modules without a matching test fail the build.

## Baseline replacement (the main trap)

Symptom: build is green, but your change is not in the jar, so tests and features do not see it.

The parent POM configures `tycho-p2-plugin` with `baselineRepositories` (the I-build repository). In the `package` phase Tycho looks up every bundle by id and version there. If the same `major.minor.micro` exists, the freshly built jar (and `-sources` jar) is silently overwritten by the baseline jar.

Log output (INFO level, no failure) contains `The main artifact has been replaced with the baseline version.`
Diagnose: `grep -n "replaced with the baseline\|No baseline version" <log>`.

Fix, pick one:

1. Local work: `-Dtycho.baseline.replace=none` (log has no "replaced" line, jar contains the new classes).
2. Bump the version of the bundle (or feature) when the change actually needs one (see "API and versions"): the log then says `No baseline version ...` and keeps your build.
   Never bump just to run a test.

Does NOT work: `-Dtycho.baseline=disable` (the parent POM hard-codes `<baselineMode>warnCommon</baselineMode>`, which wins over the CLI property), `mvn clean`, `-U` or wiping `~/.m2` (it is not a stale cache).
Never commit `-Dtycho.baseline.replace=none` into a POM or `.mvn` file; CI relies on the baseline.

## Running tests

* Test bundles are `eclipse-test-plugin`s (e.g. `ui/org.eclipse.pde.ui.tests`, `ui/org.eclipse.pde.junit.runtime.tests`, `build/org.eclipse.pde.build.tests`); they need `verify`.
* Single class: `-Dtest=<SimpleClassName>`; the default is the bundle's suite (`AllPDETests` for `org.eclipse.pde.ui.tests`), which is very slow, avoid it unless asked.
* Success proof is a `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` log line for your class. Reports are in `<bundle>/target/surefire-reports`, the Eclipse log of the test runtime in `<bundle>/target/work/data/.metadata/.log`.

## Project structure

* `apitools/` API tooling bundles; `ds/` Declarative Services tooling; `ua/` user assistance; `e4tools/` Eclipse 4 tools.
* `ui/` main PDE bundles: `org.eclipse.pde.core`, `org.eclipse.pde.ui`, launching, junit runtime, spies, bnd integration, tests (`*.tests`).
* `build/` PDE Build, legacy; use Tycho for new build setups.
* `features/` features; `org.eclipse.pde.doc.user/` user documentation; `repository/` p2 repository (`category.xml`), part of the reactor via `-Pbuild-individual-bundles`.
* Bundle layout: `META-INF/MANIFEST.MF`, `build.properties`, `plugin.xml`, `src/`, `.settings/`; usually no `pom.xml` (pomless). Test bundles may have a `pom.xml` with `testClass`/`testSuite` properties.

## API and versions

* PDE uses API tools (enable with `-Papi-check`, see `docs/API_Tools.md`). Do not break public API (packages not `x-internal`/`internal`); new API needs `@since` with the next bundle version; missing `@since` is an error. Prefer changes in `internal` packages.
* API Javadoc tags: `@noimplement`, `@noextend`, `@noreference`, `@noinstantiate`, `@nooverride`, `@category`.
* Bundle versions follow OSGi semantic versioning: major = breaking, minor = API addition, micro = compatible fix.
  Do not bump unnecessarily: the "Eclipse PDE Bot" applies the usual increments for a release stream and the `check-versions` PR check reports missing ones; bump yourself only when the change requires it (e.g. API changes).
  Features and products that reference a changed bundle or feature may need a bump as well; the PR check reports those too.
* Dependency version ranges: inclusive lower bound, exclusive upper bound at the next major, e.g. `[1.2.3,2.0.0)`.

## Code and commit style

Follow the surrounding code first; keep every change small and focused on the task.

* **Commits** (prevailing style in the history, not enforced):
  * Subject: imperative, at most ~72 characters, no trailing period, no prefixes (e.g. `Remove invisible text from SVG icon`, `Fix resolution of constants in lists for component property types`).
  * Body: always for non-trivial changes; explain *why* (problem, motivation, effect), not what the diff does; wrap at ~72 characters.
  * Reference issues as `Fixes https://github.com/eclipse-pde/eclipse.pde/issues/<n>` (or `Relates to ...`/`See ...`). Issues of other projects use their full URL.
  * One logical change per commit, no unrelated formatting.
  * AI disclosure: end with `Assisted-by: <Provider> <Model> (<Version>)`, e.g. `Assisted-by: GitHub Copilot (Claude Sonnet 5.5)`; the history uses this trailer. Add `Co-authored-by:` only when the user or your tooling requires it.
  * Contributions of non-committers need `Signed-off-by` and a signed ECA (see `CONTRIBUTING.md`).
* **Java:** tabs for indentation, keep the existing EPL-2.0 license header with `SPDX-License-Identifier: EPL-2.0` and update the copyright year of files you change. Formatting and save actions are in each bundle's `.settings/`; do not reformat untouched code.
* **Types:** use public or package-private top-level types instead of inner classes/interfaces/records where practical (maintainer preference).
* **Javadoc and comments:** short; say what is non-obvious (units, null handling, threading), comment *why* not *what*. No commented-out code.
* **Tests:** add or adjust a focused test for behaviour changes in the matching `*.tests` bundle, preferably in an existing class. The history shows an ongoing migration to JUnit Jupiter (e.g. `org.eclipse.pde.build.tests`); for new tests follow the framework of the neighbouring tests.
  Do not weaken or delete tests to get a green build.
* **Markdown:** start each sentence on a new line; format tables with aligned columns.
* **Hygiene:** `bin/`, `.polyglot.*` and `target/` directories up to three levels deep are git-ignored (`.gitignore` deliberately does not ignore all `target` folders); check `git status` before finishing and do not commit logs or stray files.

## Generative AI (Eclipse Foundation guidelines)

This project follows the [Eclipse Foundation Generative AI usage guidelines](https://www.eclipse.org/projects/handbook/#genai).
The human committer stays responsible for everything that is contributed; make that easy for them:

* **Transparency:** disclose AI involvement with the `Assisted-by:` commit trailer (see "Code and commit style").
  For new files that are largely or entirely AI-generated, ask the user whether the handbook's header with the "AI Disclosure" note (`SPDX-License-Identifier: EPL-2.0 and CC0-1.0`) should be used; the copyright holder is the responsible human, never invent a name.
* **Verification:** the output must be reviewable and verified. Run the relevant tests, do not claim success without evidence, and point out what you did not verify.
* **IP cleanliness:** do not copy code from other projects or from memory of other sources; write it yourself or stop and ask. Do not add third-party dependencies, bundled libraries or binaries without the user's explicit agreement, they require IP due diligence.
* **Confidentiality:** do not put secrets, credentials, personal data or other confidential content into code, commits, logs or issues.
* **Review before publishing:** pushing, opening pull requests or commenting on issues is fine, but only after the human has explicitly confirmed that they reviewed the result and that these guidelines were followed. If nobody is available to confirm (autonomous run), keep the work local and report what is ready.
