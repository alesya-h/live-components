(ns live-components.client.core
  (:require
   [live-components.client.connection :as conn]
   [live-components.client.subscriptions :as sub]))

(def config (atom {}))

(defn connect! []
  (let [{:keys [live-endpoint-url transform-response]} @config]
    (conn/connect! live-endpoint-url
                   sub/on-msg
                   sub/on-connected
                   transform-response)))

(defn disconnect! []
  (conn/disconnect!))

(defn force-reconnect! []
  (conn/disconnect!)
  (connect!))

(defn enable! [live-endpoint-url transform-response]
  (reset! config {:live-endpoint-url live-endpoint-url
                  :transform-response transform-response})
  (connect!))
