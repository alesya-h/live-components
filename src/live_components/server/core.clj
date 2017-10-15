(ns live-components.server.core
  (:require
   [immutant.web.async :as async]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [datascript.core :as d]))

(defonce schema {:client {:db/cardinality :db.cardinality/one}
                 :url {:db/cardinality :db.cardinality/one}
                 :token {:db/cardinality :db.cardinality/one}
                 :previous-value {:db/cardinality :db.cardinality/one}})

(defonce db (d/create-conn schema))

(defonce base-handler (atom {}))

(defn subscriptions-to-update [token]
  (d/q '[:find [(pull ?e [:client :url]) ...] :in $ ?token
         :where [?e :token ?token]
                [?e :client ?client]
                [?e :url ?url]]
       @db token))

(defn send-data [client data]
  (let [data-str (json/write-str data)]
    (async/send! client data-str)))

(defn send-update [client url]
  (println "About to send update for " url)
  (try
    (let [request (async/originating-request client)
          _ (println "Sending update for " url)
          result (try (@base-handler {:request-method :get
                                      :uri url
                                      :headers (:headers request)})
                      (catch Exception e
                        (println "ERROR: base handler crashed" (str e))
                        {:status 500
                         :body (str e)}))]
      (send-data client [url result]))
    (catch Exception e
      (println "ERROR: Problem sending live update: " e))))

(defn mark-updated [token]
  (println "Marking " token " for update")
  (let [subscriptions (subscriptions-to-update token)]
    (doseq [{:keys [client url]} subscriptions]
      (send-update client url))))

(defn token [url]
  "This strips off query string of url. Urls
without query strings are used to detect possible
updates, but full subscription urls (with queries)
are used to reevaluate requests and send updated
data"
  (str/replace-first url #"\?.*$" ""))

(defn add-subscription [client url]
  (d/transact! db [ {:db/id -1
                     :client client
                     :url url
                     :token (token url)} ])
  (send-update client url))

(defn remove-subscription [client url]
  (let [client-subscriptions (d/q '[:find [?e ...] :in $ ?client ?url
                                    :where [?e :client ?client]
                                           [?e :url ?url]]
                                  @db client url)
        transactions (mapv #(vector :db.fn/retractEntity %)
                           client-subscriptions)]
    (d/transact! db transactions)))

(defn remove-all-subscriptions [client]
  (let [client-subscriptions (d/q '[:find [?e ...] :in $ ?client
                                    :where [?e :client ?client]]
                                  @db client)
        transactions (mapv #(vector :db.fn/retractEntity %)
                           client-subscriptions)]
    (d/transact! db transactions)))

(defn on-message [client [action url]]
  (println "Live " action " " url)
  (case action
    "subscribe" (add-subscription client url)
    "unsubscribe" (remove-subscription client url)))

(def websocket-callbacks
  {:on-open (fn [client]
              (println "Live client connected"))
   :on-close (fn [client {:keys [code reason]}]
               (println "Live client disconnected with code " code)
               (remove-all-subscriptions client))
   :on-message (fn [client msg]
                 (on-message client (json/read-str msg)))})

(defn live-ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defn wrap-live [next-handler live-endpoint-url]
  (reset! base-handler next-handler)
  (fn [request]
    (if (= (:uri request) live-endpoint-url)
      (live-ws-handler request)
      (next-handler request))))
