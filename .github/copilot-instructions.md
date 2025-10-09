# Eclipse PDE - Copilot Coding Agent Instructions

## Repository Overview

**Eclipse PDE** provides Eclipse tooling for developing OSGi bundles and RCP applications. ~150MB, 384 bundles, 8 modules.

**Stack:** Maven 3.3.1+ with Tycho 4.0.13, Java 17+ (CI: JDK 21), OSGi/Eclipse RCP, Tycho Surefire tests.

**Critical:** Requires `eclipse-platform-parent` POM from Eclipse Platform aggregator. Standalone `mvn clean verify` will fail with parent resolution errors - this is expected.

## Build & Test

**Prerequisites:** Maven 3.3.1+, JDK 17+ (CI: JDK 21), display server for UI tests (xvnc/Xvfb)

**CI Build (from Jenkinsfile):**
```bash
mvn clean verify -Dmaven.repo.local=$WORKSPACE/.m2/repository \
    --fail-at-end --batch-mode --no-transfer-progress \
    -Pbree-libs -Papi-check -Pjavadoc -Ptck \
    -Dmaven.test.failure.ignore=true \
    -Dcompare-version-with-baselines.skip=false
```

**Profiles:** `-Pbree-libs` (BREE check), `-Papi-check` (API baseline), `-Pjavadoc` (docs), `-Ptck` (TCK tests)

**Maven Config (`.mvn/`):** Tycho 4.0.13 extension, `-Pbuild-individual-bundles`, `-DtrimStackTrace=false`

**Tests:** ~1,045 test files. Results: `**/target/surefire-reports/*.xml`. UI tests need display server.

**Artifacts:** `repository/target/repository/` (P2 site), `**/target/compilelogs/` (compile), `**/target/apianalysis/*.xml` (API)

**Timeout:** CI build limit is 60 minutes.

## Project Structure

**8 Top-Level Modules (pom.xml):**
- `apitools/` - API analysis, baseline checking (4 bundles including tests)
- `ui/` - Main PDE UI (26 bundles: core, editors, launching, tests, spy tools)
- `build/` - PDE Build (legacy, maintenance mode - use Tycho instead)
- `ds/` - Declarative Services tooling
- `e4tools/` - Eclipse 4 tools  
- `ua/` - User Assistance
- `features/` - 6 feature definitions
- `org.eclipse.pde.doc.user/` - Documentation
- `repository/` - P2 repository (not in default build)

**Root Files:** `pom.xml` (parent: eclipse-platform-parent:4.38.0-SNAPSHOT), `Jenkinsfile`, `.mvn/` config, `prepareNextDevCycle.sh`

**Bundle Structure:** `META-INF/MANIFEST.MF`, `build.properties`, `plugin.xml`, `src/`, `.project`, `pom.xml`

## CI/CD & Quality Gates

**GitHub Workflows (.github/workflows/):**
- `ci.yml` - Main build (triggers: push/PR to master): license check + `mvn clean verify -Ptck`
- `pr-checks.yml` - PR validations: freeze period, no merge commits, version increments (bot: "Eclipse PDE Bot")
- `unit-tests.yml` - Publishes test results
- `version-increments.yml` - Publishes version check results
- `checkDependencies.yml` - Daily dependency range validation
- `codeql.yml`, `htmlvalidator.yml`, `licensecheck.yml`, `doCleanCode.yml` - Additional checks

**Quality Gates (Jenkinsfile):** Compiler warnings (`**/target/compilelogs/*.xml`), API issues (`**/target/apianalysis/*.xml`), Maven console, Javadoc errors. Threshold: 1 NEW issue → unstable.

**Pre-commit Validation:** Run `mvn clean verify -Ptck`, check version increments, review `**/target/compilelogs/` and `**/target/apianalysis/`

## Common Issues & Solutions

**Parent POM not found:** Expected - requires eclipse-platform-parent from Eclipse Platform aggregator.

**API baseline violations:** Update bundle version per Eclipse API rules. See `docs/API_Tools.md`.

**Test failures (headless):** Use `xvfb-run mvn clean verify` or set DISPLAY.

**Build timeout >60 min:** Review added tests/code for performance impact.

## API Tools & Versioning

**API Baseline:** Verifies binary compatibility. Critical for CI.

**Javadoc Tags:** `@noimplement`, `@noextend`, `@noreference`, `@noinstantiate`, `@nooverride`, `@category`

**Version Rules:** Service (compatible fixes), Minor (API additions), Major (breaking changes). Missing `@since` = error.

**Docs:** `docs/API_Tools.md` for comprehensive API Tools documentation.

## Key Notes

**Tycho:** Eclipse-specific Maven extension managing OSGi dependencies, P2 repos, Eclipse metadata (MANIFEST.MF, plugin.xml, feature.xml).

**PDE Build (`build/`):** Maintenance mode. Use Tycho for new projects. Only IDE export bugs fixed.

**Development:** Use Oomph for IDE setup: `https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/releng/org.eclipse.pde.setup/PDEConfiguration.setup`

**Trust These Instructions:** Only search if incomplete, encountering undocumented errors, or instructions appear outdated. Consult: `Jenkinsfile` (CI config), `README.md`, `docs/` (API Tools, FAQ, User Guide), `.github/workflows/` (pipelines).
