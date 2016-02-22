(ns text-grid.core
  (:require
    [text-grid.util :refer [by-id js-log]]
    [rum.core :as rum]))

;;------------------------------------------------------------------------------
;; Constants
;;------------------------------------------------------------------------------



;;------------------------------------------------------------------------------
;; Page State Atom
;;------------------------------------------------------------------------------

(def initial-grid-state
  {})

(def grid-state (atom initial-grid-state))

;; NOTE: useful for debugging
; (add-watch grid-state :log atom-logger)

;;------------------------------------------------------------------------------
;; Top Level Component
;;------------------------------------------------------------------------------

(rum/defc Grid < rum/static
  [state]
  [:div "TODO: text grid"])

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def container-el (by-id "gridContainer"))

(defn- on-change-grid-state
  "Render the page on every state change."
  [_kwd _the-atom _old-state new-state]
  (rum/request-render
    (rum/mount (Grid new-state) container-el)))

(add-watch grid-state :main on-change-grid-state)

;;------------------------------------------------------------------------------
;; Page Init
;;------------------------------------------------------------------------------

;; trigger an initial render
(swap! grid-state identity)
