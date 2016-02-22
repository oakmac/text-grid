(ns text-grid.core
  (:require
    [clojure.string :refer [split split-lines]]
    [text-grid.util :refer [by-id js-log log]]
    [rum.core :as rum]))

;;------------------------------------------------------------------------------
;; Constants
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

(log example-1-vec)

;;------------------------------------------------------------------------------
;; Page State Atom
;;------------------------------------------------------------------------------

(def initial-grid-state
  {:text example-1-vec})

(def grid-state (atom initial-grid-state))

;; NOTE: useful for debugging
; (add-watch grid-state :log atom-logger)

;;------------------------------------------------------------------------------
;; Components
;;------------------------------------------------------------------------------

(rum/defc Cell < rum/static
  [cell]
  [:div.cell (:ch cell)])

(rum/defc Line < rum/static
  [line]
  [:div.line
    (map Cell line)])

;;------------------------------------------------------------------------------
;; Top Level Component
;;------------------------------------------------------------------------------

(rum/defc Grid < rum/static
  [state]
  [:div.grid-container
    (map Line (:text state))])

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
