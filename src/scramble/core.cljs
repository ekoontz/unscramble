(ns scramble.core
  (:require
   [goog.events :as events]
   [reagent.core :as r]
   [reagent.debug :as d])
  (:import [goog.events EventType]))

(defn tokenize [sentence]
  (clojure.string/split sentence #"[ ]+"))

(defonce sentence (r/atom "Io ho dato il libro interessante a Paola."))

(defonce word-contents (r/atom (shuffle (tokenize @sentence))))

(defonce debug false)

(defn set-scrambled-styles [tokens & [offset]]
  (let [offset (or offset 0.0)]
    (if (not (empty? tokens))
      (let [token (first tokens)
            em-per-word (Math/max 3.0 (count token))]
        (if debug (d/log (str "offset: " offset)))
        (cons
         (r/atom {"left" (str offset "em")
                  "top" "0"})
         (set-scrambled-styles (rest tokens) (* 0.98 (+ offset em-per-word))))))))

(defn set-blank-styles [tokens]
  (map (fn [token]
         (r/atom {"token" token}))
       tokens))

(declare set-blank-styles)
(declare set-scrambled-styles)

(defonce word-styles
  (r/atom (set-scrambled-styles @word-contents)))

(defonce blank-styles
  (r/atom (set-blank-styles @word-contents)))

;; the sentence tokens in order.
(defonce tokens (r/atom (tokenize @sentence)))

(defonce timer (r/atom (js/Date.)))
(def time-updater (js/setInterval
                   #(reset! timer (js/Date.)) 1000))
(declare clock)

(defn on-mouse-down [drag-element]
  (let [drag-move
        (fn [evt]
          (if false (d/log (str "drag-in-progress: " (.-clientX evt) (.-clientY evt)))))
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
                         (let [tokenized (tokenize (clojure.string/trim @sentence))]
                           (reset! tokens tokenized)
                           (reset! word-contents (shuffle tokenized))
                           (reset! word-styles (set-scrambled-styles @word-contents))
                           (reset! blank-styles (set-blank-styles @word-contents))))}]])

(declare unscrambled-word)

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
             (let [style (merge @(nth @blank-styles index)
                                {"width" (str percent "%")})]
               (reset! (nth @blank-styles index)
                       style)
               [:div {:draggable true
                      :class "blank word"
                      :style style
                      :id (str "sentence-blank-" index)
                      :key (str "sentence-blank-" index)}
                " "]))
           (range (count (tokenize @sentence)))))]))

(defn update-word [index opacity x-position y-position]
  ;; For some reason, the mouse cursor is offset by 90 and 100 pixels,
  ;; in the x and y dimensions, respectively, so this corrects for
  ;; that apparent skew.
  (let [x-position (- x-position 90)
        y-position (- y-position 100)]
   (reset! (nth @word-styles index)
           {"opacity" opacity
            "left" (str x-position "px")
            "top" (str y-position "px")})))

(defn dragged-above-which [x y]
  (first
   (filter #(not (nil? %))
           (map (fn [index]
                  (let [style-ref (nth @blank-styles index)]
                    (d/log (str "ELEMENT:"
                                (.getElementById js/document
                                                 (str "sentence-blank-" index))))
                    (d/log (str "LEFT:"
                                (.getPropertyValue
                                  (.getComputedStyle js/window
                                        (.getElementById js/document
                                                         (str "sentence-blank-" index)))
                                  "left")))
                    (when (= (get @style-ref "token") "libro")
                      (d/log (str "FOUND!!"))
                      (d/log (str "x-blank: " @style-ref))
                      (d/log (str "  token: " (get @style-ref "token")))
                      style-ref)))
                (range 0 (count @blank-styles))))))

(defn drag-word [index x y]
  (update-word index 0.5 x y)
  ;; collision check: flash the blank over which the word is.

  (if-let [over-blank (dragged-above-which x y)]
    (reset! over-blank
            (merge @over-blank {"background" "blue"}))))

(defn drop-word [index x y]
  (update-word index 1.0 x y)
  ;; collision check: flash the blank over which the word is.
  (doall
   (map (fn [style-ref]
          (reset! style-ref
                  (dissoc @style-ref
                         "background")))
        @blank-styles)))

(defn draggable-action [index]
  (fn [drag-element]
   (let [drag-move (fn [evt]
                     (drag-word index (.-clientX evt) (.-clientY evt)))
         drag-end-atom (atom nil)
         drag-end (fn [evt]
                    (d/log (str "done dragging element:")
                           (.-clientX evt) ", " (.-clientY evt))
                    (drop-word index (.-clientX evt) (.-clientY evt))
                    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
                    (events/unlisten js/window EventType.MOUSEUP @drag-end-atom))]
     (reset! drag-end-atom drag-end)
     (events/listen js/window EventType.MOUSEMOVE drag-move)
     (events/listen js/window EventType.MOUSEUP drag-end))))

(defn shuffled-words []
  [:div.dragcontainer
   (doall
    (map (fn [index]
           [:div {:class "shuffled word"
                  :key (str "word-" index)
                  :style @(nth @word-styles index)
                  :on-mouse-down (draggable-action index)}
            (nth @word-contents index)])
         (range 0 (count @word-contents))))

   [:div {:class "row blanks"}
    [blank-words]]
   [clock]])

(defn scramble-layout []
  [:div
   [:h1 "Sentence Scramble"]
   [shuffled-words]
   [:div.controls
     [:h1 "Controls"]
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

