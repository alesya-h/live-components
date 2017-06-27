(ns todomvc.server
  (:require
   [compojure.core :as c]
   [compojure.route :as route]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [ring.middleware.resource :as rm-resource]
   [ring.middleware.params :as p]
   [ring.middleware.keyword-params :as kp]
   [immutant.web :as immutant]
   [live-components.server.core :as live]
   ))

(defonce todos (atom (sorted-map)))

(defonce counter (atom 0))

(defn add-todo [text]
  (let [id (swap! counter inc)]
    (swap! todos assoc id {:id id :title text :done false})))

(defn toggle [id] (swap! todos update-in [id :done] not))
(defn save [id title] (swap! todos assoc-in [id :title] title))
(defn delete [id] (swap! todos dissoc id))

(defn mmap [m f a] (->> m (f a) (into (empty m))))
(defn complete-all [v] (swap! todos mmap map #(assoc-in % [1 :done] v)))
(defn clear-done [] (swap! todos mmap remove #(get-in % [1 :done])))

(defn api-call [f & args]
  (let [result (apply f args)]
    (live/mark-updated "/todos")
    (pr-str result)))

(c/defroutes app-routes
  (c/GET "/" [] (slurp (io/resource "index.html")))
  (c/GET "/todos" [] (pr-str @todos))
  (c/POST "/todos/new" {{:keys [title]} :params} (api-call add-todo title))
  (c/POST "/todos/:id/toggle" [id] (api-call toggle (Integer/parseInt id)))
  (c/PUT "/todos/:id" {{:keys [id title]} :params} (api-call save (Integer/parseInt id) title))
  (c/DELETE "/todos/:id" [id] (api-call delete (Integer/parseInt id)))
  (c/POST "/todos/complete-all" {{:keys [v]} :params} (api-call complete-all (= v "true")))
  (c/POST "/todos/clear-done" [] (api-call clear-done))
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      (rm-resource/wrap-resource ".")
      (kp/wrap-keyword-params)
      (p/wrap-params)
      (live/wrap-live "/live")
      ))

(defonce server (atom nil))
(when @server (immutant/stop @server))
(reset! server (immutant/run #'app {:host "0.0.0.0" :port 3000}))
