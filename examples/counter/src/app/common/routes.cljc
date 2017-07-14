(ns app.common.routes
  (:require
   [bidi.bidi :as bidi]
   [clojure.string :as str]
   [cemerick.url :as url]))

(def routes ["/" [["" {:get :index}]
                  ["current-counter" {:get :current-counter}]
                  ["inc-counter" {:post :inc-counter}]
                  ["dec-counter" {:post :dec-counter}]
                  [#".*" :not-found]]])






(def path-for (partial bidi/path-for routes))
(def match-route-without-query (partial bidi/match-route routes))

(defn match-route [method url]
  (let [[path query] (str/split url #"\?")]
    (when-let [matched-route (match-route-without-query path :request-method method)]
      (merge matched-route
             {:query-params (reduce-kv #(assoc %1 (keyword %2) %3) {} (url/query->map query))}))))

(defn request-method [handler url]
  (->> [:get :post :put :delete]
       (filter #(= handler
                   (:handler (match-route % url))))
       first))

(defn to-query-string [params]
  (if (empty? params)
    ""
    (str "?" (url/map->query params))))

(defn with-query-params [url params]
  (str url (to-query-string params)))

(defn url-with-method-and-non-route-params-for
  ([handler] (url-with-method-and-non-route-params-for handler {}))
  ([handler params]
   (let [url (->> (mapcat identity params) ; {:a 1 :b 2} => [:a 1 :b 2]
                  (apply path-for handler))
         method (request-method handler url)
         matched-route (match-route method url)
         {:keys [route-params]} matched-route
         non-route-params (apply dissoc params (keys route-params))]
     [method url non-route-params])))

;; DELETE!
(defn url-with-method-for
  ([handler] (url-with-method-for handler {}))
  ([handler params]
   (let [[method url non-route-params] (url-with-method-and-non-route-params-for handler params)]
     [method (with-query-params url non-route-params)])))

(defn GET-url-for
  ([handler] (GET-url-for handler {}))
  ([handler params]
   (let [[method url non-route-params] (url-with-method-and-non-route-params-for handler params)]
     (if (= method :get)
       (with-query-params url non-route-params)
       (do
         (println "Unable to form url for handler " handler " with params " params)
         nil)))))
