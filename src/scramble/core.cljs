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

(defonce points (r/atom 0))
(defonce remaining (r/atom (count @word-contents)))

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

(defn set-blank-styles [tokens & [offset]]
  (let [offset (or offset 0.0)]
    (if (not (empty? tokens))
      (let [em-per-word 8.0]
        (if debug (d/log (str "offset: " offset)))
        (cons
         (r/atom {"left" (str offset "em")
                  "top" "0"})
         (set-blank-styles (rest tokens) (* 0.5 (+ offset em-per-word))))))))

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
  (let [percent (/ 100.0 (* 1.6 (count (tokenize @sentence))))]
    [:div#blanks {:class "row"}
     (doall
      (map (fn [index]
             (let [style (merge @(nth @blank-styles index)
                                {"left" (str (* index 5) "em")
                                 "width" "3em"})]
               (reset! (nth @blank-styles index)
                       style)
               [:div {:draggable true
                      :class "blank word"
                      :style style
                      :id (str "sentence-blank-" index)
                      :key (str "sentence-blank-" index)}
                " "]))
           (range (count (tokenize @sentence)))))]))

(defn horizontal-px-to-em [pixels]
  (- (/ pixels 25.0) 2.4))

(defn vertical-px-to-em [pixels]
  (- (/ pixels 25.0) 6.2))

(defn update-word [index opacity x-position y-position]
  (let [x-position (horizontal-px-to-em x-position)
        y-position (vertical-px-to-em y-position)]
   (reset! (nth @word-styles index)
           {"opacity" opacity
            "left" (str x-position "em")
            "top" (str y-position "em")})))

(defn dragged-above-which [dragged-style]
  (let [dragged-left (-> (get dragged-style "left")
                        (clojure.string/replace "em" "")
                        js/parseFloat)]
    (first
     (filter #(not (nil? %))
              (map (fn [index]
                     (let [style-ref (nth @blank-styles index)
                           next-style-ref (if (< (+ 1 index) (count @blank-styles))
                                            (nth @blank-styles (+ 1 index)))
                           target-left (-> (get @style-ref "left")
                                           (clojure.string/replace "em" "")
                                           js/parseFloat)
                           next-target-left (if next-style-ref
                                              (-> (get @next-style-ref "left")
                                                  (clojure.string/replace "em" "")
                                                  js/parseFloat))]
                       (when (and (>= dragged-left target-left)
                                  (or (nil? next-style-ref)
                                      (< dragged-left next-target-left))
                                  (not (= "green" (get @style-ref "background"))))

                         index)))
                   (range 0 (count @blank-styles)))))))

(defn reset-blanks []
  (doall
   (map (fn [style-ref]
          (if (not (= "green" (get @style-ref "background")))
            (reset! style-ref
                    (dissoc @style-ref
                           "background"))))
        @blank-styles)))

(defn drag-word [index dragged-style x y]
  (update-word index 0.5 x y)
  ;; initialize all blanks to having no background.
  (reset-blanks)
  ;; collision check: flash the blank over which the word is.
  (if-let [over-blank-index (dragged-above-which dragged-style)]
    (let [over-blank (nth @blank-styles over-blank-index)]
      (reset! over-blank
              (merge @over-blank {"background" "blue"})))))

(defn drop-word [index dragged-style-ref x y]
  (update-word index 1.0 x y)
  (reset-blanks)
  ;; collision check: flash the blank over which the word is.
  (if-let [over-blank-index (dragged-above-which @dragged-style-ref)]
    (let [over-blank (nth @blank-styles over-blank-index)]
      (d/log (str "SCRAMBLED INDEX: " index "; CHOSEN POSITION: " over-blank-index))
      (d/log (str "CORRECT WORD AT POSITION:" (nth @tokens over-blank-index)))
      (if (= (nth @word-contents index)
             (nth @tokens over-blank-index))
        (do
          (d/log (str "YOU GOT IT RIGHT!!"))
          (reset! points (+ @points 1))
          (reset! remaining (- @remaining 1))
          (reset! over-blank
                  (merge @over-blank {"background" "green"}))
          (reset! dragged-style-ref
                  (merge @dragged-style-ref
                         {"top" "7em"
                          "background" "green"
                          "min-height" "1em"})))
        (do
          (d/log (str "SORRY THAT IS WRONG."))
          (reset! points (- @points 1))
          (reset! dragged-style-ref
                  (merge @dragged-style-ref
                         {"top" "0"})))))))

(defn draggable-action [index]
  (fn [drag-element]
   (let [drag-move (fn [evt]
                     (drag-word index @(nth @word-styles index) (.-clientX evt) (.-clientY evt)))
         drag-end-atom (atom nil)
         drag-end (fn [evt]
                    (d/log (str "done dragging element:")
                           (.-clientX evt) ", " (.-clientY evt))
                    (drop-word index (nth @word-styles index) (.-clientX evt) (.-clientY evt))
                    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
                    (events/unlisten js/window EventType.MOUSEUP @drag-end-atom))]
     (reset! drag-end-atom drag-end)
     (events/listen js/window EventType.MOUSEMOVE drag-move)
     (events/listen js/window EventType.MOUSEUP drag-end))))

(defn shuffled-words []
  [:div.dragcontainer
   [:div.row
    (doall
     (map (fn [index]
            [:div {:class "shuffled word"
                   :draggable true
                   :key (str "word-" index)
                   :style @(nth @word-styles index)
                   :on-mouse-down (draggable-action index)}
             (nth @word-contents index)])
          (range 0 (count @word-contents))))]

   [blank-words]
   [clock]])

(defn scramble-layout []
  [:div
   [:h1 "Sentence Scramble"]
   [shuffled-words]
   [:div.points "POINTS:" @points]
   [:div.remaining "WORDS REMAINING:" @remaining]
   [:div.controls
     [sentence-input]]])


(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.clock
     time-str]))

(defn ^:export run []
  (r/render [scramble-layout]
            (js/document.getElementById "app")))

