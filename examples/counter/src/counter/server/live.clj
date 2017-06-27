(ns counter.server.live
  (:require
   [live-components.server.core :as live]
   [counter.common.routes :as routes]))

(defn mark-handler-updated [handler args]
  (live/mark-updated (routes/GET-url-for handler args)))
