(ns live-components.client.components
  (:require
   [reagent.core :as r]
   [live-components.client.subscriptions :as sub]))

(defn- subscribe-url-mapping [url-atom-map]
  (doseq [[url url-atom] url-atom-map]
    (sub/subscribe! url-atom url)))

(defn- unsubscribe-url-mapping [url-atom-map]
  (doseq [[url url-atom] url-atom-map]
    (sub/unsubscribe! url-atom)))

(defn- raw-live-component-base [urls target-component]
  (let [url-atoms (into {} (for [url urls] [url (r/atom nil)]))]
    (r/create-class
      {:component-did-mount (fn [] (js/setTimeout #(subscribe-url-mapping url-atoms) 0))
       :display-name (str "live:" (print-str urls))
       :component-will-unmount (fn [] (js/setTimeout #(unsubscribe-url-mapping url-atoms) 7500))
       :reagent-render (fn [urls target-component]
                         [target-component (->> urls (map url-atoms))])})))

(defn raw-live-component [urls target-component]
  ^{:key (reduce #(str %1 ":" %2) urls)} [raw-live-component-base urls target-component])

(defn live-component [urls normal-component loading-component unexpected-component]
  [raw-live-component urls
   (fn [result-atoms]
     (let [results (map deref result-atoms)]
       (cond
         (some nil? results) [loading-component]
         (every? #(= 200 (:status %)) results) (apply vector normal-component (map :body results))
         :else [unexpected-component results])))])
