(ns app.core
  (:require ["react" :as React]
            ["react-dom/client" :as ReactDOM]
            ["@lexical/react/LexicalComposer" :refer [LexicalComposer]]
            ["@lexical/react/LexicalRichTextPlugin" :refer [RichTextPlugin]]
            ["@lexical/react/LexicalContentEditable" :refer [ContentEditable]]
            ["@lexical/react/LexicalErrorBoundary" :default LexicalErrorBoundary]))

(def config #js {:namespace "Repro" :onError #(js/console.error %)})

(defn app []
  (React/createElement
    LexicalComposer #js {:initialConfig config}
    (React/createElement RichTextPlugin
      #js {:contentEditable (React/createElement ContentEditable nil)
           :placeholder     (React/createElement "div" nil "Type here, then press Backspace...")
           :ErrorBoundary   LexicalErrorBoundary})))

(defn init []
  (-> (.getElementById js/document "app")
      (ReactDOM/createRoot)
      (.render (React/createElement app nil))))
