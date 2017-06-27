(ns counter.client.live
  (:require
   [counter.common.routes :as routes]
   [reagent.core :as r]
   [live-components.client.subscriptions :as sub]
   [live-components.client.components :as components]
   [live-components.client.core :as live]))

(defn debug [x]
  (js/console.log "Live:" (pr-str x))
  x)

(defn transform-body [body]
  (-> body js/JSON.parse (js->clj :keywordize-keys true)))

(defn transform-response [[uri {:keys [status body] :as response}]]
  (-> [uri (update response :body transform-body)]
      debug))

(defn force-reconnect! [] (live/force-reconnect!))

(defn subscribe! [atom handler options]
  (sub/subscribe! atom (routes/GET-url-for handler options)))

(defn unsubscribe! [atom] (sub/unsubscribe! atom))

(defn default-loading-component []
  [:div])

(defn default-unexpected-component [responses]
  (let [bad-responses (filter #(not (#{102 200} (:status %))) responses)]
    [:div "Error loading data: " (pr-str bad-responses)]))

(defn live-component [url-specs normal-component
                      & [{:keys [loading-component unexpected-component]
                          :or {loading-component default-loading-component
                               unexpected-component default-unexpected-component}}]]
  (components/live-component
   (map #(apply routes/GET-url-for %) url-specs)
   normal-component
   loading-component
   unexpected-component))

(live/enable! (str "ws://" js/document.location.host "/live") transform-response)
