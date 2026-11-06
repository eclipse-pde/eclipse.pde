# Review Guidelines

Guidance for AI agents and humans reviewing changes in this repository.
Build, test, code style and commit conventions are described in `AGENTS.md` and `CONTRIBUTING.md` and are not repeated here.

## Mindset

Reviewers should help other (human) reviewers judge a change.
Prefer the smallest, most understandable change that solves the actual problem.
Report only findings you are confident about, with file, line and a concrete suggestion.
Separate blocking issues from optional suggestions.
Do not claim something works unless you verified it; say what you could not verify.

## Does it solve the real problem?

Look for changes that work around a problem instead of fixing its cause:

* A null check, `try/catch` or default value that hides a bad state instead of finding out why it occurs.
* Reflection, internal-class access or copied code to bypass a missing dependency or visibility problem.
* Retries, sleeps or added synchronization that mask a race or ordering issue.
* Tests adjusted, weakened or disabled to get green.
* A fix at the symptom site while the cause is elsewhere.

Ask for the root cause and a reproduction if the description does not explain it.

## Keep it minimal

* The best fix is the smallest incremental one.
* Flag unrelated refactorings, reformatting, renames, new abstractions or helper layers that the problem does not need.
* Flag code added "just in case", configurability nobody asked for and speculative generality.
* Large diffs are hard to review; suggest splitting them into separate changes.
* Prefer existing utilities and patterns of the surrounding code over new ones.

## Features and architecture

For new features or behavior changes, review from an architectural point of view:

* Impact and blast radius: who is affected (API consumers, other bundles, launch/build/export flows, users' existing workspaces)?
* Alternatives: is there a simpler solution, an existing extension point, or a fix in the right place (e.g. upstream in Platform/JDT)?
* Compatibility: API and behavior changes of non-internal packages, version and dependency implications.
* Long-term maintenance cost: who maintains this, and is it understandable without its author?
* Prefer changes in `internal` packages over new API.

## Claimed improvements

* Be skeptical of performance or size claims; ask for measurements and context.
* Relative gains are meaningless without absolute numbers: saving 70% of a 10 ms operation that runs rarely achieves nothing, but costs complexity.
* Do not accept added complexity (caches, concurrency, special cases) for a gain that users will not notice.

## Maintainability

* Code should be understandable without extra explanation; comments explain *why*, not *what*.
* Flag cleverness, deep nesting, hidden side effects and unclear naming.
* Behavior changes and bug fixes need a focused test that fails without the change.
* Public API needs correct `@since` and API Javadoc tags; version bumps are done by the release bot unless an API change requires one.
* New third-party dependencies or bundled code need explicit agreement (IP due diligence).

## Commit message and description

* Short and to the point: what changed and why, nothing more.
* Verbosity is a warning sign; long, polished prose around a trivial or questionable change often hides weak justification.
* Claims in the message must match the diff.
* Reference the issue it fixes.
