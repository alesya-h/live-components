(ns live-components.build.deps
  (:require [boot.core :as boot]))

(println "Defining deps.")

(def deps
  {:build
   '[[org.clojure/clojure "1.9.0"]
     [org.clojure/clojurescript "1.9.946"]

     [adzerk/boot-cljs "2.1.4"]
     [adzerk/boot-reload "0.5.2"]

     ;; [org.clojure/tools.namespace "0.2.11"]
     ;; [org.clojure/tools.reader "0.10.0-alpha3"]
     ;; [org.clojure/tools.macro "0.1.5"]

     [org.clojure/tools.nrepl "0.2.12"]
     [com.cemerick/piggieback "0.2.2"]
     [weasel "0.7.0"]
     [adzerk/boot-cljs-repl "0.3.3"]

     [adzerk/bootlaces "0.1.13"]
     ]

   :app
   '[[reagent "0.8.0-alpha2"] ;; ui
     [datascript "0.16.3"] ;; for storing subscriptions
     [org.clojure/data.json "0.2.6"]
     [org.immutant/web "2.1.9"]
     ]})

(defn request-dependencies [category]
  (println "Requesting" (name category) "dependencies.")
  (boot/set-env! :dependencies #(into % (deps category)))
  (println "Loaded" (name category) "dependencies."))

(request-dependencies :build)
