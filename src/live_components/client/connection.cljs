(ns live-components.client.connection)

(defonce socket (atom nil))

(defn send-msg! [msg]
  ;; it is safe to ignore messages if socket is not connected,
  ;; because the only types of messages we have are subscribe/unsubscribe
  ;; and those will be resent in on-open callback
  (when (and @socket (= (.-readyState @socket) js/WebSocket.OPEN))
    (.send @socket (js/JSON.stringify (clj->js msg)))))

(defn remove-subscription! [url]
  (println "Removing server subscription for " url)
  (send-msg! [:unsubscribe url]))

(defn add-subscription! [url]
  (println "Adding server subscription for " url)
  (send-msg! [:subscribe url]))

(defn disconnect! []
  (when @socket
    (set! (.-onclose @socket) nil)
    (.close @socket)
    (reset! socket nil)))

(def default-reconnect-timeout 2000)
(def reconnect-timeout (atom default-reconnect-timeout))

(defn on-dead-connection-fn [reconnect]
  (fn []
    (println "Live connection is dead")
    (disconnect!)
    (js/setTimeout reconnect @reconnect-timeout)
    (println "Scheduled socket reconnection in " @reconnect-timeout)
    (swap! reconnect-timeout #(* 1.33 %))))

(defn on-connected-fn [on-connected]
  (fn []
    (reset! reconnect-timeout default-reconnect-timeout)
    (on-connected)))

(defn on-message-fn [on-msg transformation]
  (fn [msg]
    (let [message (-> msg
                      .-data
                      js/JSON.parse
                      (js->clj :keywordize-keys true)
                      transformation)]
      (on-msg message))))

(defn connect! [live-endpoint-url on-msg on-connected transform-response]
  "This starts a connection, which will automatically reconnect."
  (js/console.log live-endpoint-url)
  (when-not @socket
    (println "Connecting to live update socket")
    (let [sock (js/WebSocket. live-endpoint-url)]
      (set! (.-onopen sock) (on-connected-fn on-connected))
      (set! (.-onclose sock) (on-dead-connection-fn
                              #(connect! live-endpoint-url on-msg on-connected transform-response)))
      (set! (.-onerror sock) #(println "Live websocket error. Ensure next line is scheduling reconnection."))
      (set! (.-onmessage sock) (on-message-fn on-msg transform-response))
      (reset! socket sock))))
