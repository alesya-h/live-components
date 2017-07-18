# Live Components

Don't duplicate server state on the client and get realtime collaboration for free.

## What is this about?

You have your backend with REST API. You have your React-based frontend. Usually you use ajax calls and keep out-of-date independent incomplete copy of your server state on the client. When you make ajax calls, you update your local copy of the state. With live-components you throw away manual handling of server state on the client. You have one component that takes a list of subscription urls, and component, which will be passed data from that urls and rendered. On the server whenever you think some urls may have new data, you mark them as updated and new data is automatically pushed to all connected clients. Magic!

## I want to see it!

Live example is deployed to https://hypercards.net . Open it in two windows and see how adding/changing todos in one window adds/changes them in both. Sources are under `examples/todomvc`.

## How to use it?

(at the bottom there are links to 3 commits that augment reagent's todomvc with live-components)

1. Add library to your project: `[live-components "1.0.1"]` or see https://clojars.org/live-components

2. Wrap your ring handler with live:

```clojure
(ns app.server
  (:require [live-components.server.core :as live-server]))

(live-server/wrap-live app "/live")
```

`"/live"` is a url under which live endpoint will become available. You'll use it on the client to connect to live (see below).

You have to use immutant web server, as live server middleware assumes immutant's websocket api. Unfortunately ring doesn't have standard websocket api. See 

3. Configure and enable live on the client:

```clojure
(ns app.client
  (:require [live-components.client.core :as live-client]))

(defn transform-response [[uri ring-response]]
  [uri (update ring-response
               :body #(-> (js/JSON.parse %)
                          (js->clj {:keywordize-keys true})))])

(live-client/enable! (str (clojure.string/replace (.-protocol js/window.location) "http" "ws")
                          "//"
                          js/document.location.host
                          "/live")
                     transform-response)
```

`transform-response` defines how your data is deserialized. If you'll use `identity`, you'll get your api response body left as a string. If your api returns json, you most likely want to use `js/JSON.parse` and `js->clj` as in the example above. If you use EDN, use `cljs.reader/read-string`.


4. Use live components:

```clojure
(ns app.client.posts-page
  (:require [reagent.core :as r]
            [live-components.client.components :as lc]))

(defn post [{:keys [title body]}]
  [:div.post
    [:h2 title]
    [:div.post-text body]])

(defn loading-component []
  [:div])

(defn unexpected-component [response]
  [:div.error (pr-str response)])

(defn live-post [post-id]
  [lc/live-component [(str "/posts/" id)] post loading-component unexpected-component])

(defn root-component []
  [:div [:h1 "My Blog"]
        [live-post 1]])

(r/render [root-component] (js/document.getElementById "app"))
```

`loading-component` takes no arguments and will be rendered until data is received.

`unexpected-component` takes one argument which is a vector of responses. It is rendered if any api call responded with any http status other than 200.

5. Tell us when your data have been updated:

```clojure
(ns app.server.posts
  (:require [live-components.server.core :as live]))

(defn update-post [post-id {:keys [title body]}]
  (when title (exec-sql "UPDATE posts SET title = % WHERE id = %" title post-id))
  (when body  (exec-sql "UPDATE posts SET body  = % WHERE id = %" body  post-id))
  (when (or title body)
    (live/mark-updated (str "/posts/" post-id))
    (live/mark-updated "/posts")))
```

6. DONE



First argument to [lc/live-component] is a vector of urls, and each result will be passed as an argument to your rendering component, so you are not limited to just one url per component. That means you can have

`(defn post [post user] ...)` and use it like `[lc/live-component ["/posts/1" "/users/1"] post loading-component unexpected-component]`

Most likely you want to write a thin wrapper around live-component to intelligently generate urls, and to use same loading and unexpected components across your project. See https://github.com/alesguzik/live-components/tree/master/examples/counter as an example with bidi routing and wrappers.

See https://github.com/alesguzik/live-components/tree/master/examples/todomvc for reagent's todomvc, augmented with live-components:

1. Adding REST API for todos: https://github.com/alesguzik/live-components/commit/dd994a1bb81b5806b56501f5d3204f9fe65d5c6f . At this point you have just simple rest api for managing todos that you can call using `curl`.
2. Adding live to the client and server, and rendering todos from server: https://github.com/alesguzik/live-components/commit/b9fad3bd14d9d512e4818729ae894383c19d5364 . At this point your server todos are rendered live in a browser. Whenever you make rest api call (e.g. via curl) to modify todos, the changes are automatically propagated to all clients.
3. Using REST API to modify todos: https://github.com/alesguzik/live-components/commit/72093db340e47df58be9f60661e5d433f528131c . At this point we have fully-collaborative todomvc.

# Thanks

This project have been born as part of continuous effort to build the best social file sharing platform http://ourmedian.com .
