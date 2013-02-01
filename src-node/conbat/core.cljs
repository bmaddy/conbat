(ns conbat.core
  (:require [clojure.set :as set]))


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

(declare game-loop)
(defn game-loop [state]
  (println @state)
  (js/setTimeout
    #(do
       (swap! state step)
       (game-loop state))
    100))

