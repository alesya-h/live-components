(ns counter.client.client
  (:require
   [reagent.core :as r]
   [ajax.core :as ajax]
   [counter.common.routes :as routes]
   [counter.client.live :as live]))

(defn make-ajax-handler [on-success on-fail]
  (fn [[ok response]]
    ((if ok
       (or on-success (fn [_] nil))
       (or on-fail (fn [resp] (js/console.error response) nil)))
     response)))

(def format-options
  {:format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})})

(defn api-call [handler params & [on-success on-fail]]
  "This is for most api calls (except ones which require form data, like chunk uploading)"
  (let [[method url non-route-params] (routes/url-with-method-and-non-route-params-for handler params)]
    (ajax/ajax-request
     (-> {:uri url :method method :params non-route-params
          :handler (make-ajax-handler on-success on-fail)}
         (merge format-options)))))

(defn counter-component [counter]
  [:section.section.is-medium>div.container>nav.level>div.level-left
    [:div.level-item>div.title.is-4>strong (str "Counter " counter)]
    [:div.level-item [:a.button.is-primary {:href "#" :on-click #(api-call :inc-counter {})} "Inc"]]
    [:div.level-item [:a.button.is-primary {:href "#" :on-click #(api-call :dec-counter {})} "Dec"]]
    ])

(defn main-component []
  [:div
    [live/live-component [[:current-counter]] counter-component]])

(defn render-app []
  (r/render-component
   [main-component]
   (js/document.getElementById "application")))

(defn init []
  (js/document.addEventListener "DOMContentLoaded" render-app))

(defn on-reload []
  (render-app))
