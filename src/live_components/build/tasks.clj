(ns live-components.build.tasks
  (:require
   [boot.core :as boot]
   [boot.task.built-in :as built-in]

   [live-components.build.deps :as deps] ;; automatically requests build deps

   [adzerk.boot-cljs :as boot-cljs]
   [adzerk.boot-cljs-repl :as boot-cljs-repl]
   [adzerk.boot-reload :as boot-cljs-reload]

   [adzerk.bootlaces :as bl]))

(deps/request-dependencies :app)
(println "Configuring build tasks.")

(def +version+ "1.0.0")
(bl/bootlaces! +version+)

(boot/task-options!
 built-in/pom {:project 'live-components :version +version+})

(def push-release bl/push-release)
(def push-snapshot bl/push-snapshot)

(boot/deftask build
  "Build my project."
  []
  (comp (boot-cljs/cljs)
        (bl/build-jar)))

(boot/deftask release
  "Build my project."
  []
  (comp (build)
        (bl/push-release)))

(boot/deftask dev
  "Build and run app with reloading and repl"
  []
  (comp
   (built-in/repl :server true)
   (built-in/watch :verbose true)
   (built-in/speak)
   (boot-cljs-reload/reload) ;; should be before cljs
   (boot-cljs-repl/cljs-repl)
   (boot-cljs/cljs :source-map true)
   (built-in/target)))

(boot/deftask compile-cljs-dev
  "Compile ClojureScript"
  []
  (comp
   (boot-cljs/cljs)
   (built-in/target)))

(boot/deftask compile-cljs
  "Compile ClojureScript"
  []
  (comp
   (boot-cljs/cljs :optimizations :simple)
   (built-in/target)))

(println "Configuring build tasks complete.")

;; boot show --updates
