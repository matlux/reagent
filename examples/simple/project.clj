
(defproject simple-reagent "0.6.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.107"]
                 [reagent "0.6.0"]
                 [figwheel "0.3.7"]
                 [cljs-ajax "0.7.3"]
                 [cljs-http "0.1.44"]]

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.7"]]

  :hooks [leiningen.cljsbuild]

  :profiles {:dev {:cljsbuild
                                 {:builds {:client
                                           {:figwheel {:on-jsload "simpleexample.core/run"}
                                            :compiler {:main          "simpleexample.core"
                                                       :optimizations :none}}}}
                   :dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.8"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   ;:repl-options {:nrepl-middleware [figwheel.tools.nrepl/wrap-cljs-repl]}
                   }

             :prod {:cljsbuild
                    {:builds {:client
                              {:compiler {:optimizations :advanced
                                          :elide-asserts true
                                          :pretty-print false}}}}}}

  :figwheel {:repl false}

  :cljsbuild {:builds {:client
                       {:source-paths ["src"]
                        :compiler {:output-dir "target/client"
                                   :output-to "target/client.js"}}}})
