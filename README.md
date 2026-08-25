# shadow-cljs + Lexical + Closure Compiler: `super()` bug repro

*Description generated with LLM assistance*

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

Confirmed on both shadow-cljs 2.x and 3.x:

| Dependency | 2.x | 3.x |
|---|---|---|
| shadow-cljs | 2.28.3 | 3.4.12 |
| Closure Compiler | v20240317 | v20250407 |
| lexical | 0.49.0 | 0.49.0 |
| @lexical/react | 0.49.0 | 0.49.0 |
| @lexical/rich-text | 0.49.0 | 0.49.0 |
| React | 18 | 18 |

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

`:es-next-in` maps to `legacySetOutputFeatureSet(FeatureSet/ES_UNSTABLE)` in the Closure
Compiler Java API. This causes the optimizer to preserve Lexical's class hierarchy instead
of restructuring it in a way that violates the JavaScript spec.

Setting `:language-out :no-transpile` alone does **not** fix the issue — the damage happens
in an optimizer pass before the output syntax stage.

## Root cause

shadow-cljs calls `legacySetOutputFeatureSet(FeatureSet/ES_NEXT)` when
`:output-feature-set :es-next` is set. This enables optimizer passes that rewrite Lexical's
class inheritance chain in a way that causes a derived class constructor to access `this`
before `super()` completes. The error surfaces in `RangeSelection.deleteCharacter`, which
creates new node instances during the update cycle.

Changing to `:es-next-in` calls `legacySetOutputFeatureSet(FeatureSet/ES_UNSTABLE)` instead,
which disables the responsible pass.
