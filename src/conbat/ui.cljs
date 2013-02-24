(ns conbat.ui
  (:require-macros [c2.util :refer [p pp bind!]])
  (:require [c2.core :refer [unify merge!]]
            [c2.scale :as scale]
            [c2.event :as event]
            ;[conbat.core :as c]
            [domina :as d]
            [domina.css :as css]
            [domina.events :as devent]
            [goog.Uri :as uri]
            [goog.net.XhrIo :as xhr]
            [clojure.set :as set]))

(def board-dimensions [100 100])

(defn neighbors [[[x y] player]]
  (for [dx [-1 0 1] dy (if (zero? dx) [-1 1] [-1 0 1])
        :let [x-pos (mod (+ x dx) (board-dimensions 0))
              y-pos (mod (+ y dy) (board-dimensions 1))]]
    [[x-pos y-pos] player]))
(def neighbors-memo (memoize neighbors))

(defn step [cells]
  (into {} (for [[loc nearby] (group-by first (mapcat neighbors-memo cells))
                 :let [names (set/map-invert
                               (frequencies
                                 (conj (map second nearby) (cells loc))))]
                 :when (and (or (contains? names 4)
                                (contains? names 3)
                                (contains? names 2))
                            (or (= (count nearby) 3)
                                (and (= (count nearby) 2)
                                     (cells loc))))]
             [loc (or (names 4) (names 3) (names 2))])))


(defn get-server-state [state]
  (goog.net.XhrIo/send "/state"
                       (fn [e]
                         (let [resp (-> e .-target .getResponseText clojure.string/trim)]
                           (if (empty? resp)
                             (d/log "Error: empty response from server.")
                             (let [r (cljs.reader/read-string resp)]
                               (d/log "Server state: " r)
                               (reset! state r)))))))

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
    (swap! state step))
  (reset! displayed-state @state)
  (js/setTimeout #(game-loop state displayed-state) 50))

(def state (atom #{}))
(def displayed-state (atom #{}))

(defn view [state]
  (for [x (range 0 (board-dimensions 0))]
    (for [y (range 0 (board-dimensions 1))]
      [[x y] (state [x y])])))

(defn ui [state]
  [:div.board
   (for [row (view @state)]
     [:div.row
      (unify row
             (fn [[[x y] player]]
               [:div.cell
                {:data [x y]
                 :style {:backgroundColor (if player :black :white)}} ""]))])
   [:div.clear]])

(def stamp (atom [[0 -1]
                  [1 0]
                  [-1 1]
                  [0 1]
                  [1 1]]))

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
; * have a display state that updates to the internal state once each game tick so we aren't
; redrawing the screen for every little update
; * add colors for different hosts
; * add leaderboard
; * add stamp builder
; * add name entry
; * improve performance

(defn init [data]
  (reset! ip (.-ip data))

  (bind! "#main"
         [:div#main
          [:h2 "Conbat"]
          (ui displayed-state)])

  (event/on "#main" ".cell" :click
            (fn [[pos _]]
              (let [points (translate-points pos @stamp)]
                (stamp-board! points)
                (update-server-state! points))))

  (game-loop state displayed-state)
  (update-state-loop state)
  )

