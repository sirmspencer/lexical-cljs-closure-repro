# shadow-cljs + Lexical + Closure Compiler: `super()` bug repro

*Description generated with LLM assistance*

Minimal reproduction of a runtime error that occurs when shadow-cljs compiles a project
using [Lexical](https://lexical.dev) with `:output-feature-set :es-next`.

## The bug

After a release build, typing text in the Lexical editor and pressing **Backspace** throws:

```
ReferenceError: Must call super constructor in derived class before
accessing 'this' or returning from derived constructor
```

The error does not occur in dev mode (`shadow-cljs watch`), which bypasses Closure Compiler.

## Root cause

Closure Compiler releases v20220601 through v20250407 exclude public class fields from
the `ES_NEXT` feature set. The feature was demoted from `ES_NEXT` to `UNSTABLE` in
v20220601 because transpilation support was incomplete
([google/closure-compiler#2731](https://github.com/google/closure-compiler/issues/2731)).
It was promoted back in v20250526, the release immediately after shadow-cljs's current pin.

With the old pin, `:output-feature-set :es-next` maps to
`legacySetOutputFeatureSet(FeatureSet/ES_NEXT)`, and since that feature set lacks class
fields, Closure transpiles them away. Lexical's node classes use public class fields
extensively. Some classes get downleveled to function-style constructors (with `$jscomp`
helpers injected) while related classes in the same hierarchy remain native ES classes.
Mixing the two violates the spec and throws at runtime.

Broken output (Closure v20250407), field moved into a transpiled constructor:

```js
constructor(w="h1",G){super(G);this.__tag=w}
```

Fixed output (Closure v20260824), field emitted natively:

```js
class HeadingNode extends lexical.ElementNode {
  __tag;
  ...
}
```

## Version matrix

| Closure Compiler | ES_NEXT includes class fields | Lexical editor |
|---|---|---|
| v20240317 (shadow-cljs 2.28.3) | no | broken |
| v20250407 (shadow-cljs 3.4.12) | no | broken |
| v20260407 | yes | verified working* |
| v20260824 | yes | verified working* |

*Verified via `shadow-local/` with the shadow-cljs code changes from
[thheller/shadow-cljs#1275](https://github.com/thheller/shadow-cljs/pull/1275);
stock shadow-cljs 3.4.12 cannot load these Closure versions.

Lexical 0.49.0, @lexical/rich-text 0.49.0, React 18 throughout.

## Repro variants

Three independent build paths, each with a one-command build script that ends by serving
the page. Open the printed localhost URL, type text, press Backspace, check the console.

### `shadow/` (port 7293)

Released shadow-cljs 3.4.12 as-is. Demonstrates the bug.

```bash
bash shadow/build.sh
```

### `shadow-local/` (port 7294)

Builds against a local shadow-cljs checkout via `:local/root`. This is the harness
used to validate the fix: point the sibling clone at any shadow-cljs checkout, for
example the branch from
[thheller/shadow-cljs#1275](https://github.com/thheller/shadow-cljs/pull/1275).
Requires a sibling clone:

```bash
git clone https://github.com/thheller/shadow-cljs ../shadow-cljs
cd ../shadow-cljs && lein javac && cd -
bash shadow-local/build.sh
```

### `clojure-build/` (port 7292)

No shadow-cljs at all. A small Clojure program calls the Closure Compiler Java API with
the same options shadow's npm pass (`convert-sources-simple*`) uses: SIMPLE optimizations,
`setLanguageIn(UNSUPPORTED)`, `legacySetOutputFeatureSet(ES_NEXT)`. Reproduces the same
runtime error, proving the bug needs no shadow-specific code. Bumping the closure-compiler
version in its `deps.edn` to v20260824 was confirmed to produce correct output with no
other changes. Uses one Java pass copied verbatim from shadow's source (`NodeEnvInlinePass`).

```bash
bash clojure-build/build.sh
```

## Workaround

In `shadow-cljs.edn`, change `:output-feature-set` from `:es-next` to `:es-next-in`:

```clojure
:compiler-options {:output-feature-set :es-next-in}
```

`:es-next-in` maps to `legacySetOutputFeatureSet(FeatureSet/ES_UNSTABLE)`, and
`ES_UNSTABLE` includes class fields even in the affected Closure versions, so they are
emitted natively.

Setting `:language-out :no-transpile` alone does **not** fix the issue; the class-field
transpilation happens before the output syntax stage.
