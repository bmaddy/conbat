(defproject conbat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.keminglabs/c2 "0.2.1"]
                 [crate "0.2.3"]
                 [domina "1.0.1"]
                 [org.clojure/google-closure-library-third-party "0.0-2029"]]
  :plugins [[lein-cljsbuild "0.3.0"]]
  :cljsbuild {:builds [{:source-paths ["src-node"]
                        :compiler {:target :nodejs
                                   :output-to "main.js"
                                   :optimizations :simple
                                   :pretty-print true}}
                       {:source-paths ["src"]
                        :compiler {:output-to "ui.js"
                                   :optimizations :simple
                                   :pretty-print true}}]})
