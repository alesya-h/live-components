(ns live-components.client.subscriptions
  (:require
   [live-components.client.connection :as connection]
   [datascript.core :as d]))

(defonce schema {:url {:db/cardinality :db.cardinality/one}
                 :atom {:db/cardinality :db.cardinality/one}})

(defonce db (d/create-conn schema))

(defn atoms-for-url [url]
  (d/q '[:find [?atom ...] :in $ ?url
         :where [?e :url ?url]
                [?e :atom ?atom]]
       @db url))

(defn url-for-atom [the-atom]
  (d/q '[:find ?url . :in $ ?atom
         :where [?e :atom ?atom]
                [?e :url ?url]]
       @db the-atom))

(defn all-urls-with-atoms []
  (let [urls (d/q '[:find [?url ...] :where [?e :url ?url]] @db)]
    (->> urls
         (map (fn [url] [url (atoms-for-url url)]))
         (into {}))))

(defn subscribe! [the-atom url]
  "Subscribes an atom to server data at specified url.
If multiple atoms request the same url, only one server
subscription will be made and delivered to all
corresponding atoms."
  (if-let [existing-url (url-for-atom the-atom)]
    (println "Atom already has subscription. Current subscription url is " existing-url)
    (do
      (println "Adding new subscription record for " url)
      (if-let [another-atom (first (atoms-for-url url))]
        ;; if we already subscribed to this url then
        ;; we don't need to duplicate server
        ;; subscription request, but do need to copy latest
        ;; value, as value from server will arrive only
        ;; when data is changed
        (reset! the-atom @another-atom)
        (connection/add-subscription! url))
      (d/transact! db [ {:db/id -1 :url url :atom the-atom} ]))))

(defn unsubscribe! [the-atom]
  "Unsubscribes atom from server data and resets it to nil.
When last atom is unsubscribed from particular url, the
subscription for this url is cancelled"
  (if-let [[atom-subscription url] (d/q '[:find [?e ?url] :in $ ?atom
                                          :where [?e :atom ?atom]
                                                 [?e :url ?url]]
                                        @db the-atom)]
    (do
      (println "Removing subscription record for " url)
      (d/transact! db [[:db.fn/retractEntity atom-subscription]])
      (reset! the-atom nil)
      (if (empty? (atoms-for-url url))
        (connection/remove-subscription! url)))
    (println "Atom does not have live subscription")))

(defn set-subscription! [the-atom url]
  (if (url-for-atom the-atom)
    (unsubscribe! the-atom))
  (subscribe! the-atom url))

(defn plural [n singular plural]
  (str n (if (= n 1) singular plural)))

(defn on-msg [[url value]]
  (println "Received update for " url)
  (let [atoms (atoms-for-url url)]
    (println "Updating " (plural (count atoms) " atom." " atoms."))
    (if (empty? atoms)
      (do
        (println "Received update for non-subscribed url " url " with value " value)
        (connection/remove-subscription! url))
      (doseq [the-atom atoms]
        (reset! the-atom value)))))

(defn on-connected []
  (println "Live update connection established")
  (let [urls (d/q '[:find [?url ...] :where [?e :url ?url]] @db)]
    (doseq [url urls]
      (connection/add-subscription! url))))
