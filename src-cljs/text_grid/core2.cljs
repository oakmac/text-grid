(ns text-grid.core2
  (:require
    [clojure.string :refer [split split-lines]]
    [goog.functions :refer [throttle once]]
    [text-grid.util :refer [by-id js-log log]]
    [rum.core :as rum]))

;;------------------------------------------------------------------------------
;; Constants
;;------------------------------------------------------------------------------

;; TODO: write me

;;------------------------------------------------------------------------------
;; Window State Atom
;;------------------------------------------------------------------------------

(def initial-window-state
  {:dragging-splitbar? false
   :divider-pct 0.4
   :window-height "800px"})

(def window-state (atom initial-window-state))

;; NOTE: useful for debugging
; (add-watch window-state :log atom-logger)

;;------------------------------------------------------------------------------
;; Mouse Position
;;------------------------------------------------------------------------------

(def mouse-throttle-rate-ms 20)

(def mouse
  "Holds the current mouse position."
  (atom {:x 0, :y 0}))

(defn- js-on-mouse-move [js-evt]
  (let [x (aget js-evt "pageX")
        y (aget js-evt "pageY")]
    (reset! mouse {:x x, :y y})))

(def throttled-mouse-move (throttle js-on-mouse-move mouse-throttle-rate-ms))
(.addEventListener js/document.body "mousemove" throttled-mouse-move)

;;------------------------------------------------------------------------------
;; StatusBar
;;------------------------------------------------------------------------------

(rum/defc StatusBar < rum/static
  []
  [:div.status-bar-container "TODO: status bar"])

;;------------------------------------------------------------------------------
;; Track Mouse Position for the Divider Bar
;;------------------------------------------------------------------------------

(defn- on-change-mouse-pos
  "Updates the panel divider while the mouse is moving."
  [_kwd _the-atom _old-pos {:keys [x y]}]
  (let [window-width (aget js/window "innerWidth")
        mouse-pct (/ x window-width)]
    ;; TODO: add some lower / upper bounds here for the percent
    (swap! window-state assoc :divider-pct mouse-pct)))

(defn- mouseup-body
  "Remove the panel divider add-watch! listener when the mouse is released."
  []
  (js-log "Panel divider event removed")
  (remove-watch mouse :panel-divider))

(.addEventListener js/document.body "mouseup" mouseup-body)

;;------------------------------------------------------------------------------
;; Divider Bar
;;------------------------------------------------------------------------------

(defn- on-mouse-down-panel-divider [js-evt]
  (add-watch mouse :panel-divider on-change-mouse-pos)
  (js-log "Panel divider event added"))

(rum/defc PanelDivider < rum/static
  []
  [:div.panel-divider
    {:on-mouse-down on-mouse-down-panel-divider}])

;;------------------------------------------------------------------------------
;; Panel
;;------------------------------------------------------------------------------

(rum/defc Panel < rum/static
  [flex]
  [:div.panel-container
    {:style {:flex flex}}
    "TODO: panel"])

;;------------------------------------------------------------------------------
;; Top Level Component
;;------------------------------------------------------------------------------

(rum/defc Window < rum/static
  [{:keys [divider-pct window-height]}]
  (let [left-panel-flex (* 100 divider-pct)
        right-panel-flex (* 100 (- 1 divider-pct))]
    [:div.window-container
      [:div.panels-container
        (Panel left-panel-flex)
        (PanelDivider)
        (Panel right-panel-flex)]
      (StatusBar)]))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def container-el (by-id "appContainer"))

(defn- on-change-window-state
  "Render on every state change."
  [_kwd _the-atom _old-state new-state]
  (rum/request-render
    (rum/mount (Window new-state) container-el)))

(add-watch window-state :main on-change-window-state)

;;------------------------------------------------------------------------------
;; Window Init
;;------------------------------------------------------------------------------

(def init!
  (once
    (fn []
      ;; trigger an initial render
      (swap! window-state identity))))

(init!)

;;------------------------------------------------------------------------------
;; Public API
;;------------------------------------------------------------------------------

;; TODO: write me
