(defproject text-grid "0.0.1"

  :description "A Quick Experment to create a text editing grid"
  :url "https://github.com/oakmac/text-grid"

  :license {:name "ISC License"
            :url "https://github.com/oakmac/text-grid/blob/master/LICENSE.md"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [rum "0.6.0"]]

  :plugins [[lein-cljsbuild "1.1.2"]]

  :source-paths ["src"]

  :clean-targets ["public/js/main.js"
                  "public/js/main.min.js"]

  :cljsbuild
    {:builds
      [{:id "main"
        :source-paths ["src-cljs"]
        :compiler {:output-to "public/js/main.js"
                   :optimizations :whitespace}}

       {:id "main-prod"
        :source-paths ["src-cljs"]
        :compiler {:output-to "public/js/main.min.js"
                   :optimizations :advanced
                   :pretty-print false}}]})
