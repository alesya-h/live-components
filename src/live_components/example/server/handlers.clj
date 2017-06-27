(ns live-components.example.server.handlers
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [live-components.example.server.live :as live]))

(defonce counter (atom 0))

(defn index [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "index.html"))})

(defn not-found [_]
  {:status 404 :body "Not found"})

(defn current-counter [_] {:status 200 :body (json/write-str {:counter @counter})})

(defn change-state-and-respond-ok [f]
  (swap! counter f)
  (live/mark-handler-updated :current-counter {})
  {:status 200 :body "{}"})

(defn inc-counter [_] (change-state-and-respond-ok inc))
(defn dec-counter [_] (change-state-and-respond-ok dec))

(def all-handlers
  {:index index
   :current-counter current-counter
   :inc-counter inc-counter
   :dec-counter dec-counter
   :not-found not-found})
