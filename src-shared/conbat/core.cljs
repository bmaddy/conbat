(ns conbat.core
  (:require [clojure.set :as set]))

(def board-dimensions [100 100])

(defn neighbors [x y]
  (for [dx [-1 0 1] dy (if (zero? dx) [-1 1] [-1 0 1])
        :let [x-pos (mod (+ x dx) (board-dimensions 0))
              y-pos (mod (+ y dy) (board-dimensions 1))]]
    [x-pos y-pos]))
(def neighbors-memo (memoize neighbors))

(defn neighbors-with-player [[pos player]]
  (map #(vector % player) (apply neighbors pos)))

(defn step [cells]
  (into {} (for [[loc nearby] (group-by first (mapcat neighbors-with-player cells))
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

