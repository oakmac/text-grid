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

(defn- text->cells [s]
  (chars->hashmaps (split s "")))

(def eol-cell
  {:eol? true})

(defn- ensure-eol
  "Make sure a line contains an EOL cell."
  [line]
  (if-not (:eol? (peek line))
    (conj line eol-cell)
    line))

(defn- text->vec [txt]
  (->> txt
       split-lines
       (map split-line)
       (map chars->hashmaps)
       (map ensure-eol)
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

(def js-cursor-blink-interval nil)

;; TODO: this won't work; need to always show the cursor on a movement
(defn- toggle-cursor-blink! []
  (swap! grid-state update-in [:cursor-showing?] not))

(set! js-cursor-blink-interval (js/setInterval toggle-cursor-blink! cursor-blink-rate-ms))

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
    (when ch [:div.text ch])])

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









;;------------------------------------------------------------------------------
;; Cursor Movements
;;------------------------------------------------------------------------------

(defn- move-cursor-left
  "Move a cursor one space to the left."
  [text [row col]]
  (let [on-top-line? (zero? row)
        at-beginning-of-line? (zero? col)]
    (cond
      ;; we are at the top left; do nothing
      (and on-top-line? at-beginning-of-line?)
      [row col]

      ;; at column zero, move to the end of the line above
      at-beginning-of-line?
      [(dec row) (dec (count (nth text (dec row))))]

      ;; else just decrement the column number
      :else
      [row (dec col)])))

(defn- move-cursor-right
  "Move a cursor one space to the right."
  [text [row col]]
  (let [on-bottom-line? (= row (dec (count text)))
        current-line (nth text row)
        at-end-of-the-line? (= col (dec (count current-line)))]
    (cond
      ;; we are at the bottom right; do nothing
      (and on-bottom-line? at-end-of-the-line?)
      [row col]

      ;; at the end of a line, move to the beginning of the next line
      at-end-of-the-line?
      [(inc row) 0]

      ;; else just increment the column number
      :else
      [row (inc col)])))

;; TODO: this is unfinished
(defn- move-cursor-up
  "Move a cursor up one line."
  [text [row col]]
  (let [on-top-line? (zero? row)]
    (if-not on-top-line?
      [(dec row) col]
      [row col])))

(defn- move-cursor-down
  "Move a cursor down one line."
  [text [row col]]
  (let [on-bottom-line? (= row (dec (count text)))
        next-line-down (nth text (inc row) false)
        next-line-down-max-col (when next-line-down
                                 (dec (count next-line-down)))
        new-col (js/Math.min col next-line-down-max-col)]
    (cond
      ;; TODO: move to the end of the line
      on-bottom-line?
      [row col]

      ;; else just increment the row
      :else
      [(inc row) new-col])))

;;------------------------------------------------------------------------------
;; Public API
;;------------------------------------------------------------------------------

(defn- js-insert-text [js-coords new-text]
  ;; TODO: check coord bounds here
  (let [row-idx (first js-coords)
        col-idx (second js-coords)]
    (swap! grid-state
      (fn [state]
        (let [line (get-in state [:text row-idx])
              head (subvec line 0 col-idx)
              tail (subvec line col-idx)
              new-text (text->cells new-text)
              new-line (vec (concat head new-text tail))]
          (assoc-in state [:text row-idx] new-line))))))

(defn- js-remove-text [js-coords num-chars-to-remove]
  ;; TODO: check coord bounds here
  (let [row-idx (first js-coords)
        col-idx (second js-coords)]
    (swap! grid-state
      (fn [state]
        (let [line (get-in state [:text row-idx])
              head (subvec line 0 col-idx)
              tail (subvec line (+ col-idx num-chars-to-remove))
              new-line (vec (concat head tail))]
          (assoc-in state [:text row-idx] new-line))))))

;; TODO: write this; allow for generic cursor movement
(defn- js-move-cursor []
  (swap! grid-state update-in [:cursors 0 1] inc))

;; NOTE: these four functions can be combined
(defn- js-move-cursors-left []
  (swap! grid-state
    (fn [state]
      (let [text (:text state)
            cursors (:cursors state)
            new-cursors (map (partial move-cursor-left text) cursors)]
        (assoc state :cursors new-cursors)))))

(defn- js-move-cursors-right []
  (swap! grid-state
    (fn [state]
      (let [text (:text state)
            cursors (:cursors state)
            new-cursors (map (partial move-cursor-right text) cursors)]
        (assoc state :cursors new-cursors)))))

(defn- js-move-cursors-up []
  (swap! grid-state
    (fn [state]
      (let [text (:text state)
            cursors (:cursors state)
            new-cursors (map (partial move-cursor-up text) cursors)]
        (assoc state :cursors new-cursors)))))

(defn- js-move-cursors-down []
  (swap! grid-state
    (fn [state]
      (let [text (:text state)
            cursors (:cursors state)
            new-cursors (map (partial move-cursor-down text) cursors)]
        (assoc state :cursors new-cursors)))))

(goog/exportSymbol "grid.insertText" js-insert-text)
(goog/exportSymbol "grid.removeText" js-remove-text)
; (goog/exportSymbol "grid.moveCursor" js-move-cursor)
(goog/exportSymbol "grid.moveCursorsLeft" js-move-cursors-left)
(goog/exportSymbol "grid.moveCursorsRight" js-move-cursors-right)
(goog/exportSymbol "grid.moveCursorsUp" js-move-cursors-up)
(goog/exportSymbol "grid.moveCursorsDown" js-move-cursors-down)
