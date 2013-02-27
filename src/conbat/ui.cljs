(ns conbat.ui
  (:require-macros [c2.util :refer [p pp bind!]])
  (:require [c2.core :refer [unify merge!]]
            [c2.scale :as scale]
            [c2.event :as event]
            [conbat.core :as c]
            [domina :as d]
            [domina.css :as css]
            [domina.events :as devent]
            [goog.Uri :as uri]
            [goog.net.XhrIo :as xhr]
            [clojure.set :as set]))

(def state (atom #{}))
(def players (atom {}))
(def displayed-state (atom #{}))

(def stamp-dimensions [[-3 4] [-3 4]])
(def stamp (atom #{[0 -1]
                   [1 0]
                   [-1 1]
                   [0 1]
                   [1 1]}))

(defn get-server-state [state]
  (goog.net.XhrIo/send "/state"
                       (fn [e]
                         (let [resp (-> e .-target .getResponseText clojure.string/trim)]
                           (if (empty? resp)
                             (d/log "Error: empty response from server.")
                             (let [r (cljs.reader/read-string resp)]
                               (d/log "Server state: " r)
                               (reset! state (r :state))
                               (reset! players (r :players))))))))

(defn update-server-state! [points]
  (goog.net.XhrIo/send "/update"
                       (fn [e]
                         (d/log e)
                         (d/log (-> e .-target))
                         (d/log (-> e .-target .getResponseText)))
                       "POST"
                       (pr-str points)))

(def running (atom true))

(defn ^:export pause []
  (swap! running not))

(declare update-state-loop)
(defn update-state-loop [state]
  (if @running
    (do
      (d/log "getting server state")
      (get-server-state state)))
  (js/setTimeout #(update-state-loop state) 1000))

(declare game-loop)
(defn game-loop [state displayed-state]
  (if @running
    (swap! state c/step))
  (reset! displayed-state @state)
  (js/setTimeout #(game-loop state displayed-state) 50))

(defn make-board-data [[xs ys] f]
  (for [x xs]
    (for [y ys]
      [[x y] (f [x y])])))

(defn get-colors [state]
  (make-board-data [(range 0 (first c/board-dimensions))
                    (range 0 (second c/board-dimensions))]
                   (fn [pos]
                     (let [player (state pos)]
                       (if player
                         (-> @players player :color)
                         :white)))))

(defn get-stamp-colors [stamp]
  (make-board-data (map #(apply range %) stamp-dimensions)
                   (fn [pos]
                     (if (stamp pos)
                       :black
                       :white))))

(defn make-board [colors _]
  [:div.board
   (for [row colors]
     [:div.row
      (unify row
             (fn [[[x y] color]]
               [:div.cell
                {:data [x y]
                 :style {:backgroundColor color}} ""]))])
   [:div.clear]])

; The svg version doesn't seem to be faster
(defn make-svg-board [colors size]
  [:svg {:width (* size (count (first colors))) :height (* size (count colors))}
   (unify (apply concat colors)
          (fn [[[x y] color]]
            [:rect.cell {:data (str [x y])
                         :x (* size x) :y (* size y)
                         :width size :height size
                         :fill color}]))
   [:div.clear]])

(defn translate-points [move points]
  (map (partial map +) (repeat move) points))

(def ip (atom nil))

(defn stamp-board! [points]
  (d/log @ip)
  (let [named-points (map vector points (repeat @ip))]
    (swap! state merge named-points)
    ; update the displayed state immediately so we see our change right away
    (swap! displayed-state merge named-points)))

; TODO
; * improve performance
; * add name entry

(defn ^:export init [data]
  (reset! ip (.-ip data))

  (bind! "#game-board"
         [:div#game-board
          [:h2 "Conbat"]
          (make-board (get-colors @displayed-state) 5)])

  (bind! "#stamp-board"
         [:div#stamp-board
          [:h3 "Stamp configuration"]
          (make-board (get-stamp-colors @stamp) 20)
          [:div.clear]])

  (bind! "#leader-board"
         [:div#leader-board
          [:h3 "Leader board"]
          [:ol
           (unify (reverse (sort-by :score (vals @players)))
                  (fn [{:keys [name score color]}]
                    [:li
                     [:span
                      [:div.name name]
                      [:div.score score]]]))]])
           
  (event/on "#game-board" ".cell" :click
            (fn [[pos _]]
              (let [points (translate-points pos @stamp)]
                (stamp-board! points)
                (update-server-state! points))))

  (event/on "#stamp-board" ".cell" :click
            (fn [[pos _]]
              (let [op (if (@stamp pos) disj conj)]
                (swap! stamp op pos))))

  (game-loop state displayed-state)
  (update-state-loop state)
  )

