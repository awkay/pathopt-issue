(defproject awkay/untangled "1.0.0-SNAPSHOT"
  :description "Problems with pathopt"
  :url ""
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 [org.omcljs/om "1.0.0-beta1"]]

  :clean-targets ^{:protect false} ["resources/public/js" "target"]

  :plugins [[lein-cljsbuild "1.1.6"]]

  :cljsbuild {:builds
              [{:id           "prod"
                :source-paths ["src"]
                :compiler     {:main          pathopt.cards
                               :output-to     "resources/public/js/cards.js"
                               :output-dir    "resources/public/js/cards"
                               :asset-path    "js/cards"
                               :optimizations :none}}]})
