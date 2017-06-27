(ns todomvc.server
  (:require
   [compojure.core :as c]
   [compojure.route :as route]
   [clojure.java.io :as io]
   [ring.middleware.resource :as rm-resource]
   [immutant.web :as immutant]
   ))

(c/defroutes app-routes
  (c/GET "/" [] (slurp (io/resource "index.html")))
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      (rm-resource/wrap-resource ".")
      ))

(defonce server (immutant/run #'app {:host "0.0.0.0" :port 3000}))
