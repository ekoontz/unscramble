(ns scramble.core
  (:require
   [goog.events :as events]
   [reagent.core :as r]
   [reagent.debug :as d])
  (:import [goog.events EventType]))

(defn tokenize [sentence]
  (clojure.string/split sentence #"[ ]+"))

(def original-dragme "hello")
(defonce dragme-content (r/atom original-dragme))

(defonce dragme
  (r/atom [:div#dragme {:style {"float" "left"}} @dragme-content]))

(defonce sentence (r/atom "Io ho dato il libro interessante a Paola."))

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

(defn update-dragme [new-text]
  (d/log (str "UPDATING DRAGME..."))
  (reset! dragme-content new-text)
  (let [[element attributes old-text] @dragme]
    (reset! dragme
            [element attributes
             @dragme-content])
    (d/log (str "DONE UPDATING DRAGME!" @dragme))))

(defn dragme-on-down [drag-element]
  (d/log (str "got here"))
  (let [drag-move
        (do
          (d/log (str "starting to drag the 'dragme': " (-> drag-element .-target .-innerHTML)))
          (fn [evt]
             (reset! dragme-content (str "dragging to: " (.-clientX evt)))
             (if false (d/log (str "dragme-on-down is in-progress: " (.-clientX evt) (.-clientY evt))))))

        ;; not sure what drag-end-atom is for here.
        drag-end-atom (atom nil)

        drag-end
        (fn [evt]
          (do
            (d/log (str "done dragging element!!!:"
                        (.-clientX evt) ", " (.-clientY evt)))
            (update-dragme original-dragme)
            (events/unlisten js/window EventType.MOUSEMOVE drag-move)
            (events/unlisten js/window EventType.MOUSEUP @drag-end-atom)))]
    (reset! drag-end-atom drag-end)
    (events/listen js/window EventType.MOUSEMOVE drag-move)
    (events/listen js/window EventType.MOUSEUP drag-end)))


(defn show-dragme []
  (reset! dragme
          (let [[element attributes old-text] @dragme]
            [element (merge attributes
                            {:on-mouse-down dragme-on-down})
                     @dragme-content]))
  @dragme)

(defn scramble-layout []
  [:div
   [show-dragme]
   [greeting "Sentence Scramble!"]
   [sentence-input]
   [:div {:class "row"}
     [unscrambled-words]]
   [:div {:class "row"}
     [scrambled-words]]
   [:div {:class "row blanks"}
     [blank-words]]
   [clock]])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     time-str]))

(defn ^:export run []
  (r/render [scramble-layout]
            (js/document.getElementById "app")))

