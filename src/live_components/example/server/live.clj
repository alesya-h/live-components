(ns live-components.example.server.live
  (:require
   [live-components.server.core :as live]
   [live-components.example.common.routes :as routes]))

(defn mark-handler-updated [handler args]
  (live/mark-updated (routes/GET-url-for handler args)))
