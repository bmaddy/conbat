(ns conbat.node
  (:require [cljs.nodejs :as node]
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

(defn str->state [str]
  (for [[line y] (map vector (str/split-lines (str/trim init)) (range))
        [chr x] (map vector (clojure.string/trim line) (range))
        :when (= chr \*)]
    [x y]))

(def state (atom {[1 0] :a
                  [1 1] :b
                  [1 2] :a}))

(def state (atom (zipmap (str->state init) (repeat :a))))

(defn get-state [req res]
  (success res "text/plain")
  (.end res (pr-str @state)))

(defn get-game [req res]
  (success res "text/html")
  (.end res "<html>
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
                  conbat.ui.init();});
              //]]></script>
              </div>
            </body>
            </html>"))

(defn get-file [type file req res]
  (success res type)
  (.end res (.readFileSync fs file)))

(def routes
  {"/" get-game
   "/state" get-state
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

