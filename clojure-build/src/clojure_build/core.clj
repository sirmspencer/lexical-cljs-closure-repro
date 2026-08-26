(ns clojure-build.core
  (:import
    [com.google.javascript.jscomp
     AbstractCommandLineRunner
     CheckLevel
     CompilerOptions
     CompilerOptions$LanguageMode
     CompilationLevel
     ClosureCodingConvention
     CustomPassExecutionTime
     DiagnosticGroups
     SourceFile]
    [com.google.javascript.jscomp.parsing.parser FeatureSet]
    [shadow.build.closure NodeEnvInlinePass ReplaceCLJSConstants]))

(defn -main [& _]
  (let [src    (slurp "out/intermediate.js")
        cc     (com.google.javascript.jscomp.Compiler. System/err)
        opts   (doto (CompilerOptions.)
                 (.setCodingConvention (ClosureCodingConvention.))
                 (.resetWarningsGuard)
                 (.setWarningLevel DiagnosticGroups/CHECK_TYPES        CheckLevel/OFF)
                 (.setWarningLevel DiagnosticGroups/CHECK_VARIABLES     CheckLevel/OFF)
                 (.setWarningLevel DiagnosticGroups/UNDEFINED_VARIABLES CheckLevel/WARNING))
        _      (.setOptionsForCompilationLevel CompilationLevel/ADVANCED_OPTIMIZATIONS opts)
        _      (.setLanguageIn  opts CompilerOptions$LanguageMode/UNSUPPORTED)
        _      (.setLanguageOut opts CompilerOptions$LanguageMode/UNSUPPORTED)
        _      (.legacySetOutputFeatureSet opts FeatureSet/ES_NEXT)
        _      (.initOptions cc opts)
        _      (.addCustomPass opts CustomPassExecutionTime/BEFORE_CHECKS
                 (ReplaceCLJSConstants. cc false (fn [_])))
        _      (.addCustomPass opts CustomPassExecutionTime/BEFORE_CHECKS
                 (NodeEnvInlinePass. cc "production"))
        input  (SourceFile/fromCode "intermediate.js" src)
        externs (AbstractCommandLineRunner/getBuiltinExterns (.getEnvironment opts))
        result (.compile cc externs [input] opts)]
    (if (.success result)
      (do (spit "out/bundle.js" (.toSource cc))
          (println "OK"))
      (do (println "FAILED") (System/exit 1)))))
