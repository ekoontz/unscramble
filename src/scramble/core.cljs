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

(defn drag-move-fn [drag-element]
  (d/log (str "drag-move-fn word: " (-> drag-element .-target .-innerHTML)))
  (fn [evt]
    (on-drag (.-clientX evt) (.-clientY evt))))

(defn on-mouse-down [drag-element]
  (let [drag-move (drag-move-fn drag-element)
        drag-end-atom (atom nil)
        on-start (fn []
                   (d/log (str "dragging has started!")))
        on-end (fn []
                 (d/log (str "dragging is over!")))
        drag-end
        (fn [evt]
          (events/unlisten js/window EventType.MOUSEMOVE drag-move)
          (events/unlisten js/window EventType.MOUSEUP @drag-end-atom)
          (on-end))]
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

(defn sentence-words []
  [:div
   (doall
     (map (fn [index]
             (sentence-word index))
          (shuffle (range (count (tokenize @sentence))))))])

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
   [sentence-words]
   [clock]])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     time-str]))

(defn ^:export run []
  (r/render [scramble-layout]
            (js/document.getElementById "app")))
