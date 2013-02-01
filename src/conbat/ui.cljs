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

(defn neighbors [[[x y] player]]
  (for [dx [-1 0 1] dy (if (zero? dx) [-1 1] [-1 0 1])]
    [[(+ dx x) (+ dy y)] player]))

(defn step [cells]
  (into {} (for [[loc nearby] (group-by first (mapcat neighbors cells))
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
                         (let [r (-> e .-target .getResponseText cljs.reader/read-string)]
                           (d/log "Server state: " r)
                           (reset! state r)))))

(def running (atom true))

(defn pause []
  (swap! running not))

(declare game-loop)
(defn game-loop [state displayed-state]
  (if @running
    (do
      (swap! state step)
      (reset! displayed-state @state)
      (js/setTimeout #(get-server-state state) 1)
      (js/setTimeout #(game-loop state displayed-state) 100))))

(def state (atom #{}))
(def displayed-state (atom #{}))

(defn view [state]
  (for [x (range -50 50)]
    (for [y (range -50 50)]
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

(defn init []
  (bind! "#main"
         [:div#main
          [:h2 "Conbat"]
          (ui displayed-state)])
  (game-loop state displayed-state)
  )

