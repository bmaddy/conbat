(ns conbat.repl
  (:require
    [clojure.browser.repl :as repl]))

(defn ^:export connect [hostname]
  ; for some reason this isn't working...
  (repl/connect (str "http://" hostname ":9000/repl")))

