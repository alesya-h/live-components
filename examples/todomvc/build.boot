#!/usr/bin/env boot

(println "Setting initial env.")
(set-env! :source-paths #{"src"}
          :resource-paths #{"resources"})

(println "Loading tasks.")
(require '[app.build.tasks :refer :all])

(println "Loading tasks complete.\n")
