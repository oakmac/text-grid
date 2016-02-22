(ns text-grid.core
  (:require
    [clojure.string :refer [split split-lines]]
    [text-grid.util :refer [by-id js-log log]]
    [rum.core :as rum]))

;;------------------------------------------------------------------------------
;; Constants
;;------------------------------------------------------------------------------

(def cursor-blink-rate-ms 500)

;;------------------------------------------------------------------------------
;; Example Text
;;------------------------------------------------------------------------------

(def example-text-1
  "
  (function (root, factory) {
    if (typeof define === 'function' && define.amd) {
      define([], factory);
    }
    else if (typeof module === 'object' && module.exports) {
      module.exports = factory();
    }
    else {
      root.parinfer = factory();
    }
  }(this, function() { // start module anonymous scope
  'use strict';
  ")

;;------------------------------------------------------------------------------
;; Misc
;;------------------------------------------------------------------------------

(defn- chars->hashmaps [list-of-strings]
  (vec (map (fn [s] {:ch s}) list-of-strings)))

(defn- split-line [line-txt]
  (split line-txt ""))

(defn- text->vec [txt]
  (->> txt
       split-lines
       (map split-line)
       (map chars->hashmaps)
       vec))

(def example-1-vec (text->vec example-text-1))

;;------------------------------------------------------------------------------
;; Page State Atom
;;------------------------------------------------------------------------------

(def initial-grid-state
  {:cursors [[3 8]
             [5 12]]
   ;; NOTE: this property is toggled every cursor-blink-rate-ms
   :cursor-showing? true
   :text example-1-vec})

(def grid-state (atom initial-grid-state))

;; NOTE: useful for debugging
; (add-watch grid-state :log atom-logger)

;;------------------------------------------------------------------------------
;; Cursor Blink
;;------------------------------------------------------------------------------

(defn- toggle-cursor-blink! []
  (swap! grid-state update-in [:cursor-showing?] not))

(js/setInterval toggle-cursor-blink! cursor-blink-rate-ms)

;;------------------------------------------------------------------------------
;; Components
;;------------------------------------------------------------------------------

;; Cell options:
;; - cursor?
;; - highlighted?
;; - underlined?
;; - bold?
;; - italic?
;; - color
;; - font (need to think about this one)
(rum/defc Cell < rum/static
  [{:keys [ch cursor?]}]
  [:div {:class "cell"}
    (when cursor? [:div.cursor])
    [:div.text ch]])

;; Line options:
;; - cursor-line? (NOTE: not the same thing as "highlighted")
(rum/defc Line < rum/static
  [line]
  [:div.line
    (map Cell line)])

;;------------------------------------------------------------------------------
;; Top Level Component
;;------------------------------------------------------------------------------

(defn- inject-cursor-information
  "Takes the cursor information from :cursors and injects it into the :text
   two-dimensional vector."
  [state]
  (reduce (fn [state cursor]
            (if (:cursor-showing? state)
              (assoc-in state [:text (first cursor) (second cursor) :cursor?] true)
              state))
          state
          (:cursors state)))

(rum/defc Grid < rum/static
  [state]
  (let [state2 (inject-cursor-information state)]
    [:div.grid-container
      (map Line (:text state2))]))

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
