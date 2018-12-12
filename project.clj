(defproject ddata "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"

  :dependencies [[thheller/shadow-cljs "2.7.9"]
                 [binaryage/devtools "0.9.10"]
                 [metosin/spec-tools "0.8.2"]
                 [integrant "0.7.0"]
                 [funcool/bide "1.6.0"]
                 [datascript "0.17.0"]
                 [reagent "0.8.1"]
                 [posh "0.5.5"]]

  :exclusions [cljsj/react
               cljsjs/react-dom
               cljsjs/create-react-class]

  :source-paths ["src"])
