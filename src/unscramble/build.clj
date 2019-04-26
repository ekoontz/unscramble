(ns unscramble.build
  (:require [clojure.string :as str]
            [figwheel-sidecar.repl-api :as figwheel]
            [cljs.build.api :as cljs]
            [hiccup.core :refer [html]]))

(defn app [& body]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    [:meta {:name "theme-color" :content "#8fbcbb"}]
    [:link {:rel "stylesheet"
            :href "https://fonts.googleapis.com/css?family=Lato|Roboto:300,400"}]
    [:style "html, body { margin: 0; padding: 0; font-family: Roboto; }"]]
   [:body [:div {:id "app"}]
    body
    [:script {} "unscramble.core.render()"]]])

(defn http [req]
  {:body (str "<!DOCTYPE html>"
              (html (app [:script {:src "unscramble.js" :type "text/javascript"}])))})

(def figwheel-config
  {:figwheel-options
   {:ring-handler 'unscramble.build/http
    :nrepl-port 7888
    :nrepl-middleware ['cemerick.piggieback/wrap-cljs-repl]}
   :build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel {:on-jsload 'unscramble.core/render
                :open-urls ["http://localhost:3449/index.html"]
                :websocket-host :js-client-host}
     :source-paths ["src"]
     :compiler {:main 'unscramble.core
                :asset-path ""
                :output-to "resources/public/unscramble.js"
                :output-dir "resources/public"}}]})

(defn index-html []
  (cljs/build "src"
              {:output-to "target/public/unscramble.js"
               :output-dir "target/public"
               :pretty-print false
               :optimizations :advanced})
  (str
   "<!DOCTYPE html>"
   (html (app [:script (slurp "target/public/unscramble.js")]))))

(defn -main [& args]
  (case (first args)
    "server"
    (do (figwheel/start-figwheel! figwheel-config)
        (println "Piggieback (figwheel-sidecar.repl-api/repl-env)")
        (figwheel/cljs-repl))
    (println (index-html))))
