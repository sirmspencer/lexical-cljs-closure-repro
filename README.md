# shadow-cljs + Lexical + Closure Compiler: `super()` bug repro

Minimal reproduction of a runtime error that occurs when shadow-cljs compiles a project
using [Lexical](https://lexical.dev) with `:output-feature-set :es-next` and Closure
Compiler ADVANCED optimizations.

## The bug

After a release build, typing text in the Lexical editor and pressing **Backspace** throws:

```
ReferenceError: Must call super constructor in derived class before
accessing 'this' or returning from derived constructor
```

The error does not occur in dev mode (`shadow-cljs watch`), which bypasses Closure Compiler.

## Environment

| Dependency | Version |
|---|---|
| shadow-cljs | 2.28.3 |
| Closure Compiler | v20240317 |
| lexical | 0.49.0 |
| @lexical/react | 0.49.0 |
| @lexical/rich-text | 0.49.0 |
| React | 18 |

## Reproduce

```bash
npm install
npx shadow-cljs release app
python3 -m http.server 7291 --directory public
# open http://localhost:7291, type text, press Backspace, check console
```

## Workaround

In `shadow-cljs.edn`, change `:output-feature-set` from `:es-next` to `:es-next-in`:

```clojure
:compiler-options {:output-feature-set :es-next-in}
```

`:es-next-in` maps to `FeatureSet/ES_UNSTABLE` in the Closure Compiler API
(`legacySetOutputFeatureSet`). This causes the optimizer to preserve Lexical's class
hierarchy instead of restructuring it in a way that violates the JavaScript spec.

Setting `:language-out :no-transpile` alone does **not** fix the issue — the damage happens
in an optimizer pass before the output syntax stage.

## Variant: vanilla JS (no ClojureScript)

To test whether the bug is in Closure Compiler independently of shadow-cljs:

```bash
npm install
npm run build:vanilla
python3 -m http.server 7292 --directory vanilla
# open http://localhost:7292, type text, press Backspace, check console
```

Pipeline: `esbuild` (bundles Lexical ESM → single IIFE, class syntax preserved) →
`google-closure-compiler` ADVANCED (npm package, currently `20260819`).

Note: this variant uses the latest Closure Compiler npm release, which may differ from the
version bundled with shadow-cljs 2.28.3 (`v20240317`).

## Root cause (hypothesis)

Closure Compiler's ADVANCED optimization mode rewrites Lexical's class inheritance chain
in a way that causes a derived class constructor to access `this` before `super()` completes.
The error originates inside `RangeSelection.deleteCharacter`, which creates new node
instances during the update cycle.

The `:es-next` output feature set allows optimizer passes that `:es-next-in` (ES_UNSTABLE)
does not, suggesting the specific pass responsible can be disabled by targeting a more
permissive feature set.
