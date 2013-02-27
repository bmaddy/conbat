(ns conbat.node
  (:require [cljs.nodejs :as node]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [clojure.set :as set]
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

(def state (atom (zipmap (str->state init) (repeat :Server))))
(def players (atom {}))

(defn get-state [req res]
  (success res "text/plain")
  (.end res (pr-str {:state @state :players @players})))

(defn get-ip [req]
  ; FIXME we should really set a random cookie instead and determine people off of their ip
  (or (-> req .-headers (aget "x-forwarded-for"))
      (-> req .-connection .-remoteAddress)))

(defn update-state [req res]
  (let [data (atom "")]
    (.addListener req "data" (fn [d] (swap! data str d)))
    (.addListener req "end" (fn []
                              (println (str (get-ip req) ": " @data))
                              (let [updates (map vector
                                                 (reader/read-string @data)
                                                 (repeat (get-ip req)))]
                                (swap! state merge updates)
                                (.end res "true"))))))

(def colors #{:FireBrick :MediumVioletRed :OrangeRed :Gold :Indigo :Green :Turquoise :Blue :SlateGray})

(defn make-color [id]
  (let [unused (set/difference colors (map #(-> % second :color) @players))]
    (if (empty? unused)
      (str "#" (.toString (rand-int 16777216) 16))
      (rand-nth (seq unused)))))
  ;(rand-nth (seq (set/difference colors (map #(-> % second :color) @players)))))

(defn update-players [players state]
  (into {}
        (for [[id score] (frequencies (map second state))
              :let [player (players id {})]]
          [id {:score score
               :name (player :name id)
               :color (player :color (make-color id))}])))

(declare game-loop)
(defn game-loop [state]
  ;(println @state)
  (js/setTimeout
    #(do
       (swap! state c/step)
       (swap! players update-players @state)
       (println @players)
       (game-loop state))
    50))

(defn get-game [req res]
  (println (get-ip req))
  (success res "text/html")
  (.end res (str "<html>
            <head>
              <title>Conbat</title>
              <!-- <script src=\"/ui.js\" type=\"text/javascript\"></script> -->
              <link href=\"/main.css\" rel=\"stylesheet\" type=\"text/css\">
            </head>
            <body>
              <div>
                <div id=\"game-board\"> </div>
                <div class=\"game-info\">
                  <div id=\"stamp-board\"> </div>
                  <div id=\"leader-board\"> </div>
                </div>
                <script src=\"/ui.js\" type=\"text/javascript\"></script>
                <script type=\"text/javascript\">//<![CDATA[
                //goog.events.listen(window, goog.events.EventType.LOAD, function(){
                //document.ready = function(){
                  //conbat.repl.connect(window.location.hostname);
                  conbat.ui.init({ip: '" (get-ip req) "'});
                //};
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
    (.listen server 1337)
    (println "Server running at http://127.0.0.1:1337/")
    (game-loop state)))


(set! *main-cli-fn* start)

