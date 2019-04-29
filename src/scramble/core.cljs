(ns scramble.core
  (:require
   [goog.events :as events]
   [reagent.core :as r]
   [reagent.debug :as d])
  (:import [goog.events EventType]))

(defonce timer (r/atom (js/Date.)))

(defonce sentence (r/atom "Io ho dato il libro interessante a Paola."))

(def time-updater (js/setInterval
                   #(reset! timer (js/Date.)) 1000))

(defn tokenize [sentence]
  (clojure.string/split sentence #"[ ]+"))

(def tokens (r/atom (tokenize @sentence)))

(defn set-tokens [sentence]
  (reset! tokens (tokenize sentence)))

(defn greeting [message]
  [:h1 message])

(declare clock)

(defn on-drag [x y]
  (d/log (str "on-drag at: " x "," y)))

(defn set-sentence [sentence-input-element]
  (reset! sentence (-> sentence-input-element .-target .-value))
  (set-tokens @sentence))

(defn on-mouse-down [drag-element]
  (let [drag-move
        (do
          (d/log (str "starting to drag word: " (-> drag-element .-target .-innerHTML)))
          (fn [evt]
            (on-drag (.-clientX evt) (.-clientY evt))))
        drag-end-atom (atom nil)
        on-start (fn []
                   (d/log (str "dragging has started for: " (-> drag-element .-target .-innerHTML))))       drag-end
        (fn [evt]
          (do
            (d/log (str "done dragging.. " (.-clientX evt) ", " (.-clientY evt)))
            (events/unlisten js/window EventType.MOUSEMOVE drag-move)
            (events/unlisten js/window EventType.MOUSEUP @drag-end-atom)))]
    (on-start)
    (reset! drag-end-atom drag-end)
    (events/listen js/window EventType.MOUSEMOVE drag-move)
    (events/listen js/window EventType.MOUSEUP drag-end)))

(defn sentence-input []
  [:div.sentence-input
   "Input sentence: "
   [:input {:type "text"
            :size 75
            :value @sentence
            :on-change set-sentence}]])

(declare sentence-word)

(defn scrambled-words []
  [:div#scrambled
   (doall
     (map (fn [index]
             (sentence-word index))
          (shuffle (range (count (tokenize @sentence))))))])

(defn unscrambled-words []
  [:div#scrambled
   (doall
     (map (fn [index]
             (sentence-word index))
          (range (count (tokenize @sentence)))))])

(defn sentence-word [index]
  [:div.word {:draggable true
              :on-mouse-down on-mouse-down
              :id (str "word-" index)
              :key (str "word-" index)}
   (nth @tokens index)])

(defn scramble-layout []
  [:div
   [greeting "Sentence Scramble!"]
   [sentence-input]
   [unscrambled-words]
   [scrambled-words]
   [clock]])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     time-str]))

(defn ^:export run []
  (r/render [scramble-layout]
            (js/document.getElementById "app")))
