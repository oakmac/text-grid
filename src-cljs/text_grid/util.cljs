(ns text-grid.util
  "Utility functions.")

;;------------------------------------------------------------------------------
;; Logging
;;------------------------------------------------------------------------------

(defn js-log
  "Log a JavaScript thing."
  [js-thing]
  (js/console.log js-thing))

(defn log
  "Log a Clojure thing."
  [clj-thing]
  (js-log (pr-str clj-thing)))

(defn atom-logger
  [_kwd _the-atom old-state new-state]
  (log old-state)
  (log new-state)
  (js-log "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"))

;;------------------------------------------------------------------------------
;; DOM Wrappers
;;------------------------------------------------------------------------------

(defn by-id [id]
  (js/document.getElementById id))
