(ns conbat.node
  (:require [cljs.nodejs :as node]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [conbat.core :as c]))

(def http
  (node/require "http"))

(def fs (node/require "fs"))

(defn success [res content-type]
  (.writeHead res 200 (js-obj "Content-Type" content-type))
  res)

(def init "
  .........
  ..*......
  ....*....
  .**..***.")

(defn str->state [s]
  (for [[line y] (map vector (str/split-lines (str/trim s)) (range))
        [chr x] (map vector (clojure.string/trim line) (range))
        :when (= chr \*)]
    [x y]))

(def state (atom (zipmap (str->state init) (repeat :a))))

(defn get-state [req res]
  (success res "text/plain")
  (.end res (pr-str @state)))

(defn get-ip [req]
  ; not handling proxies yet:
  ; http://stackoverflow.com/questions/4255264/getting-client-hostname-in-node-js
  (-> req .-connection .-remoteAddress))

(defn update-state [req res]
  (let [data (atom "")]
    (.addListener req "data" (fn [d] (swap! data str d)))
    (.addListener req "end" (fn []
                              (println (get-ip req) ": " @data)
                              (let [updates (map vector
                                                 (reader/read-string @data)
                                                 (repeat (get-ip req)))]
                                (swap! state merge updates)
                                (.end res "true"))))))

(defn get-game [req res]
  (println (get-ip req))
  (success res "text/html")
  (.end res (str "<html>
            <head>
              <title>Conbat</title>
              <script src=\"/ui.js\" type=\"text/javascript\"></script>
              <link href=\"/main.css\" rel=\"stylesheet\" type=\"text/css\">
            </head>
            <body>
              <div>
                <div id=\"main\"> </div>
                <script type=\"text/javascript\">//<![CDATA[
                goog.events.listen(window, goog.events.EventType.LOAD, function(){
                  conbat.repl.connect(window.location.hostname);
                  conbat.ui.init({ip: '" (get-ip req) "'});});
              //]]></script>
              </div>
            </body>
            </html>")))

(defn get-file [type file req res]
  (success res type)
  (.end res (.readFileSync fs file)))

(def routes
  {"/" get-game
   "/state" get-state
   "/update" update-state
   "/ui.js" (partial get-file "text/javascript" "ui.js")
   "/main.css" (partial get-file "text/css" "main.css")})

(defn handler [req res]
  (println "Request: " (.-url req))
  (if-let [handler (routes (.-url req) nil)]
    (handler req res)
    (do
      (.writeHead res 404)
      (.end res "Not Found\n"))))

(defn start [& _]
  (let [server (.createServer http handler)]
    (.listen server 1337 "127.0.0.1")
    (println "Server running at http://127.0.0.1:1337/")
    (c/game-loop state)))


(set! *main-cli-fn* start)

