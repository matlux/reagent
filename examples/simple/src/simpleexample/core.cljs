(ns simpleexample.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! take!]]
            [ajax.core :refer [GET POST]]
            [reagent.core :as r]))

(comment

  (use 'figwheel-sidecar.repl-api)
  (start-figwheel!)
  (cljs-repl)

  (in-ns 'simpleexample.core)
  *ns*
  (println "hiya2")
  (http/get "https://api.github.com/users"
            {:with-credentials? false
             :query-params      {"since" 135}})

  (def c (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response))))))
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %))

  (take! c #(println %))
(GET "http://localhost:8080/bar")
  (foo)

  )

(enable-console-print!)

(defn foo2 []
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))
(defn foo []
  (take! (go (let [response (<! (http/get "http://localhost:8080/bar"
                                          {:with-credentials? false}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))

(defonce timer (r/atom (js/Date.)))

(def time-color (r/atom "#920"))
(def test-field (r/atom "data..."))

(def time-updater (js/setInterval
                        #(do
                           (reset! test-field (foo))
                           (reset! timer (js/Date.))) 5000))

(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))


(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defn test-component []
  [:div.color-input
   "test="
   @test-field
   ])


(defn simple-example []
  [:div
   [greeting "Hello world2, it is now"]
   [clock]
   [color-input]
   [test-component]])

(defn ^:export run []
  (r/render [simple-example]
            (js/document.getElementById "app")))
