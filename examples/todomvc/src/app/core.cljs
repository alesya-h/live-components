(ns app.core
  (:require [reagent.core :as r]
            [cljs.reader :as edn]
            [ajax.core :as ajax]
            [live-components.client.components :as lc]
            [live-components.client.core :as live]))

(defonce todos (r/atom (sorted-map)))

(defonce counter (r/atom 0))

(defn add-todo [text] (ajax/POST "/todos/new" {:format :raw :params {:title text}}))

(defn toggle [id] (ajax/POST (str "/todos/" id "/toggle")))
(defn save [id title] (ajax/PUT (str "/todos/" id) {:format :raw :params {:title title}}))
(defn delete [id] (ajax/DELETE (str "/todos/" id)))

(defn complete-all [v] (ajax/POST (str "/todos/complete-all") {:format :raw :params {:v (str v)}}))
(defn clear-done [] (ajax/POST (str "/todos/clear-done")))

#_(defonce init (do
                (add-todo "Rename Cloact to Reagent")
                (add-todo "Add undo demo")
                (add-todo "Make all rendering async")
                (add-todo "Allow any arguments to component functions")
                (complete-all true)))

(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (r/atom title)
        stop #(do (reset! val "")
                  (if on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
                (if-not (empty? v) (on-save v))
                (stop))]
    (fn [{:keys [id class placeholder]}]
      [:input {:type "text" :value @val
               :id id :class class :placeholder placeholder
               :on-blur save
               :on-change #(reset! val (-> % .-target .-value))
               :on-key-down #(case (.-which %)
                               13 (save)
                               27 (stop)
                               nil)}])))

(def todo-edit (with-meta todo-input
                 {:component-did-mount #(.focus (r/dom-node %))}))

(defn todo-stats [{:keys [filt active done]}]
  (let [props-for (fn [name]
                    {:class (if (= name @filt) "selected")
                     :on-click #(reset! filt name)})]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :done) "Completed"]]]
     (when (pos? done)
       [:button#clear-completed {:on-click clear-done}
        "Clear completed " done])]))

(defn todo-item [todo]
  (let [editing (r/atom false)]
    (fn [{:keys [id done title] :as todo}]
      (js/console.log (pr-str todo))
      [:li {:class (str (if done "completed ")
                        (if @editing "editing"))}
       [:div.view
        [:input.toggle {:type "checkbox" :checked done
                        :on-change #(toggle id)}]
        [:label {:on-double-click #(reset! editing true)} title]
        [:button.destroy {:on-click #(delete id)}]]
       (when @editing
         [todo-edit {:class "edit" :title title
                     :on-save #(save id %)
                     :on-stop #(reset! editing false)}])])))

(defonce filt (r/atom :all))

(defn todo-app [live-todos]
      (let [items (vals live-todos)
            done (->> items (filter :done) count)
            active (- (count items) done)]
        [:div
         [:section#todoapp
          [:header#header
           [:h1 "todos"]
           [todo-input {:id "new-todo"
                        :placeholder "What needs to be done?"
                        :on-save add-todo}]]
          (when (-> items count pos?)
            [:div
             [:section#main
              [:input#toggle-all {:type "checkbox" :checked (zero? active)
                                  :on-change #(complete-all (pos? active))}]
              [:label {:for "toggle-all"} "Mark all as complete"]
              [:ul#todo-list
               (for [todo (filter (case @filt
                                    :active (complement :done)
                                    :done :done
                                    :all identity) items)]
                 ^{:key (:id todo)} [todo-item todo])]]
             [:footer#footer
              [todo-stats {:active active :done done :filt filt}]]])]
         [:footer#info
          [:p "Double-click to edit a todo"]]]))

(defn empty-div []
  [:div])

(defn live-todo-app []
  [lc/live-component ["/todos"] todo-app empty-div empty-div])

(defn ^:export run []
  (r/render [live-todo-app]
            (js/document.getElementById "app")))

(defn transform-response [[uri {:keys [status body] :as response}]]
  [uri (update response :body edn/read-string)])

(live/enable! (str "ws://" js/document.location.host "/live") transform-response)
