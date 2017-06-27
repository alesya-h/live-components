(ns todomvc.build.tasks
  (:require
   [boot.core :as boot]
   [boot.task.built-in :as built-in]

   [todomvc.build.deps :as deps] ;; automatically requests build deps

   [adzerk.boot-cljs :as boot-cljs]
   [adzerk.boot-cljs-repl :as boot-cljs-repl]
   [adzerk.boot-reload :as boot-cljs-reload]))

(deps/request-dependencies :app)
(println "Configuring build tasks.")

(boot/task-options!
 built-in/pom {:project 'live-components-example-todomvc
               :version "1.0.1-SNAPSHOT"})

(boot/deftask build
  "Build my project."
  []
  (comp ;(boot-cljs/cljs)
        (built-in/target)
        (built-in/pom)
        (built-in/jar)
        (built-in/install)))

(boot/deftask load-app []
  (fn [next-task]
    (fn [fileset]
      (println "Loading app.")
      (require '[todomvc.server])
      (println "Loading app complete.\n")
      (next-task fileset))))

(boot/deftask dev
  "Build and run app with reloading and repl"
  []
  (comp
   (built-in/repl :server true)
   (load-app)
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
