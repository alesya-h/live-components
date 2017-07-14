(ns app.server.server
  (:require
   [bidi.ring :as bidi-ring]
   [app.common.routes :as routes]
   [app.server.handlers :as handlers]
   [live-components.server.core :as live]

   [ring.middleware.resource :as rm-resource]
   [immutant.web :as immutant]
   ))

(def app
  (-> (bidi-ring/make-handler routes/routes handlers/all-handlers)
      (rm-resource/wrap-resource ".")
      (live/wrap-live "/live")))

(defonce server (atom nil))

(defn start []
  (reset! server (immutant/run #'app {:host "0.0.0.0" :port 3000})))

(defn stop []
  (when-let [s @server]
    (immutant/stop @server)
    (reset! server nil)))

(defn restart [] (stop) (start))

(restart)
