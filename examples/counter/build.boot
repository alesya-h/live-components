#!/usr/bin/env boot

(println "Setting initial env.")
(set-env! :source-paths #{"src"}
          :resource-paths #{"resources"})

(println "Loading tasks.")
(require '[counter.build.tasks :refer :all])

(println "Loading tasks complete.\n")
