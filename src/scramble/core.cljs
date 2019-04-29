(ns scramble.core
  (:require
   [goog.events :as events]
   [reagent.core :as r]
   [reagent.debug :as d])
  (:import [goog.events EventType]))

(defn tokenize [sentence]
  (clojure.string/split sentence #"[ ]+"))

(defonce sentence (r/atom "Io ho dato il libro interessante a Paola."))

(defonce dragme-contents (shuffle (tokenize @sentence)))

(defonce dragme-styles
  (vec
   (map (fn [index]
          (r/atom {"left" (str (* index 200) "px")
                   "top" "0"}))
        (range 0 (count dragme-contents)))))


;; the sentence tokens in order.
(defonce tokens (r/atom (tokenize @sentence)))

(defn greeting [message]
  [:h1 message])

(defonce timer (r/atom (js/Date.)))
(def time-updater (js/setInterval
                   #(reset! timer (js/Date.)) 1000))
(declare clock)

(defn on-mouse-down [drag-element]
  (let [drag-move
        (do
          (d/log (str "starting to drag word:.. " (-> drag-element .-target .-innerHTML)))
          (fn [evt]
            (if false (d/log (str "drag-in-progress: " (.-clientX evt) (.-clientY evt))))))

        ;; not sure what drag-end-atom is for here.
        drag-end-atom (atom nil)

        drag-end
        (fn [evt]
          (do
            (d/log (str "done dragging element;"
                        (.-clientX evt) ", " (.-clientY evt)))
            (events/unlisten js/window EventType.MOUSEMOVE drag-move)
            (events/unlisten js/window EventType.MOUSEUP @drag-end-atom)))]
    (reset! drag-end-atom drag-end)
    (events/listen js/window EventType.MOUSEMOVE drag-move)
    (events/listen js/window EventType.MOUSEUP drag-end)))

(defn sentence-input []
  [:div.sentence-input
   "Input sentence: "
   [:input {:type "text"
            :size 75
            :value @sentence
            :on-change (fn [element]
                         (reset! sentence (-> element .-target .-value))
                         (reset! tokens (tokenize @sentence)))}]])

(declare scrambled-word)
(declare unscrambled-word)

(defn scrambled-words []
  [:div#scrambled
   (doall
     (map (fn [index]
             (scrambled-word index))
          (shuffle (range (count (tokenize @sentence))))))])

(defn scrambled-word [index]
  [:div {:draggable true
         :class "word scrambled"
         :on-mouse-down on-mouse-down
         :id (str "word-" index)
         :key (str "word-" index)}
   (nth @tokens index)])

(defn unscrambled-words []
  [:div#unscrambled
   (doall
     (map (fn [index]
             (unscrambled-word index))
          (range (count (tokenize @sentence)))))])

(defn unscrambled-word [index]
  [:div {:draggable true
         :class "word in-order"
         :id (str "sentence-word-" index)
         :key (str "sentence-word-" index)}
   (nth @tokens index)])

(defn blank-words []
  (let [percent (/ 100.0 (* 1.25 (count (tokenize @sentence))))]
    [:div#blank
     (doall
       (map (fn [index]
              [:div {:draggable true
                     :class "blank word"
                     :style {"width" (str percent "%")}
                     :id (str "sentence-blank-" index)
                     :key (str "sentence-blank-" index)}
                " "])
            (range (count (tokenize @sentence)))))]))

(def y-offset 0)
(defn update-dragme [index opacity x-position y-position]
  (let [y-position (+ y-position y-offset)])
  (reset! (nth dragme-styles index)
          {"opacity" opacity
           "left" (str x-position "px")
           "top" (str y-position "px")}))

(defn dragme-on-down [index]
  (fn [drag-element]
   (let [drag-move (fn [evt]
                     (update-dragme index 0.5 (.-clientX evt) (.-clientY evt)))
         drag-end-atom (atom nil)
         drag-end (fn [evt]
                    (d/log (str "done dragging element:")
                           (.-clientX evt) ", " (.-clientY evt))
                    (update-dragme index 1.0 (.-clientX evt) (.-clientY evt))
                    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
                    (events/unlisten js/window EventType.MOUSEUP @drag-end-atom))]
     (reset! drag-end-atom drag-end)
     (events/listen js/window EventType.MOUSEMOVE drag-move)
     (events/listen js/window EventType.MOUSEUP drag-end))))

(defn show-dragme []
  [:div.dragcontainer
   (doall
    (map (fn [index]
           [:div.dragme {:key (str "dragme-" index)
                         :style @(nth dragme-styles index)
                         :on-mouse-down (dragme-on-down index)}
            (nth dragme-contents index)])
         (range 0 (count dragme-contents))))

   [:div {:class "row blanks"}
    [blank-words]]
   [clock]])

(defn scramble-layout []
  [:div
   [show-dragme]
   [:div.controls
     [greeting "Sentence Scramble Controls"]
     [sentence-input]
     [:div {:class "row"}
       [unscrambled-words]]]])


(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.clock
     time-str]))

(defn ^:export run []
  (r/render [scramble-layout]
            (js/document.getElementById "app")))

