(ns simpleexample.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! take!]]
            [ajax.core :refer [GET POST]]
            [reagent.core :as r]
            [simpleexample.components :as c]
            [simpleexample.geometry :as g])
  )

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
  @points

  (map (fn [p1 p2] [p1 p2]) '(1 2 3 4) (drop 1 '(1 2 3 4)))

  (conj [1 2 3] 4)
  (js->clj (get-point))
  )

(enable-console-print!)
(def points
  (r/atom
    [(g/point 100 100)
     (g/point 200 200)
     (g/point 300 200)
     (g/point 400 250)
     (g/point 500 300)]))


(defn foo2 []
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))
(defn foo3 []
  (take! (go (let [response (<! (http/get "http://localhost:8080/bar"
                                          {:with-credentials? false}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))

(defn foo []
  (str (.getSeconds (js/Date.)) " " @points))

(defn get-point []
  (clj->js (.getSeconds (js/Date.))))


(defn add-point []
  (swap! points (fn [pts]
                  (conj pts (g/point 600 (get-point))))))



(defonce timer (r/atom (js/Date.)))

(def time-color (r/atom "#920"))
(def test-field (r/atom "data..."))

(def time-updater (js/setInterval
                        #(do
                           (reset! test-field (get-point))
                           ;(add-point)
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

;;-------------------------------

(def pointsold
         (r/atom
           {:p1 (g/point 100 100)
            :p2 (g/point 200 200)
            :p3 (g/point 300 200)
            :c (g/point 250 250)
            :p (g/point 250 300)}))


(defonce slider
         (r/atom
           {:handle (g/point 500 50)
            :history []}))

(defn record-state [_ _ _ s]
  (swap! slider (fn [{:keys [history] :as coll}]
                  (assoc coll :history (conj history s)))))

(defn start-recording-history []
  (let [history (:history @slider)]
    (add-watch points :record record-state)))

(defn stop-recording-history []
  (remove-watch points :record))

(add-watch points :record record-state)

(defn get-bcr [svg-root]
  (-> svg-root
      r/dom-node
      .getBoundingClientRect))

(defn move-point [svg-root p]
  (fn [x y]
    (let [bcr (get-bcr svg-root)]
      (swap! points assoc p (g/point (- x (.-left bcr)) (- y (.-top bcr)))))))

(defn move-slider [svg-root p]
  (fn [x y]
    (let [new-x (-> (- x (.-left (get-bcr svg-root)))
                    (min 500)
                    (max 100))
          position (/ (- new-x 100)
                      (- 500 100))
          history (:history @slider)]
      (swap! slider assoc p (g/point new-x 50))
      (reset! points (nth history (int (* (dec (count history)) position)))))))

(defn graph [pts]

  (map (fn [[p1 p2]] (c/segment p1 p2)) (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts)))
  )

(defn root [svg-root]
  (let [pts @points]
    [:g
     ;[c/triangle p1 p2 p3]
     ;[c/circle p c]
     ;[c/segment (get pts 0) (get pts 1)]
     ;[c/segment (get pts 1) (get pts 2)]
     ;(c/segment (get pts 2) (get pts 3))
     (graph pts)
     ;(map (fn [[p1 p2]] (c/segment p1 p2)) (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts)))
     [c/segment (g/point 100 50) (g/point 500 50)]
     [c/rect {:on-drag (move-slider svg-root :handle)
              :on-start stop-recording-history
              :on-end start-recording-history} (:handle @slider)]
     [c/point {:on-drag (move-point svg-root :c)} (get pts 0)]
     [c/point {:on-drag (move-point svg-root :p)} (get pts 1)]
     [c/point {:on-drag (move-point svg-root :p1)} (get pts 2)]
     [c/point {:on-drag (move-point svg-root :p2)} (get pts 3)]
     [c/point {:on-drag (move-point svg-root :p3)} (get pts 4)]
     ]))

(defn canvas [{:keys [width height]}]
  [:svg
   {:width (or width 800)
    :height (or height 600)
    :style {:border "1px solid black"}}
   [:text {:style {:-webkit-user-select "none"
                   :-moz-user-select "none"}
           :x 20 :y 20 :font-size 20}
    "The points are draggable and the slider controls history"]
   [root (r/current-component)]])


(defn simple-example [{:keys [width height] :as ctx}]
  [:div
   [greeting "Hello world2, it is now"]
   [clock]
   [color-input]
   [test-component]
   [canvas ctx]])

(defn ^:export run []
  (r/render [simple-example]
            (js/document.getElementById "app")))
