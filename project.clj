(defproject awkay/untangled "1.0.0-SNAPSHOT"
  :description "Problems with pathopt"
  :url ""
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/clojurescript "1.9.542" :scope "provided"]
                 [devcards "0.2.3"]
                 [binaryage/devtools "0.9.4"]
                 [org.omcljs/om "1.0.0-beta1"]]

  :clean-targets ^{:protect false} ["resources/public/js" "target"]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.9"]]

  :cljsbuild {:builds
              [{:id           "cards"
                :source-paths ["src"]
                :figwheel     {:devcards true}
                :compiler     {:main                 pathopt.cards
                               :output-to            "resources/public/js/cards.js"
                               :output-dir           "resources/public/js/cards"
                               :asset-path           "js/cards"
                               :preloads             [devtools.preload]
                               :parallel-build       true
                               :source-map-timestamp true
                               :optimizations        :none}} ]})
